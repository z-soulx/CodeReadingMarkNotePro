package jp.kitabatakep.intellij.plugins.codereadingnote;

public interface TopicNotifier
{
    com.intellij.util.messages.Topic<TopicNotifier> TOPIC_NOTIFIER_TOPIC =
        com.intellij.util.messages.Topic.create("topic notifier", TopicNotifier.class);

    void lineRemoved(Topic topic, TopicLine topicLine);
    void lineAdded(Topic topic, TopicLine topicLine);
    
    // Group-related events
    default void groupAdded(Topic topic, TopicGroup group) {}
    default void groupRemoved(Topic topic, TopicGroup group) {}
    default void groupRenamed(Topic topic, TopicGroup group) {}
}
