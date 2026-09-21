package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.*;
import org.junit.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class GitHubNotesRemoteTest {
    private HttpServer server;
    private GitHubNotesRemote remote;
    private final List<String> requests = new ArrayList<>();
    private String sha = "observed", xml = "<topics><trash/></topics>", putSha, query, rawPath;
    private int failure, putCount;
    private String failureBody = "test-secret-never-log";
    private boolean missing, branchMissing, cacheFailure;
    @Before public void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle); server.start();
        GitHubSyncConfig config = new GitHubSyncConfig(); config.setRepository("owner/repo"); config.setToken("test-secret-never-log");
        config.setBranch("feature/笔记"); config.setBasePath("笔记");
        remote = new GitHubNotesRemote(config, "http://127.0.0.1:" + server.getAddress().getPort());
    }
    @After public void stop() { server.stop(0); }
    private void reply(HttpExchange e, int status, String body) throws java.io.IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        e.sendResponseHeaders(status, status == 304 ? -1 : data.length);
        if (status != 304) e.getResponseBody().write(data); e.close();
    }
    private void handle(HttpExchange e) throws java.io.IOException {
        requests.add(e.getRequestMethod() + " " + e.getRequestURI());
        if (failure != 0) { e.getResponseHeaders().add("Retry-After", "120"); reply(e, failure, failureBody); return; }
        if (e.getRequestURI().getPath().contains("/branches/")) { reply(e, branchMissing ? 404 : 200, "{}"); return; }
        boolean cache = e.getRequestURI().getPath().endsWith(".md5");
        if (cache && cacheFailure) { reply(e, 500, "failed"); return; }
        if (e.getRequestMethod().equals("PUT")) {
            JsonObject body = JsonParser.parseString(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!cache) {
                putCount++; putSha = body.has("sha") ? body.get("sha").getAsString() : null;
                if (!Objects.equals(putSha, missing ? null : sha)) { reply(e, 409, "moved"); return; }
                xml = new String(Base64.getDecoder().decode(body.get("content").getAsString()), StandardCharsets.UTF_8); sha = "written";
                assertEquals("feature/笔记", body.get("branch").getAsString());
            }
            reply(e, 200, "{\"content\":{\"sha\":\"written\"}}"); return;
        }
        if (cache || missing) { reply(e, 404, "{}"); return; }
        query = e.getRequestURI().getRawQuery(); rawPath = e.getRequestURI().getRawPath();
        if ("etag-v1".equals(e.getRequestHeaders().getFirst("If-None-Match"))) { reply(e, 304, ""); return; }
        e.getResponseHeaders().add("ETag", "etag-v1");
        JsonObject body = new JsonObject(); body.addProperty("sha", sha); body.addProperty("encoding", "base64");
        body.addProperty("content", Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8))); reply(e, 200, body.toString());
    }
    @Test public void branchPathEncodingAndConditionalRead() throws Exception {
        var first = remote.read("项目", null); assertTrue(first.exists());
        assertTrue(query.contains("feature%2F")); assertTrue(rawPath.contains("%E9"));
        assertSame(first, remote.read("项目", first));
    }
    @Test public void pushUsesObservedShaAndDoesNotRebaseOnConcurrentUpdate() throws Exception {
        var first = remote.read("a", null); sha = "changed-elsewhere";
        try { remote.push("a", "<topics/>", first.sha()); fail(); }
        catch (NotesRemote.Failure expected) { assertEquals("conflict", expected.key); }
        assertEquals("observed", putSha); assertEquals(1, putCount); assertEquals("<topics><trash/></topics>", xml);
    }
    @Test public void createsOnlyWithAbsencePreconditionAndUpdatesLegacyCache() throws Exception {
        missing = true; assertFalse(remote.read("a", null).exists());
        var written = remote.push("a", "<topics><trash/></topics>", null);
        assertEquals("written", written.sha()); assertNull(putSha);
        assertTrue(requests.stream().anyMatch(r -> r.startsWith("PUT") && r.endsWith(".md5")));
    }
    @Test public void missingBranchIsNotAnEmptyProject() throws Exception {
        missing = true; branchMissing = true;
        try { remote.read("a", null); fail(); } catch (NotesRemote.Failure error) { assertEquals("repository", error.key); }
    }
    @Test public void authRateAndCorruptXmlAreDistinctAndNeverExposeBodies() throws Exception {
        for (int status : new int[]{401, 429}) {
            failure = status;
            try { remote.read("a", null); fail(); } catch (NotesRemote.Failure error) {
                assertFalse(error.toString().contains("test-secret"));
                assertEquals(status == 401 ? "auth" : "rate", error.key);
                if (status == 429) assertTrue(error.retryAt > System.currentTimeMillis() + 100000);
            }
        }
        failure = 0; xml = "<broken>";
        try { remote.read("a", null); fail(); } catch (NotesRemote.Failure error) { assertEquals("invalid.remote", error.key); }
    }
    @Test public void cacheFailureDoesNotHideThatRemoteMayHaveCommitted() throws Exception {
        cacheFailure = true;
        try { remote.push("a", "<topics/>", "observed"); fail(); } catch (NotesRemote.Failure error) { assertEquals("cache", error.key); }
        assertEquals("<topics/>", remote.read("a", null).xml()); assertEquals(1, putCount);
    }
    @Test public void validationFailureIsNotMisreportedAsAnEditConflict() throws Exception {
        failure = 422;
        for (String message : new String[]{"Invalid request.\n\n\"sha\" wasn't supplied.", "Invalid commit message", "sha does not match"}) {
            JsonObject body = new JsonObject(); body.addProperty("message", message); failureBody = body.toString();
            try { remote.push("a", "<topics/>", "observed"); fail(); }
            catch (NotesRemote.Failure error) {
                assertEquals(message.contains("match") ? "conflict" : message.contains("sha") ? "sha" : "validation", error.key);
                assertFalse(error.toString().contains(message));
            }
        }
    }
    @Test public void emptyShaIsRejectedBeforeUploading() throws Exception {
        sha = "";
        try { remote.read("a", null); fail(); } catch (NotesRemote.Failure error) { assertEquals("sha", error.key); }
        try { remote.push("a", "<topics/>", ""); fail(); } catch (NotesRemote.Failure error) { assertEquals("sha", error.key); }
        assertEquals(0, putCount);
    }
}
