package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Represents a group within a Topic for organizing TopicLines
 * This is the intermediate layer between Topic and TopicLine
 */
public class TopicGroup implements Comparable<TopicGroup> {
    private String name;
    private String note;
    private Date createdAt;
    private Date updatedAt;
    private ArrayList<TopicLine> lines = new ArrayList<>();
    private boolean expanded = false; // 默认收缩状态，用户可以手动展开需要的分组
    private Topic parentTopic;
    private Project project;

    public TopicGroup(Project project, Topic parentTopic, String name, Date createdAt) {
        this.project = project;
        this.parentTopic = parentTopic;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String note() {
        return note != null ? note : "";
    }

    public void setNote(String note) {
        this.note = note;
        touch();
    }

    public Date createdAt() {
        return createdAt;
    }

    public Date updatedAt() {
        return updatedAt;
    }

    public void touch() {
        updatedAt = new Date();
        if (parentTopic != null) {
            parentTopic.touch();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public Topic getParentTopic() {
        return parentTopic;
    }

    public void setParentTopic(Topic parentTopic) {
        this.parentTopic = parentTopic;
    }

    @Override
    public int compareTo(@NotNull TopicGroup other) {
        // Sort by creation time, newer first
        return other.createdAt().compareTo(this.createdAt);
    }

    public void addLine(TopicLine line) {
        lines.add(line);
        // 设置TopicLine的分组引用
        line.setGroup(this);
        touch();
        
        // 注意：不在这里发送 lineAdded 通知
        // 通知应该由 Topic 层统一发送，避免重复通知
    }

    public void removeLine(TopicLine line) {
        lines.remove(line);
        // 清除TopicLine的分组引用
        line.setGroup(null);
        touch();
        
        // 注意：不在这里发送 lineRemoved 通知
        // 通知应该由 Topic 层统一发送，避免重复通知
    }

    public Iterator<TopicLine> linesIterator() {
        return lines.iterator();
    }

    public ArrayList<TopicLine> getLines() {
        return lines;
    }

    public void setLines(ArrayList<TopicLine> lines) {
        this.lines = lines;
        // 设置所有TopicLine的分组引用
        for (TopicLine line : lines) {
            line.setGroup(this);
        }
        touch();
    }

    public void changeLineOrder(TopicLine line, int index) {
        lines.remove(line);
        lines.add(index, line);
        touch();
    }

    public int getLineCount() {
        return lines.size();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Override
    public String toString() {
        return name + " (" + lines.size() + " lines)";
    }
}
