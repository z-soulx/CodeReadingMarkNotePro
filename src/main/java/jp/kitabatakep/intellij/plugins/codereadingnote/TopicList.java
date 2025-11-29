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
    private ArrayList<Topic> topics = new ArrayList<>();

    public TopicList(Project project)
    {
        this.project = project;
    }

    public void addTopic(String name)
    {
        // 设置 order 为当前 topics 数量，新 topic 会排在最后
        int order = topics.size();
        Topic topic = new Topic(project, name, new Date(), order);
        topics.add(topic);

        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        publisher.topicAdded(topic);
    }

    public void removeTopic(Topic topic)
    {
        topics.remove(topic);
        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        publisher.topicRemoved(topic);
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
        publisher.topicsReordered();
    }

    public void setTopics(ArrayList<Topic> topics)
    {
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
