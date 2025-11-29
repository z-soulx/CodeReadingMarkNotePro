package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Topic implements Comparable<Topic>
{
    private static final Logger LOG = Logger.getInstance(Topic.class);
    
    private String name;
    private String note;
    private Date updatedAt;
    private Project project;
    
    // 用户定义的排序顺序（用于手动排序）
    private int order = 0;
    
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
        this.order = 0; // 默认顺序
    }
    
    public Topic(Project project, String name, Date updatedAt, int order) {
        this.project = project;
        this.name = name;
        this.updatedAt = updatedAt;
        this.order = order;
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

    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    @Override
    public int compareTo(@NotNull Topic topic)
    {
        // 只按用户定义的 order 进行排序，不按 updatedAt 排序
        // 这样可以保持用户手动定义的顺序，不会因为使用而自动改变顺序
        return Integer.compare(this.order, topic.order);
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
        if (!ungroupedLines.contains(line)) {
            ungroupedLines.add(line);
        }

        if (!lines.contains(line)) {
            lines.add(line); // 保持废弃字段同步
        }

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
        
        // 发送通知以更新UI
        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        publisher.groupAdded(this, group);
        
        return group;
    }
    
    public void removeGroup(TopicGroup group) {
        // 将分组中的所有TopicLine移动到ungroupedLines
        ArrayList<TopicLine> movedLines = new ArrayList<>(group.getLines());
        for (TopicLine line : movedLines) {
            line.setGroup(null);
            ungroupedLines.add(line);
            lines.add(line); // 保持废弃字段同步
        }
        
        groups.remove(group);
        touch();
        
        // 发送通知以更新UI
        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        publisher.groupRemoved(this, group);
    }
    
    /**
     * 移动分组到新位置
     * @param fromIndex 原位置
     * @param toIndex 目标位置
     */
    public void moveGroup(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= groups.size() || 
            toIndex < 0 || toIndex >= groups.size() || 
            fromIndex == toIndex) {
            return;
        }
        
        TopicGroup group = groups.remove(fromIndex);
        groups.add(toIndex, group);
        touch();
    }
    
    /**
     * 获取分组的索引
     */
    public int getGroupIndex(TopicGroup group) {
        return groups.indexOf(group);
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

        // 发送通知（由 Topic 层统一管理）
        MessageBus messageBus = project.getMessageBus();
        TopicNotifier publisher = messageBus.syncPublisher(TopicNotifier.TOPIC_NOTIFIER_TOPIC);
        publisher.lineAdded(this, line);
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
    
    /**
     * Insert multiple TopicLines at specified position
     * Used for drag and drop and batch move operations
     * 
     * @param linesToInsert TopicLines to insert
     * @param insertIndex Insert position (-1 for end)
     */
    public void insertLines(@NotNull List<TopicLine> linesToInsert, int insertIndex) {
        if (linesToInsert.isEmpty()) {
            return;
        }
        
        // Determine actual insert position
        int actualIndex = insertIndex;
        if (actualIndex < 0 || actualIndex > lines.size()) {
            actualIndex = lines.size();
        }
        
        // Insert into lines list
        for (int i = 0; i < linesToInsert.size(); i++) {
            TopicLine line = linesToInsert.get(i);
            // Note: TopicLine.setTopic() is package-private, set via topic field directly if needed
            
            // Insert at specified position
            lines.add(actualIndex + i, line);
            
            // Add to appropriate list based on group
            if (line.getGroup() == null) {
                if (!ungroupedLines.contains(line)) {
                    ungroupedLines.add(line);
                }
            } else {
                // Ensure group belongs to current topic
                TopicGroup group = line.getGroup();
                if (!groups.contains(group)) {
                    groups.add(group);
                }
                if (!group.getLines().contains(line)) {
                    group.getLines().add(line);
                }
            }
        }
        
        touch();
    }
    
    /**
     * Reorder a TopicLine within current Topic
     * 
     * @param line TopicLine to move
     * @param newIndex New position
     */
    public void reorderLine(@NotNull TopicLine line, int newIndex) {
        int oldIndex = lines.indexOf(line);
        if (oldIndex == -1) {
            LOG.warn("Line not found in topic: " + line.url());
            return;
        }
        
        if (oldIndex == newIndex) {
            return; // No change needed
        }
        
        // Remove from old position
        lines.remove(oldIndex);
        
        // Insert at new position
        int actualIndex = newIndex;
        if (actualIndex > lines.size()) {
            actualIndex = lines.size();
        }
        lines.add(actualIndex, line);
        
        touch();
    }
}
