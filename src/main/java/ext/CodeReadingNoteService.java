package ext;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.BranchChangeListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@State(
//    name = "CodeReadingNoteService",
//    storages = {
//        @Storage("CodeReadingNoteService.xml")
//    }
//)
public class CodeReadingNoteService implements PersistentStateComponent<Element> {

    private static final Logger LOG = Logger.getInstance(CodeReadingNoteService.class);

    private final Project project;
    private final Map<String, RangeMarker> rangeMarkerMap = new HashMap<>();

    public CodeReadingNoteService(Project project) {
        this.project = project;
        addFileSystemListener(); //监控有效，但是startOffset没什么区别，定位还是原来的行
        addVcsListener();
    }

    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("CodeReadingNoteService");
        // 序列化 rangeMarkerMap 到 XML
        for (Map.Entry<String, RangeMarker> entry : rangeMarkerMap.entrySet()) {
            Element markerElement = new Element("RangeMarker");
            markerElement.setAttribute("filePath", entry.getKey());
            markerElement.setAttribute("startOffset", String.valueOf(entry.getValue().getStartOffset()));
            markerElement.setAttribute("endOffset", String.valueOf(entry.getValue().getEndOffset()));
            element.addContent(markerElement);
        }
        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        rangeMarkerMap.clear();
        for (Element markerElement : state.getChildren("RangeMarker")) {
            String filePath = markerElement.getAttributeValue("filePath");
            int startOffset = Integer.parseInt(markerElement.getAttributeValue("startOffset"));
            int endOffset = Integer.parseInt(markerElement.getAttributeValue("endOffset"));
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(filePath);
            if (file != null) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    RangeMarker rangeMarker = document.createRangeMarker(startOffset, endOffset);
                    rangeMarkerMap.put(filePath, rangeMarker);
                }
            }
        }
    }

    private void addFileSystemListener() {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event.isFromSave()) {
                        // 确认事件类型为 "内容更改" 并且不是新建或删除事件
                        if (event.getFile() != null) {
                            VirtualFile file = event.getFile();
                            // 调用原有的 updateRangeMarker 方法
                            updateRangeMarker(file);
                        }
                    }
                }
            }
        });
    }

    private void addVcsListener() {
        MessageBus messageBus = project.getMessageBus();
        MessageBusConnection connection = messageBus.connect();
        connection.subscribe(BranchChangeListener.VCS_BRANCH_CHANGED, new BranchChangeListener() {
            @Override
            public void branchWillChange(@NotNull String branchName) {
                // 在分支切换前进行处理
            }

            @Override
            public void branchHasChanged(@NotNull String branchName) {
                // 在分支切换后进行处理
                updateAllRangeMarkers();
            }
        });
    }

    private void updateRangeMarker(VirtualFile file) {
        String filePath = file.getPath();
        RangeMarker rangeMarker = rangeMarkerMap.get(filePath);
        if (rangeMarker != null) {
            // 重新计算 RangeMarker 的位置并更新
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                int line = document.getLineNumber(rangeMarker.getStartOffset());
                LOG.info("RangeMarker is now at line: " + line);
                // 可以在此处更新显示逻辑
            }
        }
    }

    private void updateAllRangeMarkers() {
        for (String filePath : rangeMarkerMap.keySet()) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(filePath);
            if (file != null) {
                updateRangeMarker(file);
            }
        }
    }

    public void addRangeMarker(Node node) {
        OpenFileDescriptor descriptor = node.getOpenFileDescriptor();
        RangeMarker rangeMarker = descriptor.getRangeMarker();

        if (rangeMarker != null) {
            VirtualFile virtualFile = descriptor.getFile();
            rangeMarkerMap.put(virtualFile.getPath(), rangeMarker);
        }
    }
}
