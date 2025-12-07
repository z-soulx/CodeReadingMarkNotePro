package jp.kitabatakep.intellij.plugins.codereadingnote;

public interface TopicListNotifier
{
    com.intellij.util.messages.Topic<TopicListNotifier> TOPIC_LIST_NOTIFIER_TOPIC =
        com.intellij.util.messages.Topic.create("topic list notifier", TopicListNotifier.class);

     void topicAdded(Topic topic);
     void topicRemoved(Topic topic);
     void topicsLoaded();
     
     /**
      * Called when a topic is renamed or note is updated
      */
     default void topicUpdated(Topic topic) {}
     
     /**
      * Called when topics are reordered (e.g., via drag and drop)
      */
     default void topicsReordered() {}
}
