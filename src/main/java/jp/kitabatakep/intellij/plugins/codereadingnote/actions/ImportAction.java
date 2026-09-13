package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicListImporter;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicListNotifier;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class ImportAction extends CommonAnAction
{
    public ImportAction() {
        super(
            jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("action.import"),
            jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("action.import.description"),
            AllIcons.ToolbarDecorator.Import
        );
    }

    private java.util.function.Supplier<jp.kitabatakep.intellij.plugins.codereadingnote.TopicList> selection;
    public ImportAction(java.util.function.Supplier<jp.kitabatakep.intellij.plugins.codereadingnote.TopicList> selection) { this(); this.selection = selection; }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        jp.kitabatakep.intellij.plugins.codereadingnote.TopicList target = jp.kitabatakep.intellij.plugins.codereadingnote.ui.WorkspaceProjectChooser.choose(e.getProject(), selection);
        if (target == null || !jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceNotesCoordinator.getInstance().canEdit(target)) return;

        Project project = e.getProject();
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(project);

        VirtualFile baseDir;
        if (!service.lastImportDir().equals("")) {
            baseDir = LocalFileSystem.getInstance().findFileByPath(service.lastImportDir());
        } else {
            baseDir = LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"));
        }

        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml");
        VirtualFile[] files = FileChooserFactory.getInstance().
            createFileChooser(fileChooserDescriptor, project, null).
            choose(project, baseDir);

        if (files.length == 0) {
            return;
        }

        VirtualFile parentDir = files[0].getParent();
        if (parentDir != null && parentDir.exists()) {
            service.setLastImportDir(parentDir.getPath());
        }

//        SAXBuilder builder = new SAXBuilder();
        Document document = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder2 = factory.newDocumentBuilder();
            org.w3c.dom.Document document1 = builder2.parse(new File(files[0].getPath()));
            DOMBuilder domBuilder = new DOMBuilder();
            document = domBuilder.build(document1);
//             document = builder.build(new File(files[0].getPath()));
        } catch (ParserConfigurationException | SAXException ex) {
            Messages.showErrorDialog(
                project,
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.import.failed.format"),
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.import.failed.title", AppConstants.appName)
            );
        } catch (IOException ex) {
            Messages.showErrorDialog(
                project,
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.import.failed"),
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.import.failed.title", AppConstants.appName)
            );
            return;
        }

        if (document == null) {
            return;
        }

        try {
            var topics = TopicListImporter.importElement(project, target.context(), document.getRootElement());
            var trash = TopicListImporter.importTrashedLines(project, target.context(), document.getRootElement());
            target.setTopics(topics);
            target.setTrashedLines(trash);
            target.context().changed();
            MessageBus messageBus = project.getMessageBus();
            TopicListNotifier publisher = messageBus.syncPublisher(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC);
            publisher.topicsLoaded(target);
        } catch (TopicListImporter.FormatException | IllegalArgumentException e2) {
            Messages.showErrorDialog(
                project,
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.import.failed.format"),
                jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle.message("message.import.failed.title", AppConstants.appName)
            );
        }


    }
}
