package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class Topic implements Comparable<Topic>
{
    private String name;
    private String note;
    private Date updatedAt;
    private Project project;
    
    // 分组管理
    private ArrayList<TopicGroup> groups = new ArrayList<>();
    
    // 保持向后兼容：没有分组的TopicLine直接属于Topic
    private ArrayList<TopicLine> ungroupedLines = new ArrayList<>();
    
    // 废弃的字段，保持向后兼容
    @Deprecated
    private ArrayList<TopicLine> lines = new ArrayList<>();

    public Topic(Project project, String name, Date updatedAt) {
        this.project = project;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public String name()
    {
        return name;
    }
    public void setName(String name) { this.name = name; }

    public String note() {
        return note != null ? note : "";
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public Date updatedAt() { return updatedAt; }

    public void touch()
    {
        updatedAt = new Date();
    }

    @Override
    public int compareTo(@NotNull Topic topic)
    {
        return topic.updatedAt().compareTo(updatedAt);
    }

    public void setLines(ArrayList<TopicLine> lines)
    {
        // 为了向后兼容，将传入的lines设置为ungroupedLines
        this.ungroupedLines = lines;
        this.lines = lines; // 保持废弃字段同步
        // 清除所有TopicLine的分组引用
        for (TopicLine line : lines) {
            line.setGroup(null);
        }
    }

    public void addLine(TopicLine line)
    {
        // 默认添加到ungroupedLines（保持历史兼容）
        ungroupedLines.add(line);
        lines.add(line); // 保持废弃字段同步
        line.setGroup(null); // 确保没有分组引用
        updatedAt = new Date();

        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        publisher.lineAdded(this, line);
    }

    public void removeLine(TopicLine line)
    {
        // 从ungroupedLines中移除
        ungroupedLines.remove(line);
        lines.remove(line); // 保持废弃字段同步
        
        // 从所有分组中移除
        for (TopicGroup group : groups) {
            group.removeLine(line);
        }
        
        updatedAt = new Date();

        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        publisher.lineRemoved(this, line);
    }

    public Iterator<TopicLine> linesIterator()
    {
        return getLines().iterator();
    }

    public ArrayList<TopicLine> getLines() {
        // 返回所有TopicLine（包括分组和未分组的）
        ArrayList<TopicLine> allLines = new ArrayList<>(ungroupedLines);
        for (TopicGroup group : groups) {
            allLines.addAll(group.getLines());
        }
        return allLines;
    }

    public void changeLineOrder(TopicLine line, int index)
    {
        if (line.hasGroup()) {
            // 如果有分组，在分组内调整顺序
            line.getGroup().changeLineOrder(line, index);
        } else {
            // 在ungroupedLines中调整顺序
            ungroupedLines.remove(line);
            ungroupedLines.add(index, line);
            
            // 保持废弃字段同步
            lines.remove(line);
            lines.add(index, line);
        }
    }

    // ========== 分组管理方法 ==========
    
    public ArrayList<TopicGroup> getGroups() {
        return groups;
    }
    
    public void setGroups(ArrayList<TopicGroup> groups) {
        this.groups = groups;
        touch();
    }
    
    public ArrayList<TopicLine> getUngroupedLines() {
        return ungroupedLines;
    }
    
    public void setUngroupedLines(ArrayList<TopicLine> ungroupedLines) {
        this.ungroupedLines = ungroupedLines;
        this.lines = new ArrayList<>(ungroupedLines); // 保持废弃字段同步
        // 清除所有TopicLine的分组引用
        for (TopicLine line : ungroupedLines) {
            line.setGroup(null);
        }
        touch();
    }
    
    public TopicGroup addGroup(String groupName) {
        // 检查是否已存在同名分组
        TopicGroup existingGroup = findGroupByName(groupName);
        if (existingGroup != null) {
            return existingGroup;
        }
        
        TopicGroup group = new TopicGroup(project, this, groupName, new Date());
        groups.add(group);
        touch();
        
        return group;
    }
    
    public void removeGroup(TopicGroup group) {
        // 将分组中的所有TopicLine移动到ungroupedLines
        for (TopicLine line : group.getLines()) {
            line.setGroup(null);
            ungroupedLines.add(line);
            lines.add(line); // 保持废弃字段同步
        }
        
        groups.remove(group);
        touch();
    }
    
    public TopicGroup findGroupByName(String name) {
        return groups.stream()
                .filter(group -> group.name().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    public void moveLineToGroup(TopicLine line, TopicGroup targetGroup) {
        // 从当前位置移除
        ungroupedLines.remove(line);
        lines.remove(line); // 保持废弃字段同步
        if (line.hasGroup()) {
            line.getGroup().removeLine(line);
        }
        
        // 添加到目标分组
        targetGroup.addLine(line);
        touch();
    }
    
    public void moveLineToUngrouped(TopicLine line) {
        // 从分组中移除
        if (line.hasGroup()) {
            line.getGroup().removeLine(line);
        }
        
        // 添加到ungroupedLines
        if (!ungroupedLines.contains(line)) {
            ungroupedLines.add(line);
            lines.add(line); // 保持废弃字段同步
            line.setGroup(null);
        }
        touch();
    }
    
    public void addLineToGroup(TopicLine line, String groupName) {
        TopicGroup group = findGroupByName(groupName);
        if (group == null) {
            group = addGroup(groupName);
        }
        moveLineToGroup(line, group);
    }
    
    /**
     * 获取总的代码行数（包括分组和未分组）
     */
    public int getTotalLineCount() {
        int count = ungroupedLines.size();
        for (TopicGroup group : groups) {
            count += group.getLineCount();
        }
        return count;
    }
    
    /**
     * 获取所有代码行（用于搜索和兼容性）
     */
    public ArrayList<TopicLine> getAllLines() {
        return getLines();
    }
}
