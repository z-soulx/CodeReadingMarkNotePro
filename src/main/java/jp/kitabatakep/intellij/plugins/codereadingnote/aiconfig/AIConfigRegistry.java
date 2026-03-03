package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry that scans, discovers, and tracks AI config files in a project.
 */
public class AIConfigRegistry {

    private static final Logger LOG = Logger.getInstance(AIConfigRegistry.class);

    /** Built-in ignore patterns that are always active */
    private static final List<String> BUILTIN_IGNORE_PATTERNS = Arrays.asList(
        ".DS_Store", "Thumbs.db", "desktop.ini",
        "*.swp", "*.swo", "*.tmp", "*.bak"
    );

    private final Project project;
    private final List<AIConfigEntry> entries = new ArrayList<>();
    private final Set<String> customPaths = new LinkedHashSet<>();
    private final Set<String> discoveredDirs = new LinkedHashSet<>();
    private final List<String> userIgnorePatterns = new ArrayList<>();

    public AIConfigRegistry(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Scans the project root for known AI config directories/files.
     * Merges with existing entries to preserve tracked/untracked state.
     */
    public void scan() {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        VirtualFile projectRoot = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (projectRoot == null || !projectRoot.isValid()) return;

        Set<String> discoveredPaths = new HashSet<>();
        discoveredDirs.clear();

        for (AIConfigType type : AIConfigType.getKnownTypes()) {
            String defaultPath = type.getDefaultPath();
            if (defaultPath == null) continue;

            VirtualFile target = projectRoot.findFileByRelativePath(defaultPath);
            if (target == null || !target.isValid()) continue;

            if (type.isDirectory() && target.isDirectory()) {
                String dirRel = getRelativePath(target, basePath);
                discoveredDirs.add(dirRel);
                collectFilesRecursively(target, basePath, discoveredPaths);
            } else if (!target.isDirectory()) {
                discoveredPaths.add(getRelativePath(target, basePath));
            }
        }

        for (String customPath : customPaths) {
            String absolutePath = (basePath + "/" + customPath).replace('\\', '/');
            VirtualFile target = LocalFileSystem.getInstance().refreshAndFindFileByPath(absolutePath);
            if (target == null || !target.isValid()) continue;
            if (target.isDirectory()) {
                String dirRel = getRelativePath(target, basePath);
                discoveredDirs.add(dirRel);
                collectFilesRecursively(target, basePath, discoveredPaths);
            } else {
                discoveredPaths.add(getRelativePath(target, basePath));
            }
        }

        mergeDiscoveredEntries(discoveredPaths);
        updateContentHashes();
    }

    private void collectFilesRecursively(@NotNull VirtualFile dir, @NotNull String basePath,
                                         @NotNull Set<String> paths) {
        for (VirtualFile child : dir.getChildren()) {
            String childRelPath = getRelativePath(child, basePath);
            if (child.isDirectory()) {
                if (!isIgnored(child.getName(), childRelPath)) {
                    discoveredDirs.add(childRelPath);
                    collectFilesRecursively(child, basePath, paths);
                }
            } else if (!isIgnored(child.getName(), childRelPath)) {
                paths.add(childRelPath);
            }
        }
    }

    /**
     * Checks if a file or directory should be ignored based on built-in and user patterns.
     * Supported patterns: exact name ("file.txt"), extension wildcard ("*.log"),
     * prefix wildcard ("temp*"), path segment (".hidden/").
     */
    private boolean isIgnored(@NotNull String name, @NotNull String relativePath) {
        for (String pattern : BUILTIN_IGNORE_PATTERNS) {
            if (matchesPattern(name, relativePath, pattern)) return true;
        }
        for (String pattern : userIgnorePatterns) {
            if (matchesPattern(name, relativePath, pattern)) return true;
        }
        return false;
    }

    private boolean matchesPattern(@NotNull String name, @NotNull String relativePath, @NotNull String pattern) {
        if (pattern.isEmpty()) return false;
        // Path-based pattern (contains '/')
        if (pattern.contains("/")) {
            String normalized = pattern.endsWith("/") ? pattern : pattern + "/";
            return relativePath.startsWith(normalized) || relativePath.contains("/" + normalized);
        }
        // Extension wildcard: *.ext
        if (pattern.startsWith("*.")) {
            return name.endsWith(pattern.substring(1));
        }
        // Prefix wildcard: prefix*
        if (pattern.endsWith("*")) {
            return name.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        // Exact name match
        return name.equals(pattern);
    }

    @NotNull
    private String getRelativePath(@NotNull VirtualFile file, @NotNull String basePath) {
        String filePath = file.getPath().replace('\\', '/');
        String base = basePath.replace('\\', '/');
        if (!base.endsWith("/")) base += "/";
        if (filePath.startsWith(base)) {
            return filePath.substring(base.length());
        }
        return filePath;
    }

    private void mergeDiscoveredEntries(@NotNull Set<String> discoveredPaths) {
        Map<String, AIConfigEntry> existingMap = entries.stream()
                .collect(Collectors.toMap(AIConfigEntry::getRelativePath, e -> e, (a, b) -> a));

        List<AIConfigEntry> merged = new ArrayList<>();
        for (String path : discoveredPaths) {
            AIConfigEntry existing = existingMap.get(path);
            if (existing != null) {
                merged.add(existing);
            } else {
                AIConfigType type = AIConfigType.detectType(path);
                merged.add(new AIConfigEntry(type, path));
            }
        }

        merged.sort(Comparator.comparing(AIConfigEntry::getRelativePath));
        entries.clear();
        entries.addAll(merged);
    }

    private void updateContentHashes() {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        for (AIConfigEntry entry : entries) {
            try {
                File file = new File(basePath, entry.getRelativePath());
                if (file.exists() && file.isFile()) {
                    byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
                    entry.setContentHash(calculateMD5(content));
                    entry.setLastModified(file.lastModified());
                }
            } catch (Exception e) {
                LOG.debug("Failed to update hash for: " + entry.getRelativePath(), e);
            }
        }
    }

    @NotNull
    private String calculateMD5(@NotNull byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @NotNull
    public List<AIConfigEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @NotNull
    public List<AIConfigEntry> getTrackedEntries() {
        return entries.stream()
                .filter(AIConfigEntry::isTracked)
                .collect(Collectors.toList());
    }

    @Nullable
    public AIConfigEntry findByPath(@NotNull String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        return entries.stream()
                .filter(e -> e.getRelativePath().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public void addCustomPath(@NotNull String path) {
        customPaths.add(path.replace('\\', '/'));
    }

    public void removeCustomPath(@NotNull String path) {
        customPaths.remove(path.replace('\\', '/'));
    }

    @NotNull
    public Set<String> getCustomPaths() {
        return Collections.unmodifiableSet(customPaths);
    }

    public void setCustomPaths(@NotNull Set<String> paths) {
        customPaths.clear();
        for (String p : paths) {
            customPaths.add(p.replace('\\', '/'));
        }
    }

    @NotNull
    public List<String> getUserIgnorePatterns() {
        return Collections.unmodifiableList(userIgnorePatterns);
    }

    public void setUserIgnorePatterns(@NotNull List<String> patterns) {
        userIgnorePatterns.clear();
        for (String p : patterns) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                userIgnorePatterns.add(trimmed);
            }
        }
    }

    @NotNull
    public List<String> getBuiltinIgnorePatterns() {
        return Collections.unmodifiableList(BUILTIN_IGNORE_PATTERNS);
    }

    /**
     * Returns entries grouped by their parent directory for tree display.
     */
    @NotNull
    public Map<String, List<AIConfigEntry>> getEntriesGroupedByDir() {
        Map<String, List<AIConfigEntry>> grouped = new LinkedHashMap<>();
        for (AIConfigEntry entry : entries) {
            String dir = entry.getParentDir();
            grouped.computeIfAbsent(dir, k -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    /**
     * Read file content for a given entry.
     */
    @Nullable
    public String readFileContent(@NotNull AIConfigEntry entry) {
        String basePath = project.getBasePath();
        if (basePath == null) return null;
        try {
            File file = new File(basePath, entry.getRelativePath());
            if (file.exists() && file.isFile()) {
                return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOG.warn("Failed to read file: " + entry.getRelativePath(), e);
        }
        return null;
    }

    /**
     * Compute a combined hash of all tracked files (paths + content).
     * Used for change detection before pushing to avoid no-op syncs.
     */
    @NotNull
    public String computeTrackedContentHash() {
        String basePath = project.getBasePath();
        if (basePath == null) return "";

        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            List<AIConfigEntry> tracked = getTrackedEntries();
            for (AIConfigEntry entry : tracked) {
                md.update(entry.getRelativePath().getBytes(StandardCharsets.UTF_8));
                File file = new File(basePath, entry.getRelativePath());
                if (file.exists() && file.isFile()) {
                    md.update(java.nio.file.Files.readAllBytes(file.toPath()));
                }
            }
            // Include discovered dirs in hash so empty dir changes are detected
            List<String> sortedDirs = new ArrayList<>(discoveredDirs);
            Collections.sort(sortedDirs);
            for (String dir : sortedDirs) {
                md.update(("DIR:" + dir).getBytes(StandardCharsets.UTF_8));
            }
            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            LOG.debug("Failed to compute tracked content hash", e);
            return "";
        }
    }

    /**
     * Collect all tracked files as a map of relativePath -> content bytes (for sync).
     */
    @NotNull
    public Map<String, byte[]> collectTrackedFilesContent() {
        Map<String, byte[]> result = new LinkedHashMap<>();
        String basePath = project.getBasePath();
        if (basePath == null) return result;

        for (AIConfigEntry entry : getTrackedEntries()) {
            try {
                File file = new File(basePath, entry.getRelativePath());
                if (file.exists() && file.isFile()) {
                    result.put(entry.getRelativePath(), java.nio.file.Files.readAllBytes(file.toPath()));
                }
            } catch (Exception e) {
                LOG.warn("Failed to read tracked file: " + entry.getRelativePath(), e);
            }
        }
        return result;
    }

    /**
     * All directory paths found during scan, including empty ones.
     */
    @NotNull
    public Set<String> getDiscoveredDirs() {
        return Collections.unmodifiableSet(discoveredDirs);
    }

    public void clear() {
        entries.clear();
        customPaths.clear();
        discoveredDirs.clear();
        userIgnorePatterns.clear();
    }

    public int size() {
        return entries.size();
    }
}
