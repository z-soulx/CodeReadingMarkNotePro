package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

public class TopicList
{
    private Project project;
    private final jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext context;
    public jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext context() { return context; }
    public TopicList(Project project, jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext context) {
        this.project = project;
        this.context = context;
    }
    public void rebind(Project project) {
        this.project = project;
        for (Topic topic : topics) topic.rebind(project);
        for (TrashedLine trash : trashedLines) trash.getLine().rebind(project);
    }
    private ArrayList<Topic> topics = new ArrayList<>();
    private ArrayList<TrashedLine> trashedLines = new ArrayList<>();
    private org.jdom.Element xmlTemplate = new org.jdom.Element("topics");
    public org.jdom.Element xmlTemplate() { return xmlTemplate.clone(); }
    public void setXmlTemplate(org.jdom.Element value) { xmlTemplate = value.clone(); }

    public TopicList(Project project)
    {
        this(project, new jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext(java.nio.file.Path.of(project.getBasePath())));
    }

    public void addTopic(String name)
    {
        int order = topics.size();
        Topic topic = new Topic(project, context, name, new Date(), order);
        topic.ownerList = this;
        topics.add(topic);
        context.changed();

        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        publisher.topicAdded(topic);
    }

    public void removeTopic(Topic topic)
    {
        if (!topics.remove(topic)) return;
        context.changed();
        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        publisher.topicRemoved(topic);
    }

    // ========== Trash Bin ==========

    public void moveToTrash(TopicLine line, String topicName) {
        trashedLines.add(new TrashedLine(line, topicName, new Date()));
        notifyTrashChanged();
    }

    public void restoreFromTrash(TrashedLine trashedLine) {
        if (!trashedLines.contains(trashedLine)) return;
        Topic target = null;
        for (Topic t : topics) {
            if (t.name().equals(trashedLine.getOriginalTopicName())) {
                target = t;
                break;
            }
        }
        if (target == null) {
            addTopic(trashedLine.getOriginalTopicName());
            target = topics.get(topics.size() - 1);
        }
        if (target != null) {
            target.addLine(trashedLine.getLine());
            trashedLines.remove(trashedLine);
        }
        notifyTrashChanged();
    }

    public void permanentlyDelete(TrashedLine trashedLine) {
        trashedLines.remove(trashedLine);
        notifyTrashChanged();
    }

    public void emptyTrash() {
        trashedLines.clear();
        notifyTrashChanged();
    }

    public ArrayList<TrashedLine> getTrashedLines() {
        return trashedLines;
    }

    public void setTrashedLines(ArrayList<TrashedLine> trashedLines) {
        for (TrashedLine trash : trashedLines) trash.getLine().topic().adoptContext(context);
        this.trashedLines = trashedLines;
    }

    private void notifyTrashChanged() {
        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        context.changed();
        publisher.trashChanged(this);
    }

    public Iterator<Topic> iterator()
    {
        // 不再自动排序，保持用户定义的顺序
        // Collections.sort(topics);  // 已禁用自动排序
        return topics.iterator();
    }
    
    /**
     * 重新分配 topics 的 order 值，按当前列表顺序
     * 注意：不要在这里调用 sort，因为列表顺序已经是用户期望的顺序
     */
    public void reorderTopics() {
        // 直接按当前列表顺序分配 order 值，不要排序！
        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setOrder(i);
        }
    }
    
    /**
     * 移动 topic 到新位置
     */
    public void moveTopic(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= topics.size() || 
            toIndex < 0 || toIndex >= topics.size() || 
            fromIndex == toIndex) {
            return;
        }
        
        Topic topic = topics.remove(fromIndex);
        topics.add(toIndex, topic);
        
        // 重新分配 order 值
        reorderTopics();
        
        // 发送通知以触发持久化保存
        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        context.changed();
        publisher.topicsReordered(this);
    }

    public void setTopics(ArrayList<Topic> topics)
    {
        for (Topic topic : topics) { topic.adoptContext(context); topic.ownerList = this; }
        this.topics = topics;
    }

    public ArrayList<Topic> getTopics() {
        return topics;
    }
    
    /**
     * Refresh all TopicLine file references.
     * This is useful when switching branches or when files might have become available again.
     * @return the number of TopicLines that were successfully refreshed (went from invalid to valid)
     */
    public int refreshAllTopicLines() {
        int refreshedCount = 0;
        for (Topic topic : topics) {
            for (TopicLine line : topic.getLines()) {
                // Only try to refresh if currently invalid
                if (!line.isValid()) {
                    if (line.refreshFile()) {
                        refreshedCount++;
                    }
                }
            }
        }
        return refreshedCount;
    }
}
