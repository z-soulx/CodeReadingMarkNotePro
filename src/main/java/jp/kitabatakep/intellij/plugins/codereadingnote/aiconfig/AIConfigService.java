package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Project-level service that manages AI config file discovery, tracking, and persistence.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "AIConfigService",
    storages = @Storage("aiConfigRegistry.xml")
)
public final class AIConfigService implements PersistentStateComponent<AIConfigService.PersistentState> {

    private static final Logger LOG = Logger.getInstance(AIConfigService.class);

    private final Project project;
    private final AIConfigRegistry registry;
    private PersistentState persistentState = new PersistentState();
    private boolean initialized = false;

    public AIConfigService(@NotNull Project project) {
        this.project = project;
        this.registry = new AIConfigRegistry(project);
        setupVfsListener();
    }

    @NotNull
    public static AIConfigService getInstance(@NotNull Project project) {
        return project.getService(AIConfigService.class);
    }

    @NotNull
    public AIConfigRegistry getRegistry() {
        ensureInitialized();
        return registry;
    }

    /**
     * Triggers a full rescan of AI config files and notifies listeners.
     */
    public void rescan() {
        registry.scan();
        notifyRegistryUpdated();
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            restoreState();
            registry.scan();
            reconcileTrackedState();
        }
    }

    private void restoreState() {
        if (persistentState.customPaths != null) {
            registry.setCustomPaths(new LinkedHashSet<>(persistentState.customPaths));
        }
        if (persistentState.ignorePatterns != null) {
            registry.setUserIgnorePatterns(persistentState.ignorePatterns);
        }
    }

    /**
     * Applies persisted tracked state to scanned entries.
     * Called ONCE during initialization after scan — NOT on runtime rescans,
     * because mergeDiscoveredEntries() already preserves runtime tracked state.
     */
    private void reconcileTrackedState() {
        if (persistentState.trackedEntries == null || persistentState.trackedEntries.isEmpty()) {
            LOG.debug("No persisted tracked entries to reconcile");
            return;
        }
        Map<String, Boolean> trackedMap = new HashMap<>();
        for (EntryState es : persistentState.trackedEntries) {
            trackedMap.put(es.relativePath, es.tracked);
        }
        int applied = 0;
        for (AIConfigEntry entry : registry.getEntries()) {
            Boolean tracked = trackedMap.get(entry.getRelativePath());
            if (tracked != null) {
                entry.setTracked(tracked);
                applied++;
            }
        }
        LOG.info("Reconciled tracked state: " + applied + " entries from " + trackedMap.size() + " persisted");
    }

    private void setupVfsListener() {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                if (!initialized) return;

                boolean needsRescan = false;
                Set<String> changedPaths = new HashSet<>();

                for (VFileEvent event : events) {
                    VirtualFile file = event.getFile();
                    if (file == null) continue;

                    String relativePath = getRelativePathForFile(file);
                    if (relativePath == null) continue;

                    if (isAIConfigPath(relativePath)) {
                        if (event instanceof VFileCreateEvent || event instanceof VFileDeleteEvent
                                || event instanceof VFileMoveEvent || event instanceof VFileCopyEvent) {
                            needsRescan = true;
                        } else if (event instanceof VFileContentChangeEvent) {
                            changedPaths.add(relativePath);
                        }
                    }
                }

                if (needsRescan) {
                    rescan();
                } else if (!changedPaths.isEmpty()) {
                    handleContentChanges(changedPaths);
                }
            }
        });
    }

    @Nullable
    private String getRelativePathForFile(@NotNull VirtualFile file) {
        String basePath = project.getBasePath();
        if (basePath == null) return null;
        String filePath = file.getPath().replace('\\', '/');
        String base = basePath.replace('\\', '/');
        if (!base.endsWith("/")) base += "/";
        if (filePath.startsWith(base)) {
            return filePath.substring(base.length());
        }
        return null;
    }

    private boolean isAIConfigPath(@NotNull String relativePath) {
        for (AIConfigType type : AIConfigType.getKnownTypes()) {
            String defaultPath = type.getDefaultPath();
            if (defaultPath != null && relativePath.startsWith(defaultPath.replace('\\', '/'))) {
                return true;
            }
        }
        for (String customPath : registry.getCustomPaths()) {
            if (relativePath.startsWith(customPath)) {
                return true;
            }
        }
        return false;
    }

    private void handleContentChanges(@NotNull Set<String> changedPaths) {
        for (String path : changedPaths) {
            AIConfigEntry entry = registry.findByPath(path);
            if (entry != null) {
                registry.scan(); // Re-read hashes
                MessageBus messageBus = project.getMessageBus();
                AIConfigNotifier publisher = messageBus.syncPublisher(AIConfigNotifier.AI_CONFIG_TOPIC);
                publisher.fileChanged(entry);
                break; // scan() already refreshes everything
            }
        }
    }

    private void notifyRegistryUpdated() {
        // Do NOT call reconcileTrackedState() here — mergeDiscoveredEntries() already
        // preserves tracked state on existing entries. Calling reconcile on every rescan
        // would revert runtime changes the user hasn't saved yet.
        MessageBus messageBus = project.getMessageBus();
        AIConfigNotifier publisher = messageBus.syncPublisher(AIConfigNotifier.AI_CONFIG_TOPIC);
        publisher.registryUpdated();
    }

    @NotNull
    public String getLastPushedHash() {
        return persistentState.lastPushedHash != null ? persistentState.lastPushedHash : "";
    }

    public void setLastPushedHash(@NotNull String hash) {
        persistentState.lastPushedHash = hash;
    }

    @NotNull
    public Map<String, String> getLastPushedFileHashes() {
        if (persistentState.lastPushedFileHashes == null) return new HashMap<>();
        Map<String, String> result = new HashMap<>();
        for (FileHashEntry fh : persistentState.lastPushedFileHashes) {
            result.put(fh.relativePath, fh.contentHash);
        }
        return result;
    }

    public void setLastPushedFileHashes(@NotNull Map<String, String> hashes) {
        persistentState.lastPushedFileHashes = new ArrayList<>();
        for (Map.Entry<String, String> e : hashes.entrySet()) {
            FileHashEntry fh = new FileHashEntry();
            fh.relativePath = e.getKey();
            fh.contentHash = e.getValue();
            persistentState.lastPushedFileHashes.add(fh);
        }
    }

    // --- PersistentStateComponent ---

    @Override
    @Nullable
    public PersistentState getState() {
        if (!initialized) {
            // Registry not yet initialized — return loaded state as-is to avoid
            // wiping out persisted tracked entries with an empty list.
            return persistentState;
        }
        persistentState.customPaths = new ArrayList<>(registry.getCustomPaths());
        persistentState.ignorePatterns = new ArrayList<>(registry.getUserIgnorePatterns());
        persistentState.trackedEntries = new ArrayList<>();
        for (AIConfigEntry entry : registry.getEntries()) {
            EntryState es = new EntryState();
            es.relativePath = entry.getRelativePath();
            es.tracked = entry.isTracked();
            es.typeName = entry.getType().name();
            persistentState.trackedEntries.add(es);
        }
        return persistentState;
    }

    @Override
    public void loadState(@NotNull PersistentState state) {
        XmlSerializerUtil.copyBean(state, this.persistentState);
    }

    /**
     * Persistent state bean for XML serialization.
     */
    public static class PersistentState {
        public List<String> customPaths = new ArrayList<>();
        public List<String> ignorePatterns = new ArrayList<>();
        public List<EntryState> trackedEntries = new ArrayList<>();
        public String lastPushedHash = "";
        public List<FileHashEntry> lastPushedFileHashes = new ArrayList<>();
    }

    public static class EntryState {
        public String relativePath = "";
        public boolean tracked = true;
        public String typeName = "CUSTOM";
    }

    public static class FileHashEntry {
        public String relativePath = "";
        public String contentHash = "";
    }
}
