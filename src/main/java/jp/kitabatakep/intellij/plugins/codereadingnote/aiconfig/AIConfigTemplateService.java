package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application-level service for managing AI config templates.
 * Templates are stored persistently and available across all projects.
 */
@State(
    name = "AIConfigTemplateService",
    storages = @Storage("aiConfigTemplates.xml")
)
public class AIConfigTemplateService implements PersistentStateComponent<AIConfigTemplateService.TemplateState> {

    private static final Logger LOG = Logger.getInstance(AIConfigTemplateService.class);

    private TemplateState state = new TemplateState();

    @NotNull
    public static AIConfigTemplateService getInstance() {
        return ApplicationManager.getApplication().getService(AIConfigTemplateService.class);
    }

    @Override
    @Nullable
    public TemplateState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull TemplateState state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }

    @NotNull
    public List<AIConfigTemplate> getTemplates() {
        return state.templates != null ? Collections.unmodifiableList(state.templates) : Collections.emptyList();
    }

    @Nullable
    public AIConfigTemplate findById(@NotNull String id) {
        return getTemplates().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public List<AIConfigTemplate> findByTag(@NotNull String tag) {
        return getTemplates().stream()
                .filter(t -> t.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    /**
     * Save a new template from the current project's tracked AI config files.
     */
    @NotNull
    public AIConfigTemplate saveFromProject(@NotNull Project project, @NotNull String name,
                                             @NotNull String description, @NotNull List<String> tags) {
        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();

        AIConfigTemplate template = new AIConfigTemplate(name, description, tags);
        List<AIConfigTemplate.TemplateFileEntry> entries = new ArrayList<>();

        for (AIConfigEntry configEntry : registry.getTrackedEntries()) {
            String content = registry.readFileContent(configEntry);
            if (content != null) {
                entries.add(new AIConfigTemplate.TemplateFileEntry(
                    configEntry.getRelativePath(), content, configEntry.getType()));
            }
        }

        template.setEntries(entries);
        addTemplate(template);
        return template;
    }

    /**
     * Save specific entries as a template.
     */
    @NotNull
    public AIConfigTemplate saveFromEntries(@NotNull Project project, @NotNull List<AIConfigEntry> selectedEntries,
                                            @NotNull String name, @NotNull String description,
                                            @NotNull List<String> tags) {
        AIConfigRegistry registry = AIConfigService.getInstance(project).getRegistry();
        AIConfigTemplate template = new AIConfigTemplate(name, description, tags);
        List<AIConfigTemplate.TemplateFileEntry> entries = new ArrayList<>();

        for (AIConfigEntry configEntry : selectedEntries) {
            String content = registry.readFileContent(configEntry);
            if (content != null) {
                entries.add(new AIConfigTemplate.TemplateFileEntry(
                    configEntry.getRelativePath(), content, configEntry.getType()));
            }
        }

        template.setEntries(entries);
        addTemplate(template);
        return template;
    }

    /**
     * Apply a template to the given project, writing files to disk.
     *
     * @param mode OVERWRITE replaces existing files, SKIP leaves them, MERGE is reserved for future use
     * @return number of files written
     */
    public int applyToProject(@NotNull Project project, @NotNull AIConfigTemplate template,
                               @NotNull ApplyMode mode) {
        String basePath = project.getBasePath();
        if (basePath == null) return 0;

        int written = 0;
        for (AIConfigTemplate.TemplateFileEntry entry : template.getEntries()) {
            File targetFile = new File(basePath, entry.getRelativePath());

            if (targetFile.exists() && mode == ApplyMode.SKIP) {
                continue;
            }

            try {
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.write(targetFile.toPath(), entry.getContent().getBytes(StandardCharsets.UTF_8));
                written++;
            } catch (Exception e) {
                LOG.warn("Failed to apply template file: " + entry.getRelativePath(), e);
            }
        }

        // Trigger rescan
        AIConfigService.getInstance(project).rescan();
        return written;
    }

    public void addTemplate(@NotNull AIConfigTemplate template) {
        if (state.templates == null) state.templates = new ArrayList<>();
        state.templates.add(template);
    }

    public void removeTemplate(@NotNull String id) {
        if (state.templates == null) return;
        state.templates.removeIf(t -> t.getId().equals(id));
    }

    public void updateTemplate(@NotNull AIConfigTemplate updated) {
        if (state.templates == null) return;
        for (int i = 0; i < state.templates.size(); i++) {
            if (state.templates.get(i).getId().equals(updated.getId())) {
                state.templates.set(i, updated);
                return;
            }
        }
    }

    public enum ApplyMode {
        OVERWRITE,
        SKIP
    }

    public static class TemplateState {
        public List<AIConfigTemplate> templates = new ArrayList<>();
    }
}
