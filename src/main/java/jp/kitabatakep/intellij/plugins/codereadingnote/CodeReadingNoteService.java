package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.*;
import org.jetbrains.annotations.NotNull;

import org.jdom.Element;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.CodeRemark;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@State(
    name = AppConstants.appName,
    storages = {
        @Storage(AppConstants.appName + ".xml"),
    }
)
public class CodeReadingNoteService implements PersistentStateComponent<Element>
{
    Project project;
    TopicList topicList;

    String lastExportDir = "";
    String lastImportDir = "";

    public CodeReadingNoteService(@NotNull Project project)
    {
        this.project = project;
        topicList = new TopicList(project);
        starConfit(project);
//        new MyBookmarkListener(project);
        addMyListener(project);

    }

    private void starConfit(Project project) {

    }

    private void addMyListener(Project project) {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
            @Override
            public void lineRemoved(Topic _topic, TopicLine _topicLine) {
                    EditorUtils.removeLineCodeRemark(project,_topicLine);
            }

            @Override
            public void lineAdded(Topic _topic, TopicLine _topicLine) {
                // 单一来源：这里负责创建并绑定 Bookmark UUID
                if (_topicLine.file() != null) {
                    String uid = UUID.randomUUID().toString();
                    Bookmark bookmark = BookmarkUtils.addBookmark(project, _topicLine.file(), _topicLine.line(), _topicLine.note(), uid);
                    if (bookmark != null) {
                        _topicLine.setBookmarkUid(uid);
                    }
                }
                EditorUtils.addLineCodeRemark(project, _topicLine);
            }
        });
    }

    public static CodeReadingNoteService getInstance(@NotNull Project project)
    {
        return project.getService(CodeReadingNoteService.class);
    }

    @Override
    public Element getState()
    {
        Element container = new Element(AppConstants.appName);
        container.addContent(TopicListExporter.export(getTopicList().iterator()));
        Element state = new Element("state");
        state.setAttribute("lastExportDir", lastExportDir());
        state.setAttribute("lastImportDir", lastImportDir());
        container.addContent(state);
        return container;
    }

    @Override
    public void loadState(@NotNull Element element)
    {
        try {
            topicList.setTopics(TopicListImporter.importElement(project, element.getChild("topics")));
        } catch (TopicListImporter.FormatException e) {
            topicList.setTopics(new ArrayList<>());
        }

        Element stateElement = element.getChild("state");
        lastExportDir = stateElement.getAttributeValue("lastExportDir");
        lastImportDir = stateElement.getAttributeValue("lastImportDir");
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
