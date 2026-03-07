package jp.kitabatakep.intellij.plugins.codereadingnote;

import java.util.Date;

/**
 * Wrapper for a TopicLine that has been moved to the trash bin.
 * Stores the original topic name and deletion date for potential restoration.
 */
public class TrashedLine {
    private final TopicLine line;
    private final String originalTopicName;
    private final Date trashedAt;

    public TrashedLine(TopicLine line, String originalTopicName, Date trashedAt) {
        this.line = line;
        this.originalTopicName = originalTopicName;
        this.trashedAt = trashedAt;
    }

    public TopicLine getLine() {
        return line;
    }

    public String getOriginalTopicName() {
        return originalTopicName;
    }

    public Date getTrashedAt() {
        return trashedAt;
    }
}
