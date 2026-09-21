package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.*;
import org.jdom.Element;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Single application queue: no duplicate writers across parent/child windows or shared remote bindings. */
@Service(Service.Level.APP)
public final class WorkspaceNotesSyncCoordinator implements Disposable {
    public enum Operation { CHECK, PUSH, PULL, AUTO, USE_LOCAL, USE_REMOTE }
    public record Review(NotesSyncBinding binding, WorkspaceNotesCoordinator.SyncSnapshot local, NotesRemote.Snapshot remote) {}
    public record Result(String status, Review review) { public boolean success() { return Set.of("synced", "pending", "remote", "manual").contains(status); } }
    public record View(NotesSyncBinding binding, String status, long checked, long synced) {}
    private static final class Session {
        NotesSyncBinding binding;
        NotesSyncState state;
        NotesRemote.Snapshot cached;
        NotesSyncSchedule schedule = new NotesSyncSchedule();
    }
    private final Set<Project> windows = ConcurrentHashMap.newKeySet();
    private final Map<Path, Session> sessions = new HashMap<>(); // worker confined
    private final Map<Path, View> views = new ConcurrentHashMap<>();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "workspace-notes-sync"); t.setDaemon(true); return t;
    });
    private volatile boolean disposed;
    private volatile long activated;
    private long networkRetryAt;
    private final Map<Path, java.util.concurrent.atomic.AtomicLong> generations = new ConcurrentHashMap<>();
    private long generation(TopicList list) { return generations.computeIfAbsent(list.context().root(), p -> new java.util.concurrent.atomic.AtomicLong()).get(); }
    private final Path stateRoot = Path.of(PathManager.getConfigPath(), "CodeReadingNote", "notes-sync");
    public WorkspaceNotesSyncCoordinator() {
        worker.scheduleWithFixedDelay(this::tick, 1, 1, TimeUnit.SECONDS);
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(ApplicationActivationListener.TOPIC, new ApplicationActivationListener() {
            @Override public void applicationActivated(com.intellij.openapi.wm.IdeFrame frame) { activated = System.currentTimeMillis(); }
        });
    }
    public static WorkspaceNotesSyncCoordinator getInstance() { return ApplicationManager.getApplication().getService(WorkspaceNotesSyncCoordinator.class); }
    public void attach(Project project) {
        if (windows.add(project)) Disposer.register(project, () -> windows.remove(project));
    }
    public View view(TopicList list) { return views.getOrDefault(list.context().root(), new View(null, "unbound", 0, 0)); }
    public static String text(String status) { return CodeReadingNoteBundle.message("notes.sync.status." + status); }
    private Path statePath(TopicList list, NotesSyncBinding binding) {
        return stateRoot.resolve(NotesSyncXml.hash(list.context().id() + "\n" + binding.identity()) + ".xml");
    }
    private Session session(TopicList list) throws IOException {
        Path root = list.context().root();
        NotesSyncBinding binding = NotesSyncBinding.read(root);
        Session s = sessions.computeIfAbsent(root, p -> new Session());
        if (s.state == null || !Objects.equals(s.binding, binding)) {
            s.binding = binding; s.cached = null; s.schedule = new NotesSyncSchedule();
            s.state = binding == null ? new NotesSyncState() : NotesSyncState.read(statePath(list, binding));
        }
        return s;
    }
    private Project window(TopicList list) {
        for (Project p : windows) if (!p.isDisposed() && WorkspaceNotesService.getInstance(p).projects().contains(list)) return p;
        return null;
    }
    private GitHubSyncConfig config() throws NotesRemote.Failure {
        SyncConfig raw = SyncSettings.getInstance().getSyncConfig();
        if (!raw.isEnabled()) throw new NotesRemote.Failure("disabled", 0);
        if (!(raw instanceof GitHubSyncConfig gh) || gh.validate() != null) throw new NotesRemote.Failure("config", 0);
        return gh.clone();
    }
    private void valid(TopicList list, NotesSyncBinding binding, long generation) throws Exception {
        if (disposed || Thread.currentThread().isInterrupted() || window(list) == null || !Files.isDirectory(list.context().root().resolve(".idea")))
            throw new NotesRemote.Failure("unavailable", 0);
        if (generation != generation(list) || !binding.equals(NotesSyncBinding.read(list.context().root())) || !binding.matches(config())) throw new NotesRemote.Failure("binding", 0);
    }
    private void publish(TopicList list, Session s, String status) {
        s.state.status = status;
        views.put(list.context().root(), new View(s.binding, status, s.state.checked, s.state.synced));
        ApplicationManager.getApplication().invokeLater(() -> {
            for (Project p : windows) if (!p.isDisposed()) p.getMessageBus().syncPublisher(NotesSyncNotifier.TOPIC).changed(list.context());
        }, ModalityState.any());
    }
    private Result finish(TopicList list, Session s, String status, Review review) throws IOException {
        publish(list, s, status);
        if (s.binding != null) s.state.save(statePath(list, s.binding));
        return new Result(status, review);
    }
    public CompletableFuture<Result> submit(Project project, TopicList list, Operation op, Review review) {
        attach(project);
        long requestedGeneration = generation(list);
        return CompletableFuture.supplyAsync(() -> requestedGeneration == generation(list) ? execute(list, op, review) : new Result("stale", null), worker);
    }
    public CompletableFuture<List<String>> remoteProjects() {
        return CompletableFuture.supplyAsync(() -> {
            try { return new GitHubNotesRemote(config()).projects(); }
            catch (Exception error) { throw new CompletionException(error); }
        }, worker);
    }
    public CompletableFuture<View> inspect(Project project, TopicList list) {
        attach(project);
        return CompletableFuture.supplyAsync(() -> {
            try { Session s = session(list); publish(list, s, s.binding == null ? "unbound" : s.state.status); return view(list); }
            catch (IOException error) { return new View(null, "storage", 0, 0); }
        }, worker);
    }
    public CompletableFuture<Result> bind(Project project, TopicList list, NotesSyncBinding binding, boolean allowShared) {
        attach(project);
        generations.computeIfAbsent(list.context().root(), p -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!binding.matches(config()) || window(list) == null) return new Result("binding", null);
                if (!allowShared) for (TopicList other : activeLists()) {
                    if (other == list) continue;
                    NotesSyncBinding existing = NotesSyncBinding.read(other.context().root());
                    if (existing != null && existing.identity().equals(binding.identity())) return new Result("duplicate", null);
                }
                binding.save(list.context().root());
                Session s = session(list); s.state.paused = false; s.state.retryAt = 0; s.schedule = new NotesSyncSchedule();
                return finish(list, s, s.state.baseline.isEmpty() ? "first" : "pending", null);
            } catch (Exception error) { return new Result(error instanceof NotesRemote.Failure f ? f.key : "storage", null); }
        }, worker);
    }
    private Result execute(TopicList list, Operation operation, Review approval) {
        Session s = null;
        long generation = generation(list);
        try {
            s = session(list);
            if (s.binding == null) return finish(list, s, "unbound", null);
            NotesSyncBinding binding = s.binding;
            valid(list, binding, generation);
            if (System.currentTimeMillis() < networkRetryAt) throw new NotesRemote.Failure("rate", networkRetryAt);
            if (operation == Operation.AUTO && s.state.paused) return new Result(s.state.status, null);
            boolean repairCache = "cache".equals(s.state.status);
            publish(list, s, "syncing");
            WorkspaceNotesCoordinator data = WorkspaceNotesCoordinator.getInstance();
            WorkspaceNotesCoordinator.SyncSnapshot local = data.syncSnapshot(list);
            GitHubNotesRemote remote = new GitHubNotesRemote(config());
            NotesRemote.Snapshot disk = remote.read(binding.remoteId(), s.cached);
            s.cached = disk; s.state.checked = System.currentTimeMillis();
            valid(list, binding, generation);
            String l = NotesSyncXml.digest(local.topics());
            Element remoteXml = disk.exists() ? NotesSyncXml.topics(disk.xml()) : null;
            String r = remoteXml == null ? null : NotesSyncXml.digest(remoteXml);
            // Recover an interrupted local apply only if both disk/model and remote confirm the journal.
            if (!s.state.pendingHash.isEmpty() && s.state.pendingHash.equals(l) && Objects.equals(l, r)) {
                s.state.baseline = r; s.state.pendingHash = ""; s.state.pendingSha = "";
            }
            NotesSyncDecision decision = NotesSyncDecision.compare(s.state.baseline, l, r);
            Review review = new Review(binding, local, disk);
            if (operation == Operation.USE_LOCAL || operation == Operation.USE_REMOTE) {
                if (approval == null || !binding.equals(approval.binding()) || !Objects.equals(disk.sha(), approval.remote().sha())
                        || !NotesSyncXml.digest(approval.local().topics()).equals(l)) return finish(list, s, "stale", review);
                backup(list, binding, "local", NotesSyncXml.write(local.topics()));
                if (disk.exists()) backup(list, binding, "remote", disk.xml());
            }
            if (decision == NotesSyncDecision.SAME) {
                if (repairCache) remote.repairCache(binding.remoteId(), disk);
                local = data.flushForSync(list);
                return acknowledge(list, s, l, disk.sha(), data, local);
            }
            boolean forcedPush = operation == Operation.USE_LOCAL;
            boolean forcedPull = operation == Operation.USE_REMOTE;
            boolean initialPush = operation == Operation.PUSH && !disk.exists() && s.state.baseline.isEmpty();
            boolean emptyLocal = local.topics().getChildren("topic").isEmpty() && local.topics().getChild("trash").getChildren().isEmpty();
            boolean initialPull = operation == Operation.PULL && s.state.baseline.isEmpty() && emptyLocal && disk.exists();
            boolean push = forcedPush || initialPush || decision == NotesSyncDecision.LOCAL_CHANGED && operation == Operation.PUSH
                    || operation == Operation.AUTO && decision.automatic(binding.policy()) == NotesSyncDecision.Direction.PUSH;
            boolean pull = forcedPull || initialPull || decision == NotesSyncDecision.REMOTE_CHANGED
                    && operation == Operation.PULL || operation == Operation.AUTO && decision.automatic(binding.policy()) == NotesSyncDecision.Direction.PULL;
            if (operation == Operation.CHECK) { push = false; pull = false; }
            if (push) {
                valid(list, binding, generation);
                if (!data.syncCurrent(list, local)) return finish(list, s, "stale", null);
                WorkspaceNotesCoordinator.SyncSnapshot saved = data.flushForSync(list);
                if (!NotesSyncXml.digest(saved.topics()).equals(l)) return finish(list, s, "stale", null);
                local = saved;
                valid(list, binding, generation);
                NotesRemote.Snapshot written = remote.push(binding.remoteId(), NotesSyncXml.write(local.topics()), disk.sha());
                s.cached = written;
                valid(list, binding, generation);
                return acknowledge(list, s, l, written.sha(), data, local);
            }
            if (pull && disk.exists()) {
                valid(list, binding, generation);
                backup(list, binding, "local", NotesSyncXml.write(local.topics()));
                s.state.pendingHash = r; s.state.pendingSha = disk.sha(); s.state.save(statePath(list, binding));
                WorkspaceNotesCoordinator.SyncSnapshot saved = data.applySync(list, remoteXml, local);
                valid(list, binding, generation);
                return acknowledge(list, s, r, disk.sha(), data, saved);
            }
            String status = switch (decision) {
                case CONFLICT -> "conflict";
                case FIRST_SYNC -> "first";
                case REMOTE_MISSING -> "missing";
                case REMOTE_CHANGED -> "remote";
                case LOCAL_CHANGED -> "pending";
                default -> "synced";
            };
            s.state.paused = Set.of("conflict", "first", "missing").contains(status);
            return finish(list, s, status, review);
        } catch (Exception error) {
            String key = error instanceof NotesRemote.Failure f ? f.key : "storage";
            if (error instanceof NotesRemote.Failure f && "rate".equals(key)) networkRetryAt = Math.max(networkRetryAt, f.retryAt);
            if (s == null) { views.put(list.context().root(), new View(null, key, 0, 0)); return new Result(key, null); }
            s.state.failures = Math.min(10, s.state.failures + 1);
            s.state.retryAt = Math.max(System.currentTimeMillis() + Math.min(1800000L, 15000L << s.state.failures),
                    error instanceof NotesRemote.Failure f ? f.retryAt : 0);
            if (Set.of("auth", "access", "binding", "conflict", "invalid.remote", "repository", "sha", "validation").contains(key)) s.state.paused = true;
            publish(list, s, key);
            try { if (s.binding != null) s.state.save(statePath(list, s.binding)); }
            catch (IOException persistenceFailure) { publish(list, s, "storage"); }
            return new Result(s.state.status, null);
        }
    }
    private Result acknowledge(TopicList list, Session s, String hash, String sha, WorkspaceNotesCoordinator data,
                               WorkspaceNotesCoordinator.SyncSnapshot uploaded) throws Exception {
        s.state.baseline = hash; s.state.remoteSha = sha == null ? "" : sha; s.state.synced = System.currentTimeMillis();
        s.state.pendingHash = ""; s.state.pendingSha = ""; s.state.paused = false; s.state.failures = 0; s.state.retryAt = 0;
        WorkspaceNotesCoordinator.SyncSnapshot now = data.syncSnapshot(list);
        s.schedule.acknowledged(now.revision(), !NotesSyncXml.digest(now.topics()).equals(hash), System.currentTimeMillis());
        return finish(list, s, NotesSyncXml.digest(now.topics()).equals(hash) ? "synced" : "pending", null);
    }
    private void backup(TopicList list, NotesSyncBinding binding, String side, String xml) throws IOException {
        Path dir = stateRoot.resolve("backups").resolve(statePath(list, binding).getFileName().toString());
        NotesSyncXml.atomicWrite(dir.resolve(System.currentTimeMillis() + "-" + UUID.randomUUID() + "-" + side + ".xml"), xml);
    }
    private List<TopicList> activeLists() {
        Set<TopicList> result = new LinkedHashSet<>();
        for (Project p : windows) if (!p.isDisposed()) {
            WorkspaceNotesService ws = WorkspaceNotesService.getInstance(p);
            if (ws.isReady()) result.addAll(ws.projects());
        }
        return List.copyOf(result);
    }
    private void tick() {
        if (disposed) return;
        List<TopicList> active = activeLists();
        sessions.keySet().removeIf(root -> active.stream().noneMatch(l -> l.context().root().equals(root)));
        views.keySet().removeIf(root -> active.stream().noneMatch(l -> l.context().root().equals(root)));
        for (TopicList list : active) {
            try {
                Session s = session(list);
                if (s.binding == null) { publishIfChanged(list, s, "unbound"); continue; }
                if (!SyncSettings.getInstance().getSyncConfig().isEnabled()) { publishIfChanged(list, s, "disabled"); continue; }
                long now = System.currentTimeMillis();
                if ("disabled".equals(s.state.status)) s.state.status = s.state.baseline.isEmpty() ? "first" : "pending";
                long revision = WorkspaceNotesCoordinator.getInstance().syncRevision(list);
                s.schedule.observe(revision, now);
                if (s.binding.policy() == NotesSyncBinding.Policy.MANUAL) { publishIfChanged(list, s, s.state.status); continue; }
                if (s.state.paused || now < s.state.retryAt || now < networkRetryAt) { publishIfChanged(list, s, s.state.status); continue; }
                if (s.schedule.due(now, s.state.checked, activated, s.state.paused, Math.max(s.state.retryAt, networkRetryAt))) {
                    s.schedule.attempted(now, s.binding.intervalMinutes());
                    execute(list, Operation.AUTO, null);
                    return; // Yield between projects so manual requests and shutdown are not starved.
                }
            } catch (Exception error) { views.put(list.context().root(), new View(null, "storage", 0, 0)); }
        }
    }
    private void publishIfChanged(TopicList list, Session s, String status) {
        View current = views.get(list.context().root());
        if (current == null || !Objects.equals(current.binding(), s.binding) || !current.status().equals(status)) publish(list, s, status);
    }
    @Override public void dispose() { disposed = true; windows.clear(); worker.shutdownNow(); }
}
