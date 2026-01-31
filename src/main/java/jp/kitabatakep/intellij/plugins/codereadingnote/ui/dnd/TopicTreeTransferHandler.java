package jp.kitabatakep.intellij.plugins.codereadingnote.ui.dnd;

import com.intellij.openapi.diagnostic.Logger;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicList;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.TopicTreeNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TransferHandler for TopicTreePanel to support drag and drop operations.
 * Supports:
 * - Reordering Topics by dragging
 * - Moving TopicLines between Groups
 */
public class TopicTreeTransferHandler extends TransferHandler {
    
    private static final Logger LOG = Logger.getInstance(TopicTreeTransferHandler.class);
    
    // Use a single DataFlavor for all tree node transfers
    private static final DataFlavor TREE_NODE_FLAVOR;
    
    static {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + 
                    ";class=java.lang.Object");
        } catch (ClassNotFoundException e) {
            LOG.error("Failed to create DataFlavor", e);
        }
        TREE_NODE_FLAVOR = flavor;
    }
    
    private final CodeReadingNoteService service;
    private final Runnable refreshCallback;
    
    public TopicTreeTransferHandler(CodeReadingNoteService service, Runnable refreshCallback) {
        this.service = service;
        this.refreshCallback = refreshCallback;
    }
    
    /**
     * Refresh the tree - SlowOperations is handled in loadTopics()
     */
    private void refreshTree() {
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (TREE_NODE_FLAVOR == null) {
            return null;
        }
        
        if (!(c instanceof JTree)) {
            return null;
        }
        
        JTree sourceTree = (JTree) c;
        TreePath[] paths = sourceTree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return null;
        }
        
        // Get the first selected node
        Object lastComponent = paths[0].getLastPathComponent();
        if (!(lastComponent instanceof TopicTreeNode)) {
            return null;
        }
        
        TopicTreeNode node = (TopicTreeNode) lastComponent;
        
        switch (node.getNodeType()) {
            case TOPIC:
                Topic topic = node.getTopic();
                if (topic != null) {
                    LOG.info("Creating transferable for Topic: " + topic.name());
                    return new TreeNodeTransferable(TreeNodeTransferable.Type.TOPIC, topic, null);
                } else {
                    LOG.warn("createTransferable: Topic is null");
                }
                break;
                
            case TOPIC_LINE:
                // Collect all selected TopicLines
                List<TopicLine> selectedLines = new ArrayList<>();
                for (TreePath path : paths) {
                    Object comp = path.getLastPathComponent();
                    if (comp instanceof TopicTreeNode) {
                        TopicTreeNode lineNode = (TopicTreeNode) comp;
                        if (lineNode.getNodeType() == TopicTreeNode.NodeType.TOPIC_LINE) {
                            TopicLine line = lineNode.getTopicLine();
                            if (line != null) {
                                selectedLines.add(line);
                            }
                        }
                    }
                }
                if (!selectedLines.isEmpty()) {
                    LOG.info("Creating transferable for " + selectedLines.size() + " TopicLine(s)");
                    return new TreeNodeTransferable(TreeNodeTransferable.Type.TOPIC_LINE, null, selectedLines);
                }
                break;
                
            case GROUP:
                // Group can be dragged to reorder within its parent Topic
                TopicGroup group = node.getGroup();
                if (group != null) {
                    LOG.info("createTransferable: GROUP " + group.name());
                    return new TreeNodeTransferable(TreeNodeTransferable.Type.GROUP, group.getParentTopic(), null, group);
                }
                break;
                
            default:
                // UNGROUPED_LINES_FOLDER is not draggable
                break;
        }
        
        return null;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        
        if (TREE_NODE_FLAVOR == null) {
            return false;
        }
        
        if (!support.isDataFlavorSupported(TREE_NODE_FLAVOR)) {
            return false;
        }
        
        // Get drop location
        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        TreePath dropPath = dropLocation.getPath();
        
        if (dropPath == null) {
            return false;
        }
        
        Object targetComponent = dropPath.getLastPathComponent();
        int childIndex = dropLocation.getChildIndex();
        
        // Try to get the transferable to check its type
        try {
            Transferable transferable = support.getTransferable();
            Object data = transferable.getTransferData(TREE_NODE_FLAVOR);
            
            if (!(data instanceof TreeNodeTransferable)) {
                return false;
            }
            
            TreeNodeTransferable treeData = (TreeNodeTransferable) data;
            
            if (treeData.getType() == TreeNodeTransferable.Type.TOPIC) {
                // Topic can be dropped:
                // 1. ON another Topic node (swap/reorder)
                // 2. BETWEEN Topics (childIndex >= 0, parent is root node)
                if (targetComponent instanceof TopicTreeNode) {
                    TopicTreeNode targetNode = (TopicTreeNode) targetComponent;
                    return targetNode.getNodeType() == TopicTreeNode.NodeType.TOPIC;
                } else {
                    // Dropping between items at root level (target is root node)
                    // childIndex >= 0 means inserting between children
                    return childIndex >= 0;
                }
            } else if (treeData.getType() == TreeNodeTransferable.Type.TOPIC_LINE) {
                // TopicLine can be dropped on GROUP or UNGROUPED_LINES_FOLDER
                if (targetComponent instanceof TopicTreeNode) {
                    TopicTreeNode targetNode = (TopicTreeNode) targetComponent;
                    TopicTreeNode.NodeType targetType = targetNode.getNodeType();
                    return targetType == TopicTreeNode.NodeType.GROUP || 
                           targetType == TopicTreeNode.NodeType.UNGROUPED_LINES_FOLDER;
                }
            } else if (treeData.getType() == TreeNodeTransferable.Type.GROUP) {
                // Group can be dropped:
                // 1. ON another Group node within same Topic (swap)
                // 2. BETWEEN Groups (childIndex >= 0, parent is Topic node)
                if (targetComponent instanceof TopicTreeNode) {
                    TopicTreeNode targetNode = (TopicTreeNode) targetComponent;
                    TopicTreeNode.NodeType targetType = targetNode.getNodeType();
                    
                    if (targetType == TopicTreeNode.NodeType.GROUP) {
                        // Can drop on another Group in the same Topic
                        TopicGroup draggedGroup = treeData.getGroup();
                        TopicGroup targetGroup = targetNode.getGroup();
                        return draggedGroup != null && targetGroup != null &&
                               draggedGroup.getParentTopic() == targetGroup.getParentTopic() &&
                               draggedGroup != targetGroup;
                    } else if (targetType == TopicTreeNode.NodeType.TOPIC) {
                        // Can drop between groups within a Topic (childIndex >= 0)
                        TopicGroup draggedGroup = treeData.getGroup();
                        Topic targetTopic = targetNode.getTopic();
                        return draggedGroup != null && targetTopic != null &&
                               draggedGroup.getParentTopic() == targetTopic &&
                               childIndex >= 0;
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore - canImport is called frequently during drag
            return false;
        }
        
        return false;
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        
        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        TreePath dropPath = dropLocation.getPath();
        int childIndex = dropLocation.getChildIndex();
        
        Object targetComponent = dropPath.getLastPathComponent();
        
        try {
            TreeNodeTransferable transferable = (TreeNodeTransferable) 
                    support.getTransferable().getTransferData(TREE_NODE_FLAVOR);
            
            if (transferable.getType() == TreeNodeTransferable.Type.TOPIC) {
                // Handle both: dropping ON a topic or BETWEEN topics
                if (targetComponent instanceof TopicTreeNode) {
                    TopicTreeNode targetNode = (TopicTreeNode) targetComponent;
                    return handleTopicDropOnTopic(transferable.getTopic(), targetNode);
                } else {
                    // Dropping between topics at root level
                    return handleTopicDropBetween(transferable.getTopic(), childIndex);
                }
            } else if (transferable.getType() == TreeNodeTransferable.Type.TOPIC_LINE) {
                if (targetComponent instanceof TopicTreeNode) {
                    TopicTreeNode targetNode = (TopicTreeNode) targetComponent;
                    return handleTopicLineDrop(transferable.getTopicLines(), targetNode);
                }
            } else if (transferable.getType() == TreeNodeTransferable.Type.GROUP) {
                if (targetComponent instanceof TopicTreeNode) {
                    TopicTreeNode targetNode = (TopicTreeNode) targetComponent;
                    return handleGroupDrop(transferable.getGroup(), targetNode, childIndex);
                }
            }
            
        } catch (UnsupportedFlavorException | IOException e) {
            LOG.error("Failed to import data", e);
        }
        
        return false;
    }
    
    /**
     * Handle dropping a Topic onto another Topic (swap positions)
     */
    private boolean handleTopicDropOnTopic(Topic draggedTopic, TopicTreeNode targetNode) {
        if (targetNode.getNodeType() != TopicTreeNode.NodeType.TOPIC) {
            return false;
        }
        
        Topic targetTopic = targetNode.getTopic();
        if (targetTopic == null || draggedTopic == null) {
            return false;
        }
        
        // Don't drop on self
        if (draggedTopic == targetTopic) {
            return false;
        }
        
        // Also check by name in case objects are different instances
        if (draggedTopic.name().equals(targetTopic.name())) {
            return false;
        }
        
        TopicList topicList = service.getTopicList();
        ArrayList<Topic> topics = topicList.getTopics();
        
        int fromIndex = topics.indexOf(draggedTopic);
        int toIndex = topics.indexOf(targetTopic);
        
        if (fromIndex == -1 || toIndex == -1) {
            LOG.warn("Could not find topic indices");
            return false;
        }
        
        LOG.info("Moving Topic '" + draggedTopic.name() + "' from " + fromIndex + " to " + toIndex);
        topicList.moveTopic(fromIndex, toIndex);
        
        // Refresh the tree
        refreshTree();
        
        return true;
    }
    
    /**
     * Handle dropping a Topic between other Topics (insert at index)
     */
    private boolean handleTopicDropBetween(Topic draggedTopic, int targetIndex) {
        if (draggedTopic == null || targetIndex < 0) {
            return false;
        }
        
        TopicList topicList = service.getTopicList();
        ArrayList<Topic> topics = topicList.getTopics();
        
        int fromIndex = topics.indexOf(draggedTopic);
        
        if (fromIndex == -1) {
            LOG.warn("Could not find dragged topic index");
            return false;
        }
        
        // Calculate actual target index
        int toIndex = targetIndex;
        if (fromIndex < targetIndex) {
            // If moving down, account for removal of source
            toIndex = targetIndex - 1;
        }
        
        // Clamp to valid range
        if (toIndex < 0) toIndex = 0;
        if (toIndex >= topics.size()) toIndex = topics.size() - 1;
        
        if (fromIndex == toIndex) {
            return false; // No change needed
        }
        
        LOG.info("Moving Topic '" + draggedTopic.name() + "' from " + fromIndex + " to " + toIndex);
        topicList.moveTopic(fromIndex, toIndex);
        
        // Refresh the tree
        refreshTree();
        
        return true;
    }
    
    /**
     * Handle dropping TopicLine(s) onto a Group or Ungrouped folder
     */
    private boolean handleGroupDrop(TopicGroup draggedGroup, TopicTreeNode targetNode, int childIndex) {
        if (draggedGroup == null) {
            return false;
        }
        
        Topic topic = draggedGroup.getParentTopic();
        if (topic == null) {
            return false;
        }
        
        TopicTreeNode.NodeType targetType = targetNode.getNodeType();
        
        if (targetType == TopicTreeNode.NodeType.GROUP) {
            // Swap with target group
            TopicGroup targetGroup = targetNode.getGroup();
            if (targetGroup == null || targetGroup.getParentTopic() != topic) {
                return false;
            }
            
            int fromIndex = topic.getGroupIndex(draggedGroup);
            int toIndex = topic.getGroupIndex(targetGroup);
            
            LOG.info("Moving group " + draggedGroup.name() + " from " + fromIndex + " to " + toIndex);
            
            topic.moveGroup(fromIndex, toIndex);
            refreshTree();
            return true;
            
        } else if (targetType == TopicTreeNode.NodeType.TOPIC && childIndex >= 0) {
            // Insert at specific position
            Topic targetTopic = targetNode.getTopic();
            if (targetTopic != topic) {
                return false;
            }
            
            int fromIndex = topic.getGroupIndex(draggedGroup);
            int toIndex = childIndex;
            
            // Adjust toIndex if needed
            if (fromIndex < toIndex) {
                toIndex--;
            }
            
            // Clamp to valid range
            if (toIndex < 0) toIndex = 0;
            if (toIndex >= topic.getGroups().size()) toIndex = topic.getGroups().size() - 1;
            
            if (fromIndex == toIndex) {
                return false;
            }
            
            LOG.info("Moving group " + draggedGroup.name() + " from " + fromIndex + " to " + toIndex);
            
            topic.moveGroup(fromIndex, toIndex);
            refreshTree();
            return true;
        }
        
        return false;
    }
    
    private boolean handleTopicLineDrop(List<TopicLine> lines, TopicTreeNode targetNode) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        
        TopicTreeNode.NodeType targetType = targetNode.getNodeType();
        
        if (targetType == TopicTreeNode.NodeType.GROUP) {
            // Move to target group
            TopicGroup targetGroup = targetNode.getGroup();
            if (targetGroup == null) {
                return false;
            }
            
            Topic topic = targetGroup.getParentTopic();
            if (topic == null) {
                return false;
            }
            
            LOG.info("Moving " + lines.size() + " line(s) to group: " + targetGroup.name());
            
            for (TopicLine line : lines) {
                // Only move if the line belongs to the same topic
                if (line.topic() == topic) {
                    topic.moveLineToGroup(line, targetGroup);
                }
            }
            
            // Refresh the tree
            refreshTree();
            
            return true;
            
        } else if (targetType == TopicTreeNode.NodeType.UNGROUPED_LINES_FOLDER) {
            // Move to ungrouped
            // Get the parent Topic from the folder
            Topic topic = targetNode.getTopic();
            if (topic == null) {
                return false;
            }
            
            LOG.info("Moving " + lines.size() + " line(s) to ungrouped");
            
            for (TopicLine line : lines) {
                // Only move if the line belongs to the same topic
                if (line.topic() == topic) {
                    topic.moveLineToUngrouped(line);
                }
            }
            
            // Refresh the tree
            refreshTree();
            
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // The actual data modification is done in importData, so nothing to clean up here
    }
    
    // ========== Transferable implementation ==========
    
    /**
     * Transferable wrapper for tree nodes
     */
    public static class TreeNodeTransferable implements Transferable {
        
        public enum Type {
            TOPIC,
            TOPIC_LINE,
            GROUP
        }
        
        private final Type type;
        private final Topic topic;
        private final List<TopicLine> topicLines;
        private final TopicGroup group;
        
        public TreeNodeTransferable(Type type, Topic topic, List<TopicLine> topicLines) {
            this(type, topic, topicLines, null);
        }
        
        public TreeNodeTransferable(Type type, Topic topic, List<TopicLine> topicLines, TopicGroup group) {
            this.type = type;
            this.topic = topic;
            this.topicLines = topicLines != null ? new ArrayList<>(topicLines) : null;
            this.group = group;
        }
        
        public Type getType() {
            return type;
        }
        
        public Topic getTopic() {
            return topic;
        }
        
        public List<TopicLine> getTopicLines() {
            return topicLines;
        }
        
        public TopicGroup getGroup() {
            return group;
        }
        
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            if (TREE_NODE_FLAVOR == null) {
                return new DataFlavor[0];
            }
            return new DataFlavor[] { TREE_NODE_FLAVOR };
        }
        
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return TREE_NODE_FLAVOR != null && TREE_NODE_FLAVOR.equals(flavor);
        }
        
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return this;
        }
    }
}
