package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a reusable AI config template that can be applied to projects.
 * Stored at application level for cross-project use.
 */
public class AIConfigTemplate {

    private String id;
    private String name;
    private String description;
    private List<String> tags;
    private List<TemplateFileEntry> entries;
    private long createdAt;
    private long updatedAt;

    public AIConfigTemplate() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.description = "";
        this.tags = new ArrayList<>();
        this.entries = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public AIConfigTemplate(@NotNull String name, @NotNull String description, @NotNull List<String> tags) {
        this();
        this.name = name;
        this.description = description;
        this.tags = new ArrayList<>(tags);
    }

    @NotNull
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return name != null ? name : "";
    }

    public void setName(@NotNull String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    @NotNull
    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    public List<String> getTags() {
        return tags != null ? tags : Collections.emptyList();
    }

    public void setTags(@NotNull List<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    @NotNull
    public List<TemplateFileEntry> getEntries() {
        return entries != null ? entries : Collections.emptyList();
    }

    public void setEntries(@NotNull List<TemplateFileEntry> entries) {
        this.entries = new ArrayList<>(entries);
        this.updatedAt = System.currentTimeMillis();
    }

    public void addEntry(@NotNull TemplateFileEntry entry) {
        if (this.entries == null) this.entries = new ArrayList<>();
        this.entries.add(entry);
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * A single file within a template.
     */
    public static class TemplateFileEntry {
        private String relativePath;
        private String content;
        private String typeName;

        public TemplateFileEntry() {
            this.relativePath = "";
            this.content = "";
            this.typeName = "CUSTOM";
        }

        public TemplateFileEntry(@NotNull String relativePath, @NotNull String content, @NotNull AIConfigType type) {
            this.relativePath = relativePath;
            this.content = content;
            this.typeName = type.name();
        }

        @NotNull
        public String getRelativePath() {
            return relativePath != null ? relativePath : "";
        }

        public void setRelativePath(@NotNull String relativePath) {
            this.relativePath = relativePath;
        }

        @NotNull
        public String getContent() {
            return content != null ? content : "";
        }

        public void setContent(@NotNull String content) {
            this.content = content;
        }

        @NotNull
        public String getTypeName() {
            return typeName != null ? typeName : "CUSTOM";
        }

        public void setTypeName(@NotNull String typeName) {
            this.typeName = typeName;
        }

        @NotNull
        public AIConfigType getType() {
            try {
                return AIConfigType.valueOf(getTypeName());
            } catch (IllegalArgumentException e) {
                return AIConfigType.CUSTOM;
            }
        }
    }
}
