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
import java.util.List;
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
        var language = new jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings();
        language.setSelectedLanguage(jp.kitabatakep.intellij.plugins.codereadingnote.settings.PluginLanguage.ENGLISH);
        MessageBus applicationBus = project(temporary.getRoot().toPath()).getMessageBus();
        var notifications = new com.intellij.notification.NotificationsManager() {
            @Override public void showNotification(com.intellij.notification.Notification notification, Project project) {}
            @Override public void expire(com.intellij.notification.Notification notification) {}
            @Override @SuppressWarnings("unchecked") public <T extends com.intellij.notification.Notification> T[] getNotificationsOfType(Class<T> type, Project project) {
                return (T[]) java.lang.reflect.Array.newInstance(type, 0);
            }
        };
        Application application = (Application) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Application.class}, (p, method, args) -> {
            if (method.getName().equals("invokeLater")) { edt.add((Runnable) args[0]); return null; }
            if (method.getName().equals("invokeAndWait")) { ((Runnable) args[0]).run(); return null; }
            if (method.getName().equals("isDispatchThread")) return true;
            if (method.getName().equals("getService") && args[0] == WorkspaceNotesCoordinator.class) return coordinator;
            if (method.getName().equals("getService") && args[0] == jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings.class) return language;
            if (method.getName().equals("getService") && args[0] == com.intellij.notification.NotificationsManager.class) return notifications;
            if (method.getName().equals("getMessageBus")) return applicationBus;
            if (method.getReturnType() == boolean.class) return false;
            return null;
        });
        ApplicationManager.setApplication(application, applicationLifetime);
    }
    @After public void cleanup() { coordinator.dispose(); Disposer.dispose(applicationLifetime); }
    private Project project(Path root) {
        MessageBus bus = (MessageBus) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{MessageBus.class}, (p, method, args) -> {
            if (method.getName().equals("syncPublisher")) {
                Class<?> type = ((com.intellij.util.messages.Topic<?>) args[0]).getListenerClass();
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
            default -> method.getReturnType() == boolean.class ? false : null;
        });
    }
    private void drainEdt() { Runnable task; while ((task = edt.poll()) != null) task.run(); }
    private void reload(Project project, Path root) throws Exception {
        java.util.concurrent.CountDownLatch complete = new java.util.concurrent.CountDownLatch(1);
        coordinator.refreshAll(project, List.of(root), true, complete::countDown);
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (complete.getCount() != 0 && System.nanoTime() < deadline) {
            drainEdt();
            Thread.yield();
        }
        assertEquals("Reload completion must follow model application", 0, complete.getCount());
        drainEdt();
    }
    private String xml(String name) {
        return "<project><component name='CodeReadingNote'><topics><topic><name>" + name
                + "</name><updatedAt>2026-10-29 00:00:00</updatedAt></topic></topics></component></project>";
    }
    @Test public void refreshReloadsExternalChildChangesAndClearingWithoutReopening() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("child");
        Path notes = Files.createDirectories(child.resolve(".idea")).resolve("CodeReadingNote.xml");
        Files.writeString(notes, xml("before"));
        Project window = project(root);
        TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        assertEquals("before", list.getTopics().get(0).name());
        long revision = coordinator.syncRevision(list);
        Files.writeString(notes, xml("after"));
        reload(window, child);
        assertEquals("after", list.getTopics().get(0).name());
        assertTrue(coordinator.syncRevision(list) > revision);
        Files.writeString(notes, "<CodeReadingNote><topics/></CodeReadingNote>");
        reload(window, child);
        assertTrue(list.getTopics().isEmpty());
        assertFalse(coordinator.retainForRefresh(list));
    }
    @Test public void explicitRefreshReloadsStandaloneRootData() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("root-notes");
        Path notes = Files.createDirectories(child.resolve(".idea")).resolve("CodeReadingNote.xml");
        Files.writeString(notes, xml("before"));
        Project window = project(root), standalone = project(child);
        TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        coordinator.attachRoot(standalone);
        Files.writeString(notes, xml("after"));
        // A background discovery leaves standalone ownership to the platform.
        coordinator.refresh(window, child); drainEdt();
        assertEquals("before", list.getTopics().get(0).name());
        reload(window, child);
        assertEquals("after", list.getTopics().get(0).name());
        Disposer.dispose(standalone);
    }
    @Test public void rootRefreshPreservesPendingEditsAndRecognizesSavedModel() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("pending-root");
        Path notes = Files.createDirectories(child.resolve(".idea")).resolve("CodeReadingNote.xml");
        Files.writeString(notes, xml("before"));
        Project window = project(root), standalone = project(child);
        TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        list.addTopic("local");
        coordinator.attachRoot(standalone);
        reload(window, child);
        assertEquals(2, list.getTopics().size());
        assertTrue(coordinator.retainForRefresh(list));
        WorkspaceXmlStore disk = new WorkspaceXmlStore(notes); disk.load(); disk.save(TopicListExporter.export(list), false);
        reload(window, child);
        assertFalse(coordinator.retainForRefresh(list));
        Files.writeString(notes, xml("external"));
        reload(window, child);
        assertEquals("external", list.getTopics().get(0).name());
        Disposer.dispose(standalone);
    }
    @Test public void refreshKeepsUnsavedChildDataWhenDiskIsUnchanged() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("pending-child");
        Files.createDirectories(child.resolve(".idea"));
        Project window = project(root); TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        list.addTopic("pending");
        assertTrue(coordinator.retainForRefresh(list));
        reload(window, child);
        assertEquals("pending", list.getTopics().get(0).name());
    }
    @Test public void conflictingRootRefreshPreservesLocalModelAndExternalDisk() throws Exception {
        Path root = temporary.getRoot().toPath(), child = root.resolve("conflicting-root");
        Path notes = Files.createDirectories(child.resolve(".idea")).resolve("CodeReadingNote.xml");
        Files.writeString(notes, xml("before"));
        Project window = project(root), standalone = project(child);
        TopicList list = coordinator.acquire(window, child);
        coordinator.refresh(window, child); drainEdt();
        list.addTopic("local");
        coordinator.attachRoot(standalone);
        String external = xml("external");
        Files.writeString(notes, external);
        reload(window, child);
        assertEquals(2, list.getTopics().size());
        assertEquals("local", list.getTopics().get(1).name());
        assertEquals(external, Files.readString(notes));
        assertTrue(coordinator.persistencePaused(list));
        assertTrue(coordinator.retainForRefresh(list));
        Disposer.dispose(standalone);
    }
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
