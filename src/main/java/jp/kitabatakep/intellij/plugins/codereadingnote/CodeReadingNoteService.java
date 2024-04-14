package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.CodeRemark;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jetbrains.annotations.NotNull;

import org.jdom.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    }

    public static CodeReadingNoteService getInstance(@NotNull Project project)
    {
        // project.getService(CodeReadingNoteService.class);
        return ServiceManager.getService(project, CodeReadingNoteService.class);
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
        Stream<CodeRemark> sorted = topicList.getTopics().stream().flatMap(topic -> topic.getLines().stream()).map(topicLine -> {
            CodeRemark codeRemark = new CodeRemark();
            codeRemark.setFileName(topicLine.file().getName());
            codeRemark.setFileUrl(topicLine.file().getCanonicalPath());
            codeRemark.setLineNumber(topicLine.line());
            codeRemark.setProjectName(project.getName());
            codeRemark.setContentHash(CodeRemark.createContentHash(project, topicLine.file()));
            codeRemark.setText(topicLine.note().substring(0, Math.min(topicLine.note().length(), 20)));
            return  codeRemark;
        }).sorted(stateComparator());
        final Predicate<CodeRemark> stateFilter = this.stateFilter(file.getName(), CodeRemark.createContentHash(project,file), null);
        return sorted.filter(stateFilter).sorted(this.stateComparator()).collect(Collectors.toList());
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
