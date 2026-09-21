package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.GitHubSyncConfig;
import org.jdom.Element;
import java.io.IOException;
import java.nio.file.*;

public record NotesSyncBinding(String repository, String branch, String basePath, String remoteId,
                               Policy policy, int intervalMinutes) {
    public enum Policy { MANUAL, PUSH, BIDIRECTIONAL }
    public NotesSyncBinding {
        if (repository == null || !repository.matches("[\\w.-]+/[\\w.-]+") || branch == null || branch.isBlank()
                || !validId(remoteId) || basePath == null || basePath.startsWith("/") || basePath.contains("\\")
                || java.util.Arrays.stream(basePath.split("/", -1)).anyMatch(s -> s.equals("..") || s.equals("."))
                || policy == null || intervalMinutes < 1 || intervalMinutes > 60) throw new IllegalArgumentException("Invalid binding");
    }
    public static boolean validId(String id) {
        return id != null && !id.isBlank() && !id.equals(".") && !id.equals("..")
                && id.chars().noneMatch(c -> c < 32 || "\\/:*?\"<>|".indexOf(c) >= 0);
    }
    public static String legacyId(String name) { return name.replaceAll("[\\\\/:*?\"<>|]", "_"); }
    public String identity() { return "github\n" + repository.toLowerCase(java.util.Locale.ROOT) + "\n" + branch + "\n" + basePath + "\n" + remoteId + "\nnotes"; }
    public boolean matches(GitHubSyncConfig config) {
        return repository.equalsIgnoreCase(String.valueOf(config.getRepository())) && branch.equals(config.getBranch())
                && basePath.equals(normalizeBase(config.getBasePath()));
    }
    public static String normalizeBase(String value) { return value.replaceAll("/+$", ""); }
    public static NotesSyncBinding create(GitHubSyncConfig config, String id, Policy policy, int minutes) {
        return new NotesSyncBinding(config.getRepository(), config.getBranch(), normalizeBase(config.getBasePath()), id, policy, minutes);
    }
    public static Path path(Path root) { return root.resolve(".idea/notesSyncBinding.xml"); }
    public static NotesSyncBinding read(Path root) throws IOException {
        Path file = path(root);
        validatePath(root);
        if (!Files.exists(file)) return null;
        Element e = NotesSyncXml.parse(Files.readString(file));
        try {
            if (!"notesSyncBinding".equals(e.getName())) throw new IllegalArgumentException();
            return new NotesSyncBinding(e.getAttributeValue("repository"), e.getAttributeValue("branch"), e.getAttributeValue("basePath"),
                    e.getAttributeValue("remoteId"), Policy.valueOf(e.getAttributeValue("policy", "MANUAL")),
                    Integer.parseInt(e.getAttributeValue("intervalMinutes", "5")));
        } catch (RuntimeException error) { throw new IOException("Invalid binding", error); }
    }
    public void save(Path root) throws IOException {
        validatePath(root);
        if (!Files.isDirectory(root.resolve(".idea"))) throw new IOException("Missing project metadata");
        Element e = new Element("notesSyncBinding").setAttribute("version", "1")
                .setAttribute("repository", repository).setAttribute("branch", branch).setAttribute("basePath", basePath)
                .setAttribute("remoteId", remoteId).setAttribute("policy", policy.name()).setAttribute("intervalMinutes", "" + intervalMinutes);
        NotesSyncXml.atomicWrite(path(root), NotesSyncXml.write(e));
    }
    private static void validatePath(Path root) throws IOException {
        for (Path p : new Path[]{root, root.resolve(".idea"), path(root)})
            if (Files.exists(p, LinkOption.NOFOLLOW_LINKS) && jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceDiscovery.isLink(p))
                throw new IOException("Linked project metadata");
    }
}
