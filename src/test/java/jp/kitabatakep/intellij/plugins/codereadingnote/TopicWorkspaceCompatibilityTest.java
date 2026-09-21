package jp.kitabatakep.intellij.plugins.codereadingnote;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.NoteProjectContext;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class TopicWorkspaceCompatibilityTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private com.intellij.openapi.Disposable applicationLifetime;
    @Before public void installLanguageService() {
        applicationLifetime = com.intellij.openapi.util.Disposer.newDisposable();
        var language = new jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings();
        language.setSelectedLanguage(jp.kitabatakep.intellij.plugins.codereadingnote.settings.PluginLanguage.ENGLISH);
        var application = (com.intellij.openapi.application.Application) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{com.intellij.openapi.application.Application.class}, (p, method, args) -> {
                    if (method.getName().equals("getService") && args[0] == language.getClass()) return language;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
        com.intellij.openapi.application.ApplicationManager.setApplication(application, applicationLifetime);
    }
    @After public void removeApplication() { com.intellij.openapi.util.Disposer.dispose(applicationLifetime); }
    private Project project() {
        MessageBus bus = (MessageBus) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{MessageBus.class}, (p, method, args) -> {
            if (method.getName().equals("syncPublisher")) {
                Class<?> listener = args[0] == TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC ? TopicListNotifier.class : TopicNotifier.class;
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{listener}, (p2, method2, args2) -> null);
            }
            return null;
        });
        return (Project) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Project.class}, (p, method, args) -> switch (method.getName()) {
            case "getBasePath" -> temporary.getRoot().getAbsolutePath();
            case "getMessageBus" -> bus;
            case "isDisposed" -> false;
            default -> null;
        });
    }
    private Element xml(String text) throws Exception { return new SAXBuilder().build(new StringReader(text)).getRootElement(); }
    @Test public void duplicateTopicNamesRemainIndependentAndKeepLegacyMetadata() throws Exception {
        Project project = project();
        NoteProjectContext a = new NoteProjectContext(temporary.getRoot().toPath().resolve("a"));
        NoteProjectContext b = new NoteProjectContext(temporary.getRoot().toPath().resolve("b"));
        Element original = xml("<topics><topic custom='keep'><name>Same</name><note>old</note><updatedAt>2020-01-02 03:04:05</updatedAt><future>keep</future><topicLines/></topic></topics>");
        int[] changes = {0}; a.onChange(() -> changes[0]++);
        ArrayList<Topic> first = TopicListImporter.importElement(project, a, original);
        ArrayList<Topic> second = TopicListImporter.importElement(project, b, original);
        assertNotEquals(first.get(0).context(), second.get(0).context());
        assertEquals(0, changes[0]);
        Element unchanged = TopicListExporter.export(first.iterator());
        assertEquals("2020-01-02 03:04:05", unchanged.getChild("topic").getChildText("updatedAt"));
        first.get(0).setNote("edit a");
        Element saved = TopicListExporter.export(first.iterator());
        assertEquals("edit a", saved.getChild("topic").getChildText("note"));
        assertEquals("keep", saved.getChild("topic").getChildText("future"));
        assertEquals("keep", saved.getChild("topic").getAttributeValue("custom"));
        assertEquals("old", second.get(0).note());
        assertEquals(1, changes[0]);
    }
    @Test public void groupTimestampsOrderingAndExtensionContainersSurviveRoundTrip() throws Exception {
        Project project = project();
        NoteProjectContext context = new NoteProjectContext(temporary.getRoot().toPath());
        Element original = xml("<topics><topic><name>T</name><updatedAt>2020-01-02 03:04:05</updatedAt><hasGroups>true</hasGroups>"
                + "<groups extension='keep'><future/><group><name>G</name><createdAt>2020-01-01 00:00:00</createdAt>"
                + "<updatedAt>2020-01-02 00:00:00</updatedAt><expanded>true</expanded><topicLines custom='keep'/></group></groups></topic></topics>");
        ArrayList<Topic> topics = TopicListImporter.importElement(project, context, original);
        Element saved = TopicListExporter.export(topics.iterator());
        Element groups = saved.getChild("topic").getChild("groups");
        assertEquals("keep", groups.getAttributeValue("extension"));
        assertNotNull(groups.getChild("future"));
        assertEquals("2020-01-02 00:00:00", groups.getChild("group").getChildText("updatedAt"));
        assertEquals("keep", groups.getChild("group").getChild("topicLines").getAttributeValue("custom"));
    }
    @Test public void malformedLineRejectsTheImportRatherThanDroppingData() throws Exception {
        Element bad = xml("<topics><topic><name>T</name><updatedAt>2020-01-01 00:00:00</updatedAt><topicLines><topicLine>"
                + "<line>invalid</line><inProject>true</inProject><url>file:///x</url></topicLine></topicLines></topic></topics>");
        try { TopicListImporter.importElement(project(), bad); fail("Malformed note silently discarded"); }
        catch (TopicListImporter.FormatException expected) { }
    }
    @Test public void unresolvedNotesMoveDeleteAndRestoreWithinTheirOwner() {
        Project project = project();
        NoteProjectContext a = new NoteProjectContext(temporary.getRoot().toPath().resolve("a"));
        NoteProjectContext b = new NoteProjectContext(temporary.getRoot().toPath().resolve("b"));
        TopicList first = new TopicList(project, a), second = new TopicList(project, b);
        first.addTopic("Same"); second.addTopic("Same");
        Topic source = first.getTopics().get(0), other = second.getTopics().get(0);
        TopicLine line = new TopicLine(project, source, null, 4, "note", true, "src/Main.java", "file:///old/src/Main.java", "duplicate-uid");
        TopicLine foreign = new TopicLine(project, other, null, 4, "other", true, "src/Main.java", "file:///old/src/Main.java", "duplicate-uid");
        source.addLine(line); other.addLine(foreign);
        assertNotEquals(line.runtimeId(), foreign.runtimeId());
        assertEquals("src/Main.java", TopicListExporter.export(first.iterator()).getChild("topic").getChild("topicLines").getChild("topicLine").getChildText("relativePath"));
        first.addTopic("Target"); Topic target = first.getTopics().get(1);
        target.moveLineHere(line, target.addGroup("G"));
        assertTrue(source.getLines().isEmpty());
        assertSame(target, line.topic());
        assertTrue(first.getTrashedLines().isEmpty());
        try { other.moveLineHere(line, null); fail("Cross-project move allowed"); }
        catch (IllegalArgumentException expected) { }
        assertSame(target, line.topic());
        target.removeLine(line);
        assertEquals(1, first.getTrashedLines().size());
        assertTrue(second.getTrashedLines().isEmpty());
        first.removeTopic(target);
        first.restoreFromTrash(first.getTrashedLines().get(0));
        assertTrue(first.getTrashedLines().isEmpty());
        assertEquals("Target", line.topic().name());
        assertEquals(a, line.topic().context());
        assertEquals("duplicate-uid", line.getBookmarkUid());
        assertEquals("src/Main.java", line.relativePath());
    }
}
