package jp.kitabatakep.intellij.plugins.codereadingnote.sync.github;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import jp.kitabatakep.intellij.plugins.codereadingnote.settings.*;
import org.junit.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Regression for issue #15: existing XML in a non-default branch must not be submitted as a create. */
public class GitHubExistingFileTest {
    private HttpServer server;
    private GitHubSyncProvider provider;
    private GitHubSyncConfig config;
    private Disposable lifetime;
    private Project project;
    private String sha = "notes-v1", cacheSha = "cache-v1", current = "old notes";
    private int readsFailure, writes, deletes;
    private boolean malformedSha, fileMissing, branchMissing;
    private final List<String> queries = new ArrayList<>();
    private final List<String> sentVersions = new ArrayList<>();
    private final List<String> branches = new ArrayList<>();
    @Before public void setup() throws Exception {
        lifetime = Disposer.newDisposable();
        LanguageSettings language = new LanguageSettings(); language.setSelectedLanguage(PluginLanguage.ENGLISH);
        Application app = (Application) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Application.class}, (p, m, a) -> {
            if (m.getName().equals("getService") && a[0] == LanguageSettings.class) return language;
            if (m.getReturnType() == boolean.class) return false; return null;
        });
        ApplicationManager.setApplication(app, lifetime);
        project = (Project) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Project.class}, (p, m, a) -> m.getReturnType() == boolean.class ? false : null);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle); server.start();
        provider = new GitHubSyncProvider("http://127.0.0.1:" + server.getAddress().getPort());
        config = new GitHubSyncConfig(); config.setRepository("owner/repo"); config.setToken("test-sensitive-token");
        config.setBranch("feature/笔记"); config.setBasePath("notes");
    }
    @After public void close() { server.stop(0); Disposer.dispose(lifetime); }
    private void reply(HttpExchange e, int code, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); e.sendResponseHeaders(code, bytes.length);
        e.getResponseBody().write(bytes); e.close();
    }
    private void handle(HttpExchange e) throws java.io.IOException {
        String path = e.getRequestURI().getPath();
        if (e.getRequestMethod().equals("DELETE")) { deletes++; reply(e, 200, "{}"); return; }
        if (path.contains("/branches/")) { reply(e, branchMissing ? 404 : 200, "{}"); return; }
        boolean cache = path.endsWith(".md5");
        if (e.getRequestMethod().equals("GET")) {
            queries.add(e.getRequestURI().getRawQuery());
            if (readsFailure != 0) { reply(e, readsFailure, "{\"message\":\"Access failed test-sensitive-token\"}"); return; }
            // Simulate the actual bug: no ref reads the default branch, where this project does not exist.
            if (e.getRequestURI().getRawQuery() == null || !e.getRequestURI().getRawQuery().startsWith("ref=feature%2F") || fileMissing) { reply(e, 404, "{}"); return; }
            JsonObject data = new JsonObject(); if (!malformedSha) data.addProperty("sha", cache ? cacheSha : sha);
            data.addProperty("content", Base64.getEncoder().encodeToString((cache ? "old checksum" : current).getBytes(StandardCharsets.UTF_8)));
            reply(e, 200, data.toString()); return;
        }
        writes++;
        JsonObject request = JsonParser.parseString(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        String version = request.has("sha") ? request.get("sha").getAsString() : null;
        branches.add(request.get("branch").getAsString());
        if (!Objects.equals(fileMissing ? null : cache ? cacheSha : sha, version)) {
            reply(e, 422, "{\"message\":\"Invalid request.\\n\\n\\\"sha\\\" wasn't supplied.\"}"); return;
        }
        if (cache) cacheSha = "cache-v2";
        else { sentVersions.add(version); current = new String(Base64.getDecoder().decode(request.get("content").getAsString()), StandardCharsets.UTF_8); sha = "notes-v" + (sentVersions.size() + 1); }
        reply(e, 200, "{}");
    }
    @Test public void existingXmlCanBeUpdatedRepeatedlyWithoutDeletingIt() {
        assertTrue(provider.push(project, config, "<topics><first/></topics>", "project").isSuccess());
        assertTrue(provider.push(project, config, "<topics><second/></topics>", "project").isSuccess());
        assertEquals(List.of("notes-v1", "notes-v2"), sentVersions);
        assertEquals("<topics><second/></topics>", current); assertEquals(0, deletes);
        assertTrue(queries.stream().allMatch(q -> q != null && q.startsWith("ref=feature%2F")));
        assertTrue(branches.stream().allMatch("feature/笔记"::equals));
    }
    @Test public void unreadableShaNeverFallsBackToCreatingAFile() {
        for (int code : new int[]{401, 403, 500}) {
            readsFailure = code; var result = provider.push(project, config, "new notes", "project");
            assertFalse(result.isSuccess()); assertFalse(result.getMessage().contains("test-sensitive-token"));
        }
        assertEquals(0, writes); assertEquals("old notes", current);
    }
    @Test public void missingShaInSuccessfulResponseStopsBeforePut() {
        malformedSha = true;
        assertFalse(provider.push(project, config, "new notes", "project").isSuccess());
        assertEquals(0, writes);
    }
    @Test public void branchNotFoundIsNotPermissionToCreate() {
        fileMissing = true; branchMissing = true;
        assertFalse(provider.push(project, config, "new notes", "project").isSuccess()); assertEquals(0, writes);
    }
    @Test public void escapedErrorKeepsQuotesAndNewlinesAndRedactsCredentials() {
        String body = "{\"message\":\"Invalid request.\\n\\n\\\"sha\\\" wasn't supplied. test-sensitive-token\"}";
        String formatted = GitHubApiError.format(422, body, "test-sensitive-token");
        assertTrue(formatted.contains("\n\n\"sha\" wasn't supplied."));
        assertFalse(formatted.contains("\\n")); assertFalse(formatted.contains("test-sensitive-token"));
        assertTrue(formatted.contains("Do not delete")); assertEquals("sha", GitHubApiError.validationKind(body));
        assertEquals("validation", GitHubApiError.validationKind("{\"message\":\"Invalid commit message\"}"));
        assertEquals("conflict", GitHubApiError.validationKind("{\"message\":\"sha does not match\"}"));
    }
}
