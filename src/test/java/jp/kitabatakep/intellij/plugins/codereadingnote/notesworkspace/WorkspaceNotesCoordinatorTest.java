package jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBus;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.Queue;
import static org.junit.Assert.*;

/** Model/queue coordination without launching an IDE; actual platform close ordering needs installed-plugin acceptance. */
public class WorkspaceNotesCoordinatorTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private WorkspaceNotesCoordinator coordinator;
    private Disposable applicationLifetime;
    private final Queue<Runnable> edt = new java.util.concurrent.ConcurrentLinkedQueue<>();
    @Before public void setup() {
        applicationLifetime = Disposer.newDisposable();
        coordinator = new WorkspaceNotesCoordinator();
        Application application = (Application) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Application.class}, (p, method, args) -> {
            if (method.getName().equals("invokeLater")) { edt.add((Runnable) args[0]); return null; }
            if (method.getName().equals("invokeAndWait")) { ((Runnable) args[0]).run(); return null; }
            if (method.getName().equals("isDispatchThread")) return true;
            if (method.getName().equals("getService") && args[0] == WorkspaceNotesCoordinator.class) return coordinator;
            if (method.getReturnType() == boolean.class) return false;
            return null;
        });
        ApplicationManager.setApplication(application, applicationLifetime);
    }
    @After public void cleanup() { coordinator.dispose(); Disposer.dispose(applicationLifetime); }
    private Project project(Path root) {
        MessageBus bus = (MessageBus) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{MessageBus.class}, (p, method, args) -> {
            if (method.getName().equals("syncPublisher")) {
                Class<?> type = args[0] == TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC ? TopicListNotifier.class
                        : args[0] == TopicNotifier.TOPIC_NOTIFIER_TOPIC ? TopicNotifier.class : WorkspaceNotesNotifier.class;
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{type}, (p2, method2, args2) -> null);
            }
            return null;
        });
        return (Project) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Project.class}, (p, method, args) -> switch (method.getName()) {
            case "getBasePath" -> root.toString();
            case "getMessageBus" -> bus;
            case "isDisposed" -> false;
            case "hashCode" -> System.identityHashCode(p);
            case "equals" -> p == args[0];
            case "toString" -> root.toString();
            default -> null;
        });
    }
    private void drainEdt() { Runnable task; while ((task = edt.poll()) != null) task.run(); }
    @Test public void multipleWindowsAndIndependentProjectShareOneList() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("child");
        Files.createDirectories(child.resolve(".idea"));
        Project window = project(root), secondWindow = project(root.resolve("another")), standalone = project(child);
        TopicList first = coordinator.acquire(window, child);
        assertSame(first, coordinator.acquire(secondWindow, child.resolve("../child")));
        coordinator.refresh(window, child); drainEdt();
        assertSame(first, coordinator.attachRoot(standalone));
        assertTrue(coordinator.wasLoaded(first));
        coordinator.release(window);
        assertSame(first, coordinator.acquire(secondWindow, child));
        Disposer.dispose(standalone);
        coordinator.release(secondWindow);
    }
    @Test public void closeCapturesEditBeforeItsDebounceAndKeepsRootFileSeparate() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("child");
        Files.createDirectories(child.resolve(".idea")); Files.createDirectories(root.resolve(".idea"));
        Path rootFile = root.resolve(".idea/CodeReadingNote.xml"); Files.writeString(rootFile, "root sentinel");
        Project window = project(root);
        TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        list.addTopic("close-before-debounce"); // do not run the queued snapshot task
        coordinator.release(window);
        assertEquals("close-before-debounce", new WorkspaceXmlStore(child.resolve(".idea/CodeReadingNote.xml")).load().getChild("topic").getChildText("name"));
        assertEquals("root sentinel", Files.readString(rootFile));
    }
    @Test public void loadApplicationDoesNotScheduleAWrite() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("empty"); Files.createDirectories(child.resolve(".idea"));
        Project window = project(root); TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        assertTrue(coordinator.wasLoaded(list));
        coordinator.release(window);
        assertFalse(Files.exists(child.resolve(".idea/CodeReadingNote.xml")));
    }
    private TopicList childList(String name) throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve(name);
        Files.createDirectories(child.resolve(".idea"));
        Project window = project(root); TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt(); return list;
    }
    @Test public void syncFlushPersistsOnlyOwningProjectAndIncludesEmptyTrash() throws Exception {
        TopicList a = childList("a"), b = childList("b");
        a.addTopic("same"); b.addTopic("same");
        coordinator.flushForSync(a);
        assertTrue(Files.exists(a.context().storage())); assertFalse(Files.exists(b.context().storage()));
        org.jdom.Element xml = new WorkspaceXmlStore(a.context().storage()).load();
        assertNotNull(xml.getChild("trash")); assertEquals("same", xml.getChild("topic").getChildText("name"));
    }
    @Test public void editWhileDownloadingPreventsRemoteApply() throws Exception {
        TopicList list = childList("edit");
        var before = coordinator.syncSnapshot(list);
        list.addTopic("new local edit");
        try { coordinator.applySync(list, new org.jdom.Element("topics"), before); fail(); }
        catch (java.io.IOException expected) { }
        assertEquals("new local edit", list.getTopics().get(0).name());
    }
    @Test public void diskChangeWhileDownloadingPreventsRemoteApply() throws Exception {
        TopicList list = childList("disk"); var before = coordinator.syncSnapshot(list);
        String external = "<project><component name='CodeReadingNote'><topics><external/></topics></component></project>";
        Files.writeString(list.context().storage(), external);
        try { coordinator.applySync(list, new org.jdom.Element("topics"), before); fail(); }
        catch (java.io.IOException expected) { }
        assertEquals(external, Files.readString(list.context().storage())); assertTrue(list.getTopics().isEmpty());
    }
    @Test public void malformedRemoteLeavesModelAndDiskUntouched() throws Exception {
        TopicList list = childList("malformed"); list.addTopic("keep");
        var before = coordinator.flushForSync(list); byte[] disk = Files.readAllBytes(list.context().storage());
        org.jdom.Element invalid = new org.jdom.Element("topics").addContent(new org.jdom.Element("topic"));
        try { coordinator.applySync(list, invalid, before); fail(); } catch (TopicListImporter.FormatException expected) { }
        assertEquals("keep", list.getTopics().get(0).name()); assertArrayEquals(disk, Files.readAllBytes(list.context().storage()));
    }
    @Test public void remoteEmptyCollectionAppliesAndUnknownFieldsSurviveSave() throws Exception {
        TopicList list = childList("empty"); list.addTopic("removed remotely");
        list.setXmlTemplate(new org.jdom.Element("topics").setAttribute("localFuture", "keep").addContent(new org.jdom.Element("localExtension")));
        var before = coordinator.flushForSync(list);
        org.jdom.Element remote = new org.jdom.Element("topics").setAttribute("future", "yes")
                .addContent(new org.jdom.Element("extension").setText("keep")).addContent(new org.jdom.Element("trash"));
        coordinator.applySync(list, remote, before);
        assertTrue(list.getTopics().isEmpty());
        var saved = new WorkspaceXmlStore(list.context().storage()).load();
        assertEquals("yes", saved.getAttributeValue("future")); assertEquals("keep", saved.getChildText("extension"));
        assertEquals("keep", saved.getAttributeValue("localFuture")); assertNotNull(saved.getChild("localExtension"));
    }
}
