package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.util.messages.Topic;

/**
 * Notifier for AI config registry changes.
 */
public interface AIConfigNotifier {

    Topic<AIConfigNotifier> AI_CONFIG_TOPIC = Topic.create("AIConfigNotifier", AIConfigNotifier.class);

    /**
     * Called when the registry has been rescanned (entries added/removed/updated).
     */
    void registryUpdated();

    /**
     * Called when a tracked file's content has changed.
     */
    void fileChanged(AIConfigEntry entry);
}
