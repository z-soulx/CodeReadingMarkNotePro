package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node for representing different types of objects in the topic tree
 */
public class TopicTreeNode extends DefaultMutableTreeNode {
    
    public enum NodeType {
        TOPIC,
        GROUP, 
        TOPIC_LINE,
        UNGROUPED_LINES_FOLDER // Virtual folder for ungrouped lines
    }
    
    private NodeType nodeType;
    private boolean expanded = true;
    
    public TopicTreeNode(Object userObject, NodeType nodeType) {
        super(userObject);
        this.nodeType = nodeType;
        
        // Set default expansion state
        if (nodeType == NodeType.TOPIC || nodeType == NodeType.GROUP) {
            this.expanded = true;
        }
    }
    
    public NodeType getNodeType() {
        return nodeType;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        
        // Update the actual object's expansion state if applicable
        if (nodeType == NodeType.GROUP && getUserObject() instanceof TopicGroup) {
            ((TopicGroup) getUserObject()).setExpanded(expanded);
        }
    }
    
    public Topic getTopic() {
        if (nodeType == NodeType.TOPIC) {
            return (Topic) getUserObject();
        }
        return null;
    }
    
    public TopicGroup getGroup() {
        if (nodeType == NodeType.GROUP) {
            return (TopicGroup) getUserObject();
        }
        return null;
    }
    
    public TopicLine getTopicLine() {
        if (nodeType == NodeType.TOPIC_LINE) {
            return (TopicLine) getUserObject();
        }
        return null;
    }
    
    public String getDisplayName() {
        switch (nodeType) {
            case TOPIC:
                Topic topic = (Topic) getUserObject();
                return topic.name() + " (" + topic.getTotalLineCount() + " lines)";
                
            case GROUP:
                TopicGroup group = (TopicGroup) getUserObject();
                return group.name() + " (" + group.getLineCount() + " lines)";
                
            case TOPIC_LINE:
                TopicLine line = (TopicLine) getUserObject();
                String fileName = line.pathForDisplay();
                if (fileName.contains("/") || fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                }
                
                // 修改显示格式：主显示中文注释，附带类名和行号
                // 原格式：类名:行号 - 中文注释
                // 新格式：中文注释 (类名:行号)
                String pathInfo = fileName + ":" + (line.line() + 1);
                String note = line.note();
                
                if (note != null && !note.trim().isEmpty()) {
                    // 有注释：显示注释作为主要内容，类名作为附加信息
                    return note + " (" + pathInfo + ")";
                } else {
                    // 无注释：直接显示类名和行号
                    return pathInfo;
                }
                       
            case UNGROUPED_LINES_FOLDER:
                return "Ungrouped Lines";
                
            default:
                return getUserObject().toString();
        }
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    /**
     * Check if this node can have children
     */
    public boolean canHaveChildren() {
        return nodeType == NodeType.TOPIC || 
               nodeType == NodeType.GROUP || 
               nodeType == NodeType.UNGROUPED_LINES_FOLDER;
    }
    
    /**
     * Check if this node represents a container (can contain TopicLines)
     */
    public boolean isContainer() {
        return nodeType == NodeType.GROUP || 
               nodeType == NodeType.UNGROUPED_LINES_FOLDER;
    }
}
