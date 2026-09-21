package jp.kitabatakep.intellij.plugins.codereadingnote.sync.github;

import com.google.gson.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Conditional notes transport. Raw server bodies and credentials never escape this class. */
public final class GitHubNotesRemote implements NotesRemote {
    private final GitHubSyncConfig config;
    private final String endpoint;
    public GitHubNotesRemote(GitHubSyncConfig config) { this(config, "https://api.github.com"); }
    // Injectable endpoint for deterministic HTTP protocol tests.
    public GitHubNotesRemote(GitHubSyncConfig config, String endpoint) { this.config = config.clone(); this.endpoint = endpoint; }
    private static String segment(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String contents(String path) {
        return endpoint + "/repos/" + config.getRepository() + "/contents/"
                + String.join("/", Arrays.stream(path.split("/")).map(GitHubNotesRemote::segment).toList());
    }
    private String path(String id) {
        if (!NotesSyncBinding.validId(id)) throw new IllegalArgumentException("Invalid project identifier");
        String base = NotesSyncBinding.normalizeBase(config.getBasePath());
        return (base.isEmpty() ? "" : base + "/") + id + "/CodeReadingNote.xml";
    }
    private record Response(int status, String body, String etag) {}
    private Response request(String url, String method, JsonObject body, String etag) throws IOException {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(20000);
            c.setRequestProperty("Authorization", "Bearer " + config.getToken());
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setRequestProperty("User-Agent", "CodeReadingNotePro");
            if (etag != null && !etag.isEmpty()) c.setRequestProperty("If-None-Match", etag);
            if (body != null) {
                c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            }
            int status = c.getResponseCode();
            if (status == 401) throw new Failure("auth", 0);
            if (status == 403 || status == 429) {
                long retry = System.currentTimeMillis() + 60000;
                String after = c.getHeaderField("Retry-After"), reset = c.getHeaderField("X-RateLimit-Reset");
                try {
                    if (after != null) {
                        try { retry = Math.max(retry, System.currentTimeMillis() + Long.parseLong(after) * 1000); }
                        catch (NumberFormatException date) { retry = Math.max(retry, java.time.ZonedDateTime.parse(after, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()); }
                    }
                    if (reset != null) retry = Math.max(retry, Long.parseLong(reset) * 1000);
                } catch (RuntimeException invalidHeader) { /* Conservative default retry remains in effect. */ }
                throw new Failure(status == 429 || after != null || reset != null ? "rate" : "access", retry);
            }
            if (status == 409 || status == 412) throw new Failure("conflict", 0);
            if (status == 422) {
                String errorBody = "";
                InputStream error = c.getErrorStream();
                if (error != null) try (error) { errorBody = new String(error.readNBytes(65536), StandardCharsets.UTF_8); }
                throw new Failure(GitHubApiError.validationKind(errorBody), 0);
            }
            if (status == 304 || status == 404) return new Response(status, "", c.getHeaderField("ETag"));
            if (status < 200 || status >= 300) throw new Failure("network", 0);
            try (InputStream input = c.getInputStream()) {
                byte[] bytes = input.readNBytes(16 * 1024 * 1024 + 1);
                if (bytes.length > 16 * 1024 * 1024) throw new Failure("invalid.remote", 0);
                return new Response(status, new String(bytes, StandardCharsets.UTF_8), c.getHeaderField("ETag"));
            }
        } catch (Failure error) { throw error; }
        catch (IOException | RuntimeException error) { throw new Failure("network", 0); }
        finally { if (c != null) c.disconnect(); }
    }
    private JsonObject object(Response r) throws IOException {
        try { return JsonParser.parseString(r.body()).getAsJsonObject(); }
        catch (RuntimeException malformed) { throw new Failure("invalid.remote", 0); }
    }
    private void requireBranch() throws IOException {
        Response r = request(endpoint + "/repos/" + config.getRepository() + "/branches/" + segment(config.getBranch()), "GET", null, null);
        if (r.status == 404) throw new Failure("repository", 0);
    }
    @Override public Snapshot read(String id, Snapshot cached) throws IOException {
        Response r = request(contents(path(id)) + "?ref=" + segment(config.getBranch()), "GET", null, cached == null ? null : cached.etag());
        if (r.status == 304 && cached != null) return cached;
        if (r.status == 404) { requireBranch(); return new Snapshot(null, null, null); }
        try {
            JsonObject json = object(r);
            if (!"base64".equals(json.get("encoding").getAsString())) throw new IllegalArgumentException();
            String xml = new String(Base64.getMimeDecoder().decode(json.get("content").getAsString()), StandardCharsets.UTF_8);
            NotesSyncXml.topics(xml);
            JsonElement version = json.get("sha");
            if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isString() || version.getAsString().isBlank()) throw new Failure("sha", 0);
            String sha = version.getAsString();
            return new Snapshot(xml, sha, r.etag);
        } catch (Failure failure) { throw failure;
        } catch (RuntimeException | IOException badData) { throw new Failure("invalid.remote", 0); }
    }
    private Snapshot put(String path, String xml, String expectedSha) throws IOException {
        if (expectedSha != null && expectedSha.isBlank()) throw new Failure("sha", 0);
        JsonObject body = new JsonObject(); body.addProperty("message", "Update code reading notes");
        body.addProperty("branch", config.getBranch()); body.addProperty("content", Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)));
        if (expectedSha != null) body.addProperty("sha", expectedSha);
        Response r = request(contents(path), "PUT", body, null);
        if (r.status == 404) throw new Failure("repository", 0);
        try { return new Snapshot(xml, object(r).getAsJsonObject("content").get("sha").getAsString(), null); }
        catch (RuntimeException malformed) { throw new Failure("invalid.remote", 0); }
    }
    @Override public Snapshot push(String id, String xml, String expectedSha) throws IOException {
        Snapshot result = put(path(id), xml, expectedSha);
        repairCache(id, result);
        return result;
    }
    public void repairCache(String id, Snapshot snapshot) throws IOException {
        try {
            String cachePath = path(id) + ".md5";
            Response old = request(contents(cachePath) + "?ref=" + segment(config.getBranch()), "GET", null, null);
            String sha = old.status == 404 ? null : object(old).get("sha").getAsString();
            String md5 = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(snapshot.xml().getBytes(StandardCharsets.UTF_8)));
            put(cachePath, md5, sha);
        } catch (Exception cacheFailure) { throw new Failure("cache", 0); }
    }
    @Override public List<String> projects() throws IOException {
        Response r = request(contents(NotesSyncBinding.normalizeBase(config.getBasePath())) + "?ref=" + segment(config.getBranch()), "GET", null, null);
        if (r.status == 404) { requireBranch(); return List.of(); }
        try {
            List<String> result = new ArrayList<>();
            for (JsonElement e : JsonParser.parseString(r.body).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                if ("dir".equals(o.get("type").getAsString())) result.add(o.get("name").getAsString());
            }
            result.sort(String.CASE_INSENSITIVE_ORDER); return result;
        } catch (RuntimeException malformed) { throw new Failure("invalid.remote", 0); }
    }
}
