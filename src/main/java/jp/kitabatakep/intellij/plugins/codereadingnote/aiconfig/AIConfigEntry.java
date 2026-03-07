package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents one tracked AI config file in the project.
 */
public class AIConfigEntry {

    private final String id;
    private final AIConfigType type;
    private final String relativePath;
    private boolean tracked;
    private String contentHash;
    private long lastModified;

    public AIConfigEntry(@NotNull AIConfigType type, @NotNull String relativePath) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.relativePath = relativePath.replace('\\', '/');
        this.tracked = true;
        this.contentHash = "";
        this.lastModified = 0;
    }

    public AIConfigEntry(@NotNull String id, @NotNull AIConfigType type, @NotNull String relativePath,
                         boolean tracked, @NotNull String contentHash, long lastModified) {
        this.id = id;
        this.type = type;
        this.relativePath = relativePath.replace('\\', '/');
        this.tracked = tracked;
        this.contentHash = contentHash;
        this.lastModified = lastModified;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public AIConfigType getType() {
        return type;
    }

    @NotNull
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Returns the file name portion of the relative path.
     */
    @NotNull
    public String getFileName() {
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
    }

    /**
     * Returns the parent directory portion of the relative path.
     */
    @NotNull
    public String getParentDir() {
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(0, lastSlash) : "";
    }

    public boolean isTracked() {
        return tracked;
    }

    public void setTracked(boolean tracked) {
        this.tracked = tracked;
    }

    @NotNull
    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(@NotNull String contentHash) {
        this.contentHash = contentHash;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIConfigEntry that = (AIConfigEntry) o;
        return Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath);
    }

    @Override
    public String toString() {
        return String.format("AIConfigEntry{type=%s, path=%s, tracked=%s}", type, relativePath, tracked);
    }
}
