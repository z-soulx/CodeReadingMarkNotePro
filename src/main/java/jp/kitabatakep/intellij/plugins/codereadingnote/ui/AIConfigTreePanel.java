package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;

/**
 * Tree panel displaying AI config files in a proper hierarchical directory structure.
 * Supports checkbox-based sync selection with recursive propagation.
 */
public class AIConfigTreePanel extends JPanel {

    private final Project project;
    private final CheckboxTree tree;
    private final CheckedTreeNode rootNode;
    private SelectionListener selectionListener;
    private boolean childStateChanging = false;

    public AIConfigTreePanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

        rootNode = new CheckedTreeNode("AI Configs");

        CheckboxTree.CheckboxTreeCellRenderer renderer = new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded,
                                          boolean leaf, int row, boolean hasFocus) {
                if (!(value instanceof CheckedTreeNode)) return;
                CheckedTreeNode node = (CheckedTreeNode) value;
                Object userObj = node.getUserObject();

                if (userObj instanceof AIConfigEntry) {
                    AIConfigEntry entry = (AIConfigEntry) userObj;
                    getTextRenderer().append(entry.getFileName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    getTextRenderer().setIcon(AllIcons.FileTypes.Text);
                    appendSyncStatusTag(entry);
                } else if (userObj instanceof DirNode) {
                    DirNode dir = (DirNode) userObj;
                    getTextRenderer().append(dir.displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    getTextRenderer().append("  (" + dir.totalFileCount + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    getTextRenderer().setIcon(AllIcons.Nodes.Folder);
                } else if (userObj instanceof String) {
                    getTextRenderer().append((String) userObj, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    getTextRenderer().setIcon(AllIcons.Nodes.ConfigFolder);
                }
            }

            private void appendSyncStatusTag(@NotNull AIConfigEntry entry) {
                AIConfigSyncStatus status = getSyncStatus(entry);
                switch (status) {
                    case NEW:
                        getTextRenderer().append("  \u2605",
                            new SimpleTextAttributes(STYLE_PLAIN, new Color(92, 184, 92)));
                        break;
                    case MODIFIED:
                        getTextRenderer().append("  \u25CF",
                            new SimpleTextAttributes(STYLE_PLAIN, new Color(240, 173, 78)));
                        break;
                    case SYNCED:
                        getTextRenderer().append("  \u2713",
                            new SimpleTextAttributes(STYLE_PLAIN, Color.GRAY));
                        break;
                }
            }
        };

        tree = new CheckboxTree(renderer, rootNode) {
            @Override
            protected void onNodeStateChanged(CheckedTreeNode node) {
                super.onNodeStateChanged(node);
                Object userObj = node.getUserObject();
                if (userObj instanceof AIConfigEntry) {
                    ((AIConfigEntry) userObj).setTracked(node.isChecked());
                    childStateChanging = true;
                    updateAncestorDirStates(node);
                    childStateChanging = false;
                } else if (userObj instanceof DirNode) {
                    if (!childStateChanging) {
                        propagateCheckStateRecursive(node);
                        updateAncestorDirStates(node);
                    }
                }
            }
        };

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            if (selectionListener == null) return;
            TreePath path = e.getNewLeadSelectionPath();
            if (path == null) {
                selectionListener.onSelectionCleared();
                return;
            }
            CheckedTreeNode node = (CheckedTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof AIConfigEntry) {
                selectionListener.onFileSelected((AIConfigEntry) userObj);
            } else if (userObj instanceof DirNode) {
                selectionListener.onDirectorySelected(((DirNode) userObj).fullPath);
            } else {
                selectionListener.onSelectionCleared();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);

        loadEntries();
    }

    // ---- Tree building ----

    public void loadEntries() {
        rootNode.removeAllChildren();

        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();
        List<AIConfigEntry> entries = registry.getEntries();
        Set<String> discoveredDirs = registry.getDiscoveredDirs();

        if (entries.isEmpty() && discoveredDirs.isEmpty()) {
            CheckedTreeNode emptyNode = new CheckedTreeNode(
                CodeReadingNoteBundle.message("aiconfig.tree.empty"));
            emptyNode.setEnabled(false);
            rootNode.add(emptyNode);
        } else {
            VirtualDir virtualRoot = new VirtualDir("");

            // Ensure all discovered directories exist in the tree (including empty ones)
            for (String dirPath : discoveredDirs) {
                String[] segments = dirPath.split("/");
                VirtualDir current = virtualRoot;
                for (String segment : segments) {
                    if (!segment.isEmpty()) {
                        current = current.subdirs.computeIfAbsent(segment, VirtualDir::new);
                    }
                }
            }

            for (AIConfigEntry entry : entries) {
                String[] segments = entry.getRelativePath().split("/");
                VirtualDir current = virtualRoot;
                for (int i = 0; i < segments.length - 1; i++) {
                    current = current.subdirs.computeIfAbsent(segments[i], VirtualDir::new);
                }
                current.files.add(entry);
            }
            computeFileCounts(virtualRoot);

            for (Map.Entry<String, VirtualDir> topLevel : virtualRoot.subdirs.entrySet()) {
                buildTreeNodes(rootNode, topLevel.getValue(), topLevel.getKey(), topLevel.getKey());
            }
            for (AIConfigEntry entry : virtualRoot.files) {
                CheckedTreeNode fileNode = new CheckedTreeNode(entry);
                fileNode.setChecked(entry.isTracked());
                rootNode.add(fileNode);
            }
        }

        ((DefaultTreeModel) tree.getModel()).reload();
        expandAll();
    }

    private void buildTreeNodes(@NotNull CheckedTreeNode parentTreeNode,
                                @NotNull VirtualDir dir,
                                @NotNull String displayName,
                                @NotNull String fullPath) {
        DirNode dirData = new DirNode(displayName, fullPath, dir.totalFileCount);
        CheckedTreeNode dirTreeNode = new CheckedTreeNode(dirData);

        for (Map.Entry<String, VirtualDir> sub : dir.subdirs.entrySet()) {
            buildTreeNodes(dirTreeNode, sub.getValue(), sub.getKey(), fullPath + "/" + sub.getKey());
        }

        for (AIConfigEntry entry : dir.files) {
            CheckedTreeNode fileNode = new CheckedTreeNode(entry);
            fileNode.setChecked(entry.isTracked());
            dirTreeNode.add(fileNode);
        }

        dirTreeNode.setChecked(areAllDescendantsTracked(dir));
        parentTreeNode.add(dirTreeNode);
    }

    private boolean areAllDescendantsTracked(@NotNull VirtualDir dir) {
        for (AIConfigEntry file : dir.files) {
            if (!file.isTracked()) return false;
        }
        for (VirtualDir sub : dir.subdirs.values()) {
            if (!areAllDescendantsTracked(sub)) return false;
        }
        return !dir.files.isEmpty() || !dir.subdirs.isEmpty();
    }

    private void computeFileCounts(@NotNull VirtualDir dir) {
        int count = dir.files.size();
        for (VirtualDir sub : dir.subdirs.values()) {
            computeFileCounts(sub);
            count += sub.totalFileCount;
        }
        dir.totalFileCount = count;
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    // ---- Checkbox state management ----

    private void updateAncestorDirStates(@NotNull CheckedTreeNode node) {
        TreeNode parent = node.getParent();
        while (parent instanceof CheckedTreeNode) {
            CheckedTreeNode parentNode = (CheckedTreeNode) parent;
            if (!(parentNode.getUserObject() instanceof DirNode)) break;

            boolean allChecked = true;
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                TreeNode child = parentNode.getChildAt(i);
                if (child instanceof CheckedTreeNode && !((CheckedTreeNode) child).isChecked()) {
                    allChecked = false;
                    break;
                }
            }
            parentNode.setChecked(allChecked);
            ((DefaultTreeModel) tree.getModel()).nodeChanged(parentNode);
            parent = parentNode.getParent();
        }
    }

    private void propagateCheckStateRecursive(@NotNull CheckedTreeNode parentNode) {
        boolean checked = parentNode.isChecked();
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            TreeNode child = parentNode.getChildAt(i);
            if (child instanceof CheckedTreeNode) {
                CheckedTreeNode childNode = (CheckedTreeNode) child;
                childNode.setChecked(checked);
                Object userObj = childNode.getUserObject();
                if (userObj instanceof AIConfigEntry) {
                    ((AIConfigEntry) userObj).setTracked(checked);
                } else if (userObj instanceof DirNode) {
                    propagateCheckStateRecursive(childNode);
                }
            }
        }
        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
    }

    // ---- Status & selection ----

    @NotNull
    private AIConfigSyncStatus getSyncStatus(@NotNull AIConfigEntry entry) {
        Map<String, String> pushedHashes = AIConfigService.getInstance(project).getLastPushedFileHashes();
        String pushedHash = pushedHashes.get(entry.getRelativePath());
        if (pushedHash == null) {
            return AIConfigSyncStatus.NEW;
        }
        if (pushedHash.equals(entry.getContentHash())) {
            return AIConfigSyncStatus.SYNCED;
        }
        return AIConfigSyncStatus.MODIFIED;
    }

    @Nullable
    public AIConfigEntry getSelectedEntry() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;
        CheckedTreeNode node = (CheckedTreeNode) path.getLastPathComponent();
        return node.getUserObject() instanceof AIConfigEntry ? (AIConfigEntry) node.getUserObject() : null;
    }

    public void setSelectionListener(@Nullable SelectionListener listener) {
        this.selectionListener = listener;
    }

    // ---- Inner types ----

    public interface SelectionListener {
        void onFileSelected(@NotNull AIConfigEntry entry);
        void onDirectorySelected(@NotNull String dirPath);
        void onSelectionCleared();
    }

    static class DirNode {
        final String displayName;
        final String fullPath;
        final int totalFileCount;

        DirNode(@NotNull String displayName, @NotNull String fullPath, int totalFileCount) {
            this.displayName = displayName;
            this.fullPath = fullPath;
            this.totalFileCount = totalFileCount;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class VirtualDir {
        final String name;
        final Map<String, VirtualDir> subdirs = new LinkedHashMap<>();
        final List<AIConfigEntry> files = new ArrayList<>();
        int totalFileCount = 0;

        VirtualDir(@NotNull String name) {
            this.name = name;
        }
    }
}
