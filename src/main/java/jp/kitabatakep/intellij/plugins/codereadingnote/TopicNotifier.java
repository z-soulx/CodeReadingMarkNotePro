package jp.kitabatakep.intellij.plugins.codereadingnote;

public interface TopicNotifier
{
    com.intellij.util.messages.Topic<TopicNotifier> TOPIC_UI_OP_NOTIFIER_TOPIC =
        com.intellij.util.messages.Topic.create("topic ui op notifier", TopicNotifier.class);

    com.intellij.util.messages.Topic<TopicNotifier> TOPIC_OP_EXTENSION_NOTIFIER_TOPIC =
        com.intellij.util.messages.Topic.create("topic Operational extensions notifier", TopicNotifier.class);

    void lineRemoved(Topic topic, TopicLine topicLine);
    void lineAdded(Topic topic, TopicLine topicLine);
}
