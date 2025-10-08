package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

/**
 * Tree panel for displaying topics with subgroups in a hierarchical structure
 */
public class TopicTreePanel extends JPanel {
    
    private Project project;
    private CodeReadingNoteService service;
    
    private JTree topicTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    
    private TopicTreeNode selectedNode;
    private TopicTreeSelectionListener selectionListener;
    
    public interface TopicTreeSelectionListener {
        void onTopicSelected(Topic topic);
        void onGroupSelected(TopicGroup group);
        void onTopicLineSelected(TopicLine topicLine);
        void onUngroupedLinesSelected(Topic topic); // 新增：处理点击Ungrouped Lines文件夹
        void onSelectionCleared();
    }
    
    public TopicTreePanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.service = CodeReadingNoteService.getInstance(project);
        
        initTree();
        setupEventHandlers();
        loadTopics();
        
        add(new JBScrollPane(topicTree), BorderLayout.CENTER);
    }
    
    public void setSelectionListener(TopicTreeSelectionListener listener) {
        this.selectionListener = listener;
    }
    
    private void initTree() {
        rootNode = new DefaultMutableTreeNode("Topics");
        treeModel = new DefaultTreeModel(rootNode);
        topicTree = new JTree(treeModel);
        
        // Configure tree
        topicTree.setRootVisible(false);
        topicTree.setShowsRootHandles(true);
        topicTree.setCellRenderer(new TopicTreeCellRenderer());
        topicTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        // Enable speed search
        new TreeSpeedSearch(topicTree, treePath -> {
            Object lastComponent = treePath.getLastPathComponent();
            if (lastComponent instanceof TopicTreeNode) {
                return ((TopicTreeNode) lastComponent).getDisplayName();
            }
            return lastComponent.toString();
        });
    }
    
    private void setupEventHandlers() {
        // Selection listener
        topicTree.addTreeSelectionListener(e -> {
            TreePath selectedPath = topicTree.getSelectionPath();
            if (selectedPath != null && selectedPath.getLastPathComponent() instanceof TopicTreeNode) {
                TopicTreeNode node = (TopicTreeNode) selectedPath.getLastPathComponent();
                selectedNode = node;
                notifySelection(node);
            } else {
                selectedNode = null;
                if (selectionListener != null) {
                    selectionListener.onSelectionCleared();
                }
            }
        });
        
        // Double-click listener for navigation
        topicTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = topicTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null && path.getLastPathComponent() instanceof TopicTreeNode) {
                        TopicTreeNode node = (TopicTreeNode) path.getLastPathComponent();
                        if (node.getNodeType() == TopicTreeNode.NodeType.TOPIC_LINE) {
                            TopicLine line = node.getTopicLine();
                            if (line != null && line.canNavigate()) {
                                line.navigate(true);
                            }
                        }
                    }
                }
            }
        });
        
        // Keyboard listener
        topicTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (selectedNode != null && selectedNode.getNodeType() == TopicTreeNode.NodeType.TOPIC_LINE) {
                        TopicLine line = selectedNode.getTopicLine();
                        if (line != null && line.canNavigate()) {
                            line.navigate(true);
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // Toggle expansion
                    if (selectedNode != null && selectedNode.canHaveChildren()) {
                        TreePath path = topicTree.getSelectionPath();
                        if (topicTree.isExpanded(path)) {
                            topicTree.collapsePath(path);
                        } else {
                            topicTree.expandPath(path);
                        }
                    }
                }
            }
        });
        
        // Tree expansion listener to update node states
        topicTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                Object lastComponent = event.getPath().getLastPathComponent();
                if (lastComponent instanceof TopicTreeNode) {
                    ((TopicTreeNode) lastComponent).setExpanded(true);
                }
            }
            
            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                Object lastComponent = event.getPath().getLastPathComponent();
                if (lastComponent instanceof TopicTreeNode) {
                    ((TopicTreeNode) lastComponent).setExpanded(false);
                }
            }
        });
    }
    
    private void notifySelection(TopicTreeNode node) {
        if (selectionListener == null) return;
        
        switch (node.getNodeType()) {
            case TOPIC:
                selectionListener.onTopicSelected(node.getTopic());
                break;
            case GROUP: 
                selectionListener.onGroupSelected(node.getGroup());
                break;
            case TOPIC_LINE:
                selectionListener.onTopicLineSelected(node.getTopicLine());
                break;
            case UNGROUPED_LINES_FOLDER:
                // 处理点击"Ungrouped Lines"文件夹的情况
                // 需要找到父Topic并显示其ungrouped lines
                if (node.getParent() instanceof TopicTreeNode) {
                    TopicTreeNode parentNode = (TopicTreeNode) node.getParent();
                    if (parentNode.getNodeType() == TopicTreeNode.NodeType.TOPIC) {
                        selectionListener.onUngroupedLinesSelected(parentNode.getTopic());
                    }
                }
                break;
            default:
                selectionListener.onSelectionCleared();
                break;
        }
    }
    
    public void loadTopics() {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            
            for (Topic topic : service.getTopicList().getTopics()) {
                TopicTreeNode topicNode = new TopicTreeNode(topic, TopicTreeNode.NodeType.TOPIC);
                rootNode.add(topicNode);
                
                // Add groups if topic has any
                if (!topic.getGroups().isEmpty()) {
                    for (TopicGroup group : topic.getGroups()) {
                        TopicTreeNode groupNode = new TopicTreeNode(group, TopicTreeNode.NodeType.GROUP);
                        topicNode.add(groupNode);
                        
                        // Add lines in group
                        for (TopicLine line : group.getLines()) {
                            TopicTreeNode lineNode = new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE);
                            groupNode.add(lineNode);
                        }
                    }
                    
                    // Add ungrouped lines folder if there are ungrouped lines
                    if (!topic.getUngroupedLines().isEmpty()) {
                        TopicTreeNode ungroupedNode = new TopicTreeNode("Ungrouped", TopicTreeNode.NodeType.UNGROUPED_LINES_FOLDER);
                        topicNode.add(ungroupedNode);
                        
                        for (TopicLine line : topic.getUngroupedLines()) {
                            TopicTreeNode lineNode = new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE);
                            ungroupedNode.add(lineNode);
                        }
                    }
                } else {
                    // Legacy mode - add lines directly under topic
                    for (TopicLine line : topic.getLines()) {
                        TopicTreeNode lineNode = new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE);
                        topicNode.add(lineNode);
                    }
                }
            }
            
            treeModel.reload();
            expandAllTopics();
        });
    }
    
    public void refreshTopic(Topic topic) {
        SwingUtilities.invokeLater(() -> {
            TopicTreeNode topicNode = findTopicNode(topic);
            if (topicNode != null) {
                // Remove and re-add children
                topicNode.removeAllChildren();
                
                // Add groups if topic has any
                if (!topic.getGroups().isEmpty()) {
                    for (TopicGroup group : topic.getGroups()) {
                        TopicTreeNode groupNode = new TopicTreeNode(group, TopicTreeNode.NodeType.GROUP);
                        topicNode.add(groupNode);
                        
                        // Add lines in group
                        for (TopicLine line : group.getLines()) {
                            TopicTreeNode lineNode = new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE);
                            groupNode.add(lineNode);
                        }
                    }
                    
                    // Add ungrouped lines folder if there are ungrouped lines
                    if (!topic.getUngroupedLines().isEmpty()) {
                        TopicTreeNode ungroupedNode = new TopicTreeNode("Ungrouped", TopicTreeNode.NodeType.UNGROUPED_LINES_FOLDER);
                        topicNode.add(ungroupedNode);
                        
                        for (TopicLine line : topic.getUngroupedLines()) {
                            TopicTreeNode lineNode = new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE);
                            ungroupedNode.add(lineNode);
                        }
                    }
                } else {
                    // Legacy mode - add lines directly under topic
                    for (TopicLine line : topic.getLines()) {
                        TopicTreeNode lineNode = new TopicTreeNode(line, TopicTreeNode.NodeType.TOPIC_LINE);
                        topicNode.add(lineNode);
                    }
                }
                
                treeModel.nodeStructureChanged(topicNode);
                expandNode(topicNode);
            }
        });
    }
    
    private TopicTreeNode findTopicNode(Topic topic) {
        Enumeration<TreeNode> children = rootNode.children();
        while (children.hasMoreElements()) {
            TreeNode child = children.nextElement();
            if (child instanceof TopicTreeNode) {
                TopicTreeNode node = (TopicTreeNode) child;
                if (node.getNodeType() == TopicTreeNode.NodeType.TOPIC && node.getTopic() == topic) {
                    return node;
                }
            }
        }
        return null;
    }
    
    private void expandAllTopics() {
        Enumeration<TreeNode> children = rootNode.children();
        while (children.hasMoreElements()) {
            TreeNode child = children.nextElement();
            if (child instanceof TopicTreeNode) {
                expandNode((TopicTreeNode) child);
            }
        }
    }
    
    private void expandNode(TopicTreeNode node) {
        TreePath path = new TreePath(treeModel.getPathToRoot(node));
        topicTree.expandPath(path);
        
        // Expand subgroups if they were previously expanded
        if (node.getNodeType() == TopicTreeNode.NodeType.TOPIC) {
            Enumeration<TreeNode> children = node.children();
            while (children.hasMoreElements()) {
                TreeNode child = children.nextElement();
                if (child instanceof TopicTreeNode) {
                    TopicTreeNode childNode = (TopicTreeNode) child;
                    if (childNode.isExpanded()) {
                        TreePath childPath = new TreePath(treeModel.getPathToRoot(childNode));
                        topicTree.expandPath(childPath);
                    }
                }
            }
        }
    }
    
    public TopicTreeNode getSelectedNode() {
        return selectedNode;
    }
    
    public Topic getSelectedTopic() {
        if (selectedNode != null && selectedNode.getNodeType() == TopicTreeNode.NodeType.TOPIC) {
            return selectedNode.getTopic();
        }
        return null;
    }
    
    public TopicGroup getSelectedGroup() {
        if (selectedNode != null && selectedNode.getNodeType() == TopicTreeNode.NodeType.GROUP) {
            return selectedNode.getGroup();
        }
        return null;
    }
    
    public TopicLine getSelectedTopicLine() {
        if (selectedNode != null && selectedNode.getNodeType() == TopicTreeNode.NodeType.TOPIC_LINE) {
            return selectedNode.getTopicLine();
        }
        return null;
    }
    
    public void selectTopic(Topic topic) {
        TopicTreeNode node = findTopicNode(topic);
        if (node != null) {
            TreePath path = new TreePath(treeModel.getPathToRoot(node));
            topicTree.setSelectionPath(path);
            topicTree.scrollPathToVisible(path);
        }
    }
}
