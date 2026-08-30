package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class AIWorkspaceVersionService {
    public static final String DEFAULT_VERSION = "1.0.0";
    private static final Pattern SEMVER = Pattern.compile("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?");
    private final Project project;
    public AIWorkspaceVersionService(@NotNull Project project) { this.project = project; }
    @NotNull public static AIWorkspaceVersionService getInstance(@NotNull Project project) { return project.getService(AIWorkspaceVersionService.class); }
    @NotNull private Path file() { return AIWorkspaceService.getInstance(project).getWorkspaceRoot().resolve("VERSION"); }
    @NotNull public String readVersion() throws IOException {
        Path f = file();
        if (!Files.exists(f)) { Files.createDirectories(f.getParent()); Files.writeString(f, DEFAULT_VERSION + "\n", StandardCharsets.UTF_8); return DEFAULT_VERSION; }
        String version = Files.readString(f, StandardCharsets.UTF_8).trim();
        if (!isValidSemver(version)) throw new IllegalArgumentException("Invalid semver: " + version);
        return version;
    }
    public void writeVersion(@NotNull String version) throws IOException { if (!isValidSemver(version)) throw new IllegalArgumentException("Invalid semver: " + version); Files.createDirectories(file().getParent()); Files.writeString(file(), version + "\n", StandardCharsets.UTF_8); }
    @NotNull public String bumpPatch() throws IOException { String current = readVersion(); Semver v = Semver.parse(current); String next = (v.major + "." + v.minor + "." + (v.patch + 1)); writeVersion(next); return next; }
    public static boolean isValidSemver(String value) { return value != null && SEMVER.matcher(value.trim()).matches(); }
    public static Semver parseSemver(String value) { if (!isValidSemver(value)) throw new IllegalArgumentException("Invalid semver: " + value); return Semver.parse(value.trim()); }
    public record Semver(int major, int minor, int patch) { static Semver parse(String s) { String[] p = s.split("[-+]")[0].split("\\."); return new Semver(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])); } }
}
