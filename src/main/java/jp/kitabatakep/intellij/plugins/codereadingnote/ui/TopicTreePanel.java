package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.dnd.TopicTreeTransferHandler;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;
import java.util.function.Predicate;

/** One hierarchy for root and nested projects; selection keys always include project identity. */
public class TopicTreePanel extends JPanel {
    private final Project project;
    private final WorkspaceNotesService workspace;
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private final JTree topicTree = new JTree(treeModel);
    private TopicTreeNode selectedNode;
    private TopicTreeSelectionListener selectionListener;
    private boolean refreshQueued;
    private boolean rebuilding;

    public interface TopicTreeSelectionListener {
        void onTopicSelected(Topic topic);
        void onGroupSelected(TopicGroup group);
        void onTopicLineSelected(TopicLine topicLine);
        void onUngroupedLinesSelected(Topic topic);
        void onSelectionCleared();
    }
    public TopicTreePanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        workspace = WorkspaceNotesService.getInstance(project);
        topicTree.setRootVisible(false);
        topicTree.setShowsRootHandles(true);
        topicTree.setCellRenderer(new TopicTreeCellRenderer());
        topicTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        topicTree.setDragEnabled(true);
        topicTree.setDropMode(DropMode.ON_OR_INSERT);
        topicTree.setTransferHandler(new TopicTreeTransferHandler(CodeReadingNoteService.getInstance(project), this::loadTopics));
        TreeSpeedSearch.installOn(topicTree);
        topicTree.addTreeSelectionListener(event -> {
            if (rebuilding) return;
            selectedNode = topicTree.getLastSelectedPathComponent() instanceof TopicTreeNode node ? node : null;
            notifySelection();
        });
        topicTree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) navigateSelected();
            }
            @Override public void mousePressed(MouseEvent event) { if (event.isPopupTrigger()) trashMenu(event); }
            @Override public void mouseReleased(MouseEvent event) { if (event.isPopupTrigger()) trashMenu(event); }
        });
        topicTree.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) navigateSelected();
                if (event.getKeyCode() == KeyEvent.VK_SPACE && selectedNode != null && selectedNode.canHaveChildren()) {
                    TreePath path = new TreePath(selectedNode.getPath());
                    if (topicTree.isExpanded(path)) topicTree.collapsePath(path); else topicTree.expandPath(path);
                }
            }
        });
        topicTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override public void treeExpanded(TreeExpansionEvent event) { expansion(event, true); }
            @Override public void treeCollapsed(TreeExpansionEvent event) { expansion(event, false); }
        });
        project.getMessageBus().connect(project).subscribe(WorkspaceNotesNotifier.TOPIC, source -> loadTopics());
        project.getMessageBus().connect(project).subscribe(jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncNotifier.TOPIC, source -> topicTree.repaint());
        add(new JBScrollPane(topicTree), BorderLayout.CENTER);
        loadTopics();
    }
    public void setSelectionListener(TopicTreeSelectionListener listener) { selectionListener = listener; }
    private void expansion(TreeExpansionEvent event, boolean expanded) {
        if (!rebuilding && event.getPath().getLastPathComponent() instanceof TopicTreeNode node) node.setExpanded(expanded);
    }
    private void navigateSelected() {
        TopicLine line = getSelectedTopicLine();
        if (line != null) line.navigate(project, true);
    }
    private void notifySelection() {
        if (selectionListener == null) return;
        if (selectedNode == null) { selectionListener.onSelectionCleared(); return; }
        switch (selectedNode.getNodeType()) {
            case TOPIC -> selectionListener.onTopicSelected(selectedNode.getTopic());
            case GROUP -> selectionListener.onGroupSelected(selectedNode.getGroup());
            case TOPIC_LINE -> selectionListener.onTopicLineSelected(selectedNode.getTopicLine());
            case UNGROUPED_LINES_FOLDER -> selectionListener.onUngroupedLinesSelected(selectedNode.getTopic());
            default -> selectionListener.onSelectionCleared();
        }
    }
    public void loadTopics() {
        if (refreshQueued) return;
        refreshQueued = true;
        SwingUtilities.invokeLater(() -> {
            refreshQueued = false;
            if (project.isDisposed()) return;
            Set<String> expanded = new HashSet<>();
            Set<String> previous = new HashSet<>();
            for (TopicTreeNode node : nodes()) {
                previous.add(key(node));
                if (topicTree.isExpanded(new TreePath(node.getPath()))) expanded.add(key(node));
            }
            String selection = selectedNode == null ? null : key(selectedNode);
            Object selectedObject = selectedNode == null ? null : selectedNode.getUserObject();
            rebuilding = true;
            try {
                rootNode.removeAllChildren();
                rootNode.setUserObject(CodeReadingNoteBundle.message("workspace.title"));
                topicTree.setRootVisible(workspace.isWorkspace());
                for (TopicList list : workspace.projects()) {
                    DefaultMutableTreeNode parent = rootNode;
                    if (workspace.isWorkspace()) {
                        parent = new TopicTreeNode(new TopicTreeNode.ProjectItem(list, workspace.displayName(list.context())), TopicTreeNode.NodeType.PROJECT);
                        rootNode.add(parent);
                    }
                    for (Topic topic : list.getTopics()) {
                        TopicTreeNode node = new TopicTreeNode(topic, TopicTreeNode.NodeType.TOPIC);
                        parent.add(node);
                        for (TopicGroup group : topic.getGroups()) {
                            TopicTreeNode groupNode = new TopicTreeNode(group, TopicTreeNode.NodeType.GROUP);
                            node.add(groupNode);
                            addLines(groupNode, group.getLines());
                        }
                        if (!topic.getUngroupedLines().isEmpty()) {
                            TopicTreeNode ungrouped = new TopicTreeNode(topic, TopicTreeNode.NodeType.UNGROUPED_LINES_FOLDER);
                            node.add(ungrouped);
                            addLines(ungrouped, topic.getUngroupedLines());
                        }
                    }
                    if (!list.getTrashedLines().isEmpty()) {
                        TopicTreeNode trash = new TopicTreeNode(list, TopicTreeNode.NodeType.TRASH_BIN);
                        parent.add(trash);
                        for (TrashedLine line : list.getTrashedLines()) trash.add(new TopicTreeNode(line, TopicTreeNode.NodeType.TRASHED_LINE));
                    }
                }
                treeModel.reload();
                topicTree.expandPath(new TreePath(rootNode.getPath()));
                selectedNode = null;
                for (TopicTreeNode node : nodes()) {
                    String key = key(node);
                    if (expanded.contains(key) || node.getNodeType() == TopicTreeNode.NodeType.PROJECT && !previous.contains(key)) {
                        topicTree.expandPath(new TreePath(node.getPath()));
                    }
                    if (key.equals(selection)) {
                        selectedNode = node;
                        topicTree.setSelectionPath(new TreePath(node.getPath()));
                    }
                }
            } finally { rebuilding = false; }
            if (selectedNode == null || selectedNode.getUserObject() != selectedObject) notifySelection();
        });
    }
    private void addLines(TopicTreeNode parent, java.util.List<TopicLine> lines) {
        for (TopicLine line : lines) parent.add(new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE));
    }
    private java.util.List<TopicTreeNode> nodes() {
        java.util.List<TopicTreeNode> result = new ArrayList<>();
        Enumeration<TreeNode> all = rootNode.preorderEnumeration();
        while (all.hasMoreElements()) if (all.nextElement() instanceof TopicTreeNode node) result.add(node);
        return result;
    }
    private String key(TopicTreeNode node) {
        TopicList list = node.getProjectList();
        String owner = list == null ? "" : list.context().id();
        Object value = node.getUserObject();
        if (value instanceof Topic topic) return owner + ":" + node.getNodeType() + ":" + topic.name();
        if (value instanceof TopicGroup group) return owner + ":group:" + group.getParentTopic().name() + ":" + group.name();
        if (value instanceof TopicLine line) return line.runtimeId() + ":" + line.topic().name();
        if (value instanceof TrashedLine trash) return trash.getLine().runtimeId() + ":trash:" + trash.getTrashedAt().getTime();
        return owner + ":" + node.getNodeType();
    }
    public TopicList getSelectedProjectList() {
        if (selectedNode != null) return selectedNode.getProjectList();
        return workspace.isWorkspace() ? null : workspace.projects().get(0);
    }
    public TopicTreeNode getSelectedNode() { return selectedNode; }
    public Topic getSelectedTopic() { return selectedNode == null ? null : selectedNode.getTopic(); }
    public TopicGroup getSelectedGroup() { return selectedNode == null ? null : selectedNode.getGroup(); }
    public TopicLine getSelectedTopicLine() { return selectedNode == null ? null : selectedNode.getTopicLine(); }
    public void refreshTopic(Topic topic) { loadTopics(); }
    public void collapseAllGroups() {
        for (int row = topicTree.getRowCount() - 1; row >= 0; row--) {
            TreePath path = topicTree.getPathForRow(row);
            if (path.getLastPathComponent() instanceof TopicTreeNode node && node.getNodeType() != TopicTreeNode.NodeType.PROJECT) topicTree.collapsePath(path);
        }
    }
    public void expandAllGroups() { for (int row = 0; row < topicTree.getRowCount(); row++) topicTree.expandRow(row); }
    public boolean areAllNodesCollapsed() {
        return nodes().stream().filter(n -> n.canHaveChildren() && n.getNodeType() != TopicTreeNode.NodeType.PROJECT)
                .noneMatch(n -> topicTree.isExpanded(new TreePath(n.getPath())));
    }
    public void selectTopic(Topic topic) { select(n -> n.getTopic() == topic && n.getNodeType() == TopicTreeNode.NodeType.TOPIC); }
    public void selectTopicLine(TopicLine line) {
        if (line != null) select(n -> n.getTopicLine() != null && (n.getTopicLine() == line
                || n.getTopicLine().runtimeId().equals(line.runtimeId()) && n.getTopicLine().topic().name().equals(line.topic().name())));
    }
    public void selectGroupLine(TopicGroup group, TopicLine line) { selectTopicLine(line); }
    public void selectUngroupedLine(Topic topic, TopicLine line) { selectTopicLine(line); }
    private void select(Predicate<TopicTreeNode> predicate) {
        SwingUtilities.invokeLater(() -> {
            if (project.isDisposed()) return;
            for (TopicTreeNode node : nodes()) if (predicate.test(node)) {
                TreePath path = new TreePath(node.getPath());
                topicTree.setSelectionPath(path);
                topicTree.scrollPathToVisible(path);
                return;
            }
        });
    }
    private void trashMenu(MouseEvent event) {
        TreePath path = topicTree.getPathForLocation(event.getX(), event.getY());
        if (path == null || !(path.getLastPathComponent() instanceof TopicTreeNode node)) return;
        topicTree.setSelectionPath(path);
        TopicList list = node.getProjectList();
        if (list == null) return;
        JPopupMenu menu = new JPopupMenu();
        if (node.getNodeType() == TopicTreeNode.NodeType.TRASH_BIN) {
            JMenuItem empty = new JMenuItem(CodeReadingNoteBundle.message("trash.bin.empty"));
            empty.addActionListener(e -> list.emptyTrash());
            menu.add(empty);
        } else if (node.getTrashedLine() != null) {
            JMenuItem restore = new JMenuItem(CodeReadingNoteBundle.message("trash.bin.restore"));
            restore.addActionListener(e -> list.restoreFromTrash(node.getTrashedLine()));
            menu.add(restore);
            JMenuItem delete = new JMenuItem(CodeReadingNoteBundle.message("trash.bin.permanent.delete"));
            delete.addActionListener(e -> list.permanentlyDelete(node.getTrashedLine()));
            menu.add(delete);
        }
        if (menu.getComponentCount() > 0) menu.addSeparator();
        for (var operation : new jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.WorkspaceNotesSyncCoordinator.Operation[]{
                jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.WorkspaceNotesSyncCoordinator.Operation.PUSH,
                jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.WorkspaceNotesSyncCoordinator.Operation.PULL,
                jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.WorkspaceNotesSyncCoordinator.Operation.CHECK}) {
            JMenuItem item = new JMenuItem(CodeReadingNoteBundle.message("notes.sync." + operation.name().toLowerCase(java.util.Locale.ROOT)));
            item.addActionListener(e -> jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncUi.run(project, list, operation));
            menu.add(item);
        }
        JMenuItem settings = new JMenuItem(CodeReadingNoteBundle.message("notes.sync.settings"));
        settings.addActionListener(e -> jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.NotesSyncUi.configure(project, list, null));
        menu.add(settings);
        menu.show(topicTree, event.getX(), event.getY());
    }
}
