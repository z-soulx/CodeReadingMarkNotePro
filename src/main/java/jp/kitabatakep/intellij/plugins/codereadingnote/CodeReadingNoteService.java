package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.AutoSyncScheduler;
import org.jetbrains.annotations.NotNull;

import org.jdom.Element;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.CodeRemark;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;

@State(
    name = AppConstants.appName,
    storages = {
        @Storage(AppConstants.appName + ".xml"),
    }
)
public class CodeReadingNoteService implements PersistentStateComponent<Element>
{
    private static final Logger LOG = Logger.getInstance(CodeReadingNoteService.class);
    
    Project project;
    TopicList topicList;
    private final Alarm remarkRefreshAlarm;

    String lastExportDir = "";
    String lastImportDir = "";
    
    // Flag to prevent recursive auto-sync trigger during MD5 calculation
    private final ThreadLocal<Boolean> isCalculatingState = ThreadLocal.withInitial(() -> false);
    
    /**
     * 检查是否应该处理数据修改
     * @return true 表示应该处理，false 表示不处理
     */
    private boolean shouldHandleDataModification() {
        if (project.isDisposed()) {
            return false;
        }
        
        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig config = 
            jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings.getInstance().getSyncConfig();
        
        if (!config.isEnabled() || !config.isAutoSync()) {
            return false;
        }
        
        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService statusService = 
            jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService.getInstance(project);
        
        // 只有不在同步中和冲突暂停状态时才处理
        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatus currentStatus = statusService.getCurrentStatus();
        if (currentStatus == jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatus.SYNCING 
            || currentStatus == jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatus.CONFLICT_PAUSED) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 标记数据为脏状态（已修改但未持久化）
     */
    private void markDataAsDirty() {
        // 1. 检查是否应该处理
        if (!shouldHandleDataModification()) {
            return;
        }
        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService statusService = 
            jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService.getInstance(project);
        statusService.markDirty();
        LOG.debug("Data modified - marked as DIRTY (waiting for IDE to persist)");
    }
    
    /**
     * 直接触发自动同步调度
     * （注释掉，先测试依赖文件持久化的效果）
     */
    private void triggerDirectAutoSync() {
        //triggerAutoSyncOnStateSave
        // scheduleAutoSync();
        // LOG.debug("Data modified - scheduled auto-sync directly");
    }
    
    /**
     * 统一的数据修改触发处理
     * 当数据发生任何修改时调用此方法
     * 
     * 职责：协调各个处理步骤
     */
    private void onDataModified() {
        try {

            
            // 2. 标记为脏状态
            markDataAsDirty();
            
            // 3. 触发自动同步（可选）
            triggerDirectAutoSync();
            
        } catch (Exception e) {
            LOG.debug("Failed to handle data modification", e);
        }
    }

    public CodeReadingNoteService(@NotNull Project project)
    {
        this.project = project;
        topicList = new TopicList(project);
        remarkRefreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
        starConfit(project);
        addMyListener(project);
        addVfsListener(project);
    }

    private void starConfit(Project project) {

    }
    
    /**
     * Add VFS listener to refresh TopicLines when files become available.
     * This helps when switching branches or when files are created.
     */
    private void addVfsListener(Project project) {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                boolean needsRefresh = false;
                
                for (VFileEvent event : events) {
                    // Check for file creation, copy, move, or property change (like VFS refresh)
                    if (event instanceof VFileCreateEvent ||
                        event instanceof VFileCopyEvent ||
                        event instanceof VFileMoveEvent ||
                        event instanceof VFilePropertyChangeEvent) {
                        needsRefresh = true;
                        break;
                    }
                }
                
                if (needsRefresh) {
                    // Refresh all TopicLines and notify UI if any were refreshed
                    int refreshed = topicList.refreshAllTopicLines();
                    if (refreshed > 0) {
                        LOG.info("Refreshed " + refreshed + " TopicLine file references after VFS change");
                        // Notify UI to refresh
                        notifyTopicsNeedRefresh();
                    }
                }
            }
        });
    }
    
    /**
     * Notify UI that topics need to be refreshed (e.g., after file references are updated)
     */
    private void notifyTopicsNeedRefresh() {
        MessageBus messageBus = project.getMessageBus();
        TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
        publisher.topicsLoaded(); // Reuse existing event to trigger UI refresh
    }
    
    /**
     * Manually refresh all TopicLine file references.
     * Call this when you suspect files might have become available (e.g., after branch switch).
     * @return the number of TopicLines that were refreshed
     */
    public int refreshAllTopicLineFiles() {
        int refreshed = topicList.refreshAllTopicLines();
        if (refreshed > 0) {
            notifyTopicsNeedRefresh();
        }
        return refreshed;
    }

    private void addMyListener(Project project) {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
            @Override
            public void lineRemoved(Topic _topic, TopicLine _topicLine) {
                    EditorUtils.removeLineCodeRemark(project, _topicLine);
                    topicList.moveToTrash(_topicLine, _topic.name());
                    onDataModified();
            }

            @Override
            public void lineAdded(Topic _topic, TopicLine _topicLine) {
                    // 统一处理数据修改
                    onDataModified();
                    // Check if TopicLine already has a UUID
                    // This prevents re-generating UUID when line number is updated (which incorrectly triggers lineAdded event)
                    String uid = _topicLine.getBookmarkUid();
                    if (uid == null || uid.isEmpty()) {
                        // Generate new UUID only if TopicLine doesn't have one (truly new line)
                        uid = UUID.randomUUID().toString();
                        Bookmark bookmark = BookmarkUtils.addBookmark(project, _topicLine.file(), _topicLine.line(), _topicLine.note(), uid);
                        if (bookmark != null) {
                            _topicLine.setBookmarkUid(uid);
                        }
                    } else {
                        // TopicLine already has UUID (e.g., after line number update)
                        // Just ensure bookmark exists with the existing UUID
                        Bookmark existingBookmark = BookmarkUtils.machBookmark(_topicLine, project);
                        if (existingBookmark == null) {
                            // Bookmark missing, recreate it with existing UUID
                            Bookmark bookmark = BookmarkUtils.addBookmark(project, _topicLine.file(), _topicLine.line(), _topicLine.note(), uid);
                            if (bookmark == null) {
                                LOG.warn("Failed to recreate bookmark for TopicLine: " + _topicLine.pathForDisplay() + ":" + _topicLine.line());
                            }
                        }
                        // Don't overwrite UUID - it's already set correctly
                    }
                    EditorUtils.addLineCodeRemark(project, _topicLine);
                    // Auto-sync is now triggered from getState(), not here
            }
            
            @Override
            public void lineNoteChanged(Topic topic, TopicLine topicLine) {
                remarkRefreshAlarm.cancelAllRequests();
                remarkRefreshAlarm.addRequest(() -> {
                    if (project.isDisposed()) return;
                    EditorUtils.removeLineCodeRemark(project, topicLine);
                    EditorUtils.addLineCodeRemark(project, topicLine);
                }, 300);
                onDataModified();
            }
            
            @Override
            public void lineUpdated(Topic topic, TopicLine topicLine, int oldLineNum, int newLineNum) {
                onDataModified();
            }
            
            @Override
            public void groupAdded(Topic topic, TopicGroup group) {
                // 统一处理数据修改
                onDataModified();
            }
            
            @Override
            public void groupRemoved(Topic topic, TopicGroup group) {
                // 统一处理数据修改
                onDataModified();
            }
            
            @Override
            public void groupRenamed(Topic topic, TopicGroup group) {
                // 统一处理数据修改
                onDataModified();
            }
            
            @Override
            public void linesReordered(Topic topic) {
                // TopicLine 顺序变化
                onDataModified();
            }
            
            @Override
            public void groupsReordered(Topic topic) {
                // TopicGroup 顺序变化
                onDataModified();
            }
        });
        
        // 订阅TopicList级别的通知
        messageBus.connect().subscribe(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC, new TopicListNotifier() {
            @Override
            public void topicAdded(Topic topic) {
                // 统一处理数据修改
                onDataModified();
            }
            
            @Override
            public void topicRemoved(Topic topic) {
                // 统一处理数据修改
                onDataModified();
            }
            
            @Override
            public void topicsLoaded() {
                // Data loading should not trigger auto-sync
            }
            
            @Override
            public void topicUpdated(Topic topic) {
                // Topic 名称或备注修改
                onDataModified();
            }
            
            @Override
            public void topicsReordered() {
                // Topic 顺序变化
                onDataModified();
            }
        });
    }
    
    /**
     * 调度自动同步
     */
    private void scheduleAutoSync() {
        try {
            AutoSyncScheduler scheduler = AutoSyncScheduler.getInstance(project);
            scheduler.scheduleAutoSync();
        } catch (Exception e) {
            // 忽略错误，避免影响主要功能
        }
    }

    public static CodeReadingNoteService getInstance(@NotNull Project project)
    {
        return project.getService(CodeReadingNoteService.class);
    }

    @Override
    public Element getState()
    {
        Element container = new Element(AppConstants.appName);
        container.addContent(TopicListExporter.export(getTopicList().iterator(), getTopicList().getTrashedLines()));
        Element state = new Element("state");
        state.setAttribute("lastExportDir", lastExportDir());
        state.setAttribute("lastImportDir", lastImportDir());
        container.addContent(state);

        // Trigger auto-sync when state is saved (only if not paused and not calculating)
        triggerAutoSyncOnStateSave();
        
        return container;
    }
    
    /**
     * Get state without triggering auto-sync (for internal use like MD5 calculation)
     */
    @NotNull
    public Element getStateWithoutTrigger() {
        isCalculatingState.set(true);
        try {
            return getState();
        } finally {
            isCalculatingState.set(false);
        }
    }
    
    /**
     * Trigger auto-sync when state is saved to disk
     * This is called by IntelliJ Platform when data actually changes
     */
    private void triggerAutoSyncOnStateSave() {
        try {
            // Don't trigger if we're in the middle of calculating MD5
            if (isCalculatingState.get()) {
                LOG.debug("Skipping auto-sync trigger during state calculation");
                return;
            }
            
            // Check if auto-sync is paused (e.g., due to conflict)
            jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService statusService = 
                jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncStatusService.getInstance(project);
            if (statusService.isAutoSyncPaused()) {
                LOG.debug("Auto-sync is paused, skipping trigger from getState()");
                return;
            }
            
            LOG.debug("State persisted by IDE - transitioning from DIRTY to PENDING");
            // Trigger auto-sync with debounce (will change status from DIRTY to PENDING)
            scheduleAutoSync();
        } catch (Exception e) {
            // Don't let sync errors break state saving
            LOG.debug("Failed to trigger auto-sync from getState()", e);
        }
    }

    @Override
    public void loadState(@NotNull Element element)
    {
        try {
            topicList.setTopics(TopicListImporter.importElement(project, element.getChild("topics")));
        } catch (TopicListImporter.FormatException e) {
            topicList.setTopics(new ArrayList<>());
        }

        topicList.setTrashedLines(TopicListImporter.importTrashedLines(project, element.getChild("topics")));

        Element stateElement = element.getChild("state");
        if (stateElement != null) {
            lastExportDir = stateElement.getAttributeValue("lastExportDir");
            lastImportDir = stateElement.getAttributeValue("lastImportDir");
        } else {
            lastExportDir = "";
            lastImportDir = "";
        }
    }

    public TopicList getTopicList()
    {
        return this.topicList;
    }

    public String lastExportDir() { return lastExportDir != null ? lastExportDir : ""; }
    public void setLastExportDir(String lastExportDir) { this.lastExportDir = lastExportDir; }

    public String lastImportDir() { return lastImportDir != null ? lastImportDir : ""; }
    public void setLastImportDir(String lastImportDir) { this.lastImportDir = lastImportDir; }
    public List<CodeRemark> list(Project project, @NotNull VirtualFile file) {
        Stream<CodeRemark> sorted = topicList.getTopics().stream()
            .flatMap(topic -> topic.getLines().stream())
            .filter(topicLine -> topicLine.file() != null)  // 过滤掉file为null的TopicLine
            .map(topicLine -> {
                CodeRemark codeRemark = new CodeRemark();
                codeRemark.setFileName(topicLine.file().getName());
                codeRemark.setFileUrl(topicLine.file().getCanonicalPath());
                codeRemark.setLineNumber(topicLine.line());
                codeRemark.setProjectName(project.getName());
                codeRemark.setContentHash(CodeRemark.createContentHash(project, topicLine.file()));
                codeRemark.setText(topicLine.note().substring(0, Math.min(topicLine.note().length(), 20)));
                codeRemark.setBookmarkHash(topicLine.bookmarkHash());
                return  codeRemark;
            }).sorted(stateComparator());
        final Predicate<CodeRemark> stateFilter = this.stateFilter(file.getName(), CodeRemark.createContentHash(project,file), null);
        return sorted.filter(stateFilter).sorted(this.stateComparator()).collect(Collectors.toList());
    }

    public List<TopicLine> listSource(Project project, @NotNull VirtualFile file) {
        List<TopicLine> collect = topicList.getTopics().stream()
            .filter(topic -> topic.getLines().stream()
                .anyMatch(topicLine -> topicLine.file() != null && topicLine.file().equals(file)))
            .flatMap(topic -> topic.getLines().stream())
            .filter(topicLine -> topicLine.file() != null)  // 过滤掉file为null的TopicLine
            .collect(Collectors.toList());
        return collect;

    }

    private Predicate<CodeRemark> stateFilter(String fileName, String contentHash, Integer lineNumber) {
        return (codeRemark) -> {
            final boolean fileNameMatch = StringUtils.isEmpty(fileName) || StringUtils.equals(fileName, codeRemark.getFileName());
            final boolean contentHashMatch = StringUtils.isEmpty(contentHash) || StringUtils.equals(contentHash, codeRemark.getContentHash());
            final boolean lineNumberMatch = null == lineNumber || Objects.equals(lineNumber, codeRemark.getLineNumber());
            return fileNameMatch && contentHashMatch && lineNumberMatch;
        };
    }
    private Comparator<CodeRemark> stateComparator() {
        return Comparator.comparing(CodeRemark::getFileName).thenComparing(CodeRemark::getLineNumber);
    }
}
