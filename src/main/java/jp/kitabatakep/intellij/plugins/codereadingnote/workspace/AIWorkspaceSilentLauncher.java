package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves GUI app names (cursor, typora) to real executables on Windows and macOS.
 */
final class AIWorkspaceSilentLauncher {
    private static final String[] WINDOWS_EXTS = {".exe", ".cmd", ".bat", ".com"};

    private AIWorkspaceSilentLauncher() { }

    @Nullable
    static List<String> buildSilentCommand(@NotNull String executable,
                                           @NotNull List<String> args,
                                           boolean windows,
                                           boolean mac,
                                           @NotNull List<String> pathDirs,
                                           @NotNull List<Path> searchRoots,
                                           @NotNull String comSpec) {
        String trimmed = executable.trim();
        if (trimmed.isEmpty()) return null;

        if (mac) {
            List<String> macCommand = buildMacCommand(trimmed, args, pathDirs, searchRoots);
            if (macCommand != null) return macCommand;
        }

        Path resolved = resolveLaunchFile(trimmed, windows, pathDirs, searchRoots);
        if (resolved == null) return null;
        if (windows && !hasWindowsExecutableExtension(resolved.getFileName().toString())) return null;
        return wrapIfNeeded(resolved, args, windows, comSpec);
    }

    static boolean shouldDetach(@NotNull List<String> command, boolean windows, boolean mac) {
        if (command.isEmpty()) return false;
        if (mac && "open".equals(command.get(0))) return true;
        int index = 0;
        if (windows && isCmdExecutable(command.get(0))) {
            index = command.size() > 1 && "/c".equalsIgnoreCase(command.get(1)) ? 2 : 1;
        }
        if (index >= command.size()) return false;
        String name = Path.of(command.get(index)).getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.equals("java.exe") || name.equals("java")) return false;
        if (name.endsWith(".exe")) return true;
        return name.contains("cursor") || name.contains("typora") || name.equals("code.cmd") || name.equals("code");
    }

    static boolean isMacAppBundle(@Nullable Path path) {
        return path != null && Files.isDirectory(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".app");
    }

    @Nullable
    static Path preferWindowsImage(@NotNull Path requested) {
        String fileName = requested.getFileName().toString();
        if (hasWindowsExecutableExtension(fileName) && Files.isRegularFile(requested)) return requested;
        Path parent = requested.getParent();
        if (parent != null && !hasWindowsExecutableExtension(fileName)) {
            for (String ext : WINDOWS_EXTS) {
                Path sibling = parent.resolve(fileName + ext);
                if (Files.isRegularFile(sibling)) return sibling;
            }
        }
        return Files.isRegularFile(requested) ? requested : null;
    }

    @NotNull
    static List<String> wrapIfNeeded(@NotNull Path launch, @NotNull List<String> args, boolean windows, @NotNull String comSpec) {
        List<String> command = new ArrayList<>();
        String launchString = launch.toString();
        if (windows && needsWindowsShell(launchString)) {
            command.add(comSpec.isBlank() ? "cmd.exe" : comSpec);
            command.add("/c");
        }
        command.add(launchString);
        command.addAll(args);
        return command;
    }

    private static List<String> buildMacCommand(String executable, List<String> args, List<String> pathDirs, List<Path> searchRoots) {
        Path direct = Path.of(executable);
        if (isMacAppBundle(direct)) return openCommand(direct.toString(), args);
        if (direct.isAbsolute() || executable.contains("/") || executable.contains("\\")) {
            if (Files.isRegularFile(direct) && Files.isExecutable(direct)) {
                List<String> command = new ArrayList<>();
                command.add(direct.toString());
                command.addAll(args);
                return command;
            }
            if (isMacAppBundle(direct)) return openCommand(direct.toString(), args);
        }
        Path onPath = findOnPath(executable, false, pathDirs);
        if (onPath != null) {
            List<String> command = new ArrayList<>();
            command.add(onPath.toString());
            command.addAll(args);
            return command;
        }
        Path app = findMacApp(executable, searchRoots);
        if (app != null) return openCommand(app.toString(), args);
        if (looksLikeMacAppName(executable)) return openCommand(appName(executable), args);
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(args);
        return command;
    }

    @Nullable
    private static Path resolveLaunchFile(String executable, boolean windows, List<String> pathDirs, List<Path> searchRoots) {
        Path direct = Path.of(executable);
        boolean pathLike = direct.isAbsolute() || executable.contains("/") || executable.contains("\\");
        if (pathLike) {
            Path preferred = windows ? preferWindowsImage(direct) : (Files.isRegularFile(direct) ? direct : null);
            if (preferred != null && (!windows || hasWindowsExecutableExtension(preferred.getFileName().toString()))) {
                return preferred;
            }
            if (direct.isAbsolute() && !windows) return null;
        }
        Path onPath = findOnPath(executable, windows, pathDirs);
        if (onPath != null) {
            Path preferred = windows ? preferWindowsImage(onPath) : onPath;
            if (preferred != null && (!windows || hasWindowsExecutableExtension(preferred.getFileName().toString()))) {
                return preferred;
            }
        }
        return windows ? findInSearchRoots(executable, searchRoots) : null;
    }

    @Nullable
    static Path findOnPath(String executable, boolean windows, List<String> pathDirs) {
        String name = Path.of(executable).getFileName().toString();
        boolean requestedExt = windows && hasWindowsExecutableExtension(name);
        if (windows && !requestedExt) {
            for (String dir : pathDirs) {
                if (dir == null || dir.isBlank()) continue;
                for (String ext : WINDOWS_EXTS) {
                    Path candidate = Path.of(dir, name + ext);
                    if (Files.isRegularFile(candidate)) return candidate;
                }
            }
        }
        for (String dir : pathDirs) {
            if (dir == null || dir.isBlank()) continue;
            Path candidate = Path.of(dir, name);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    @Nullable
    static Path findInSearchRoots(String executable, List<Path> searchRoots) {
        String base = stripKnownExtension(Path.of(executable).getFileName().toString());
        if (base.isEmpty()) return null;
        String capital = capitalize(base);
        String[] folders = distinct(base, capital);
        String[] files = distinct(base + ".exe", capital + ".exe", base + ".cmd", capital + ".cmd");
        for (Path root : searchRoots) {
            if (root == null) continue;
            for (String folder : folders) {
                Path dir = root.resolve(folder);
                for (String file : files) {
                    Path candidate = dir.resolve(file);
                    if (Files.isRegularFile(candidate)) return candidate;
                }
                Path shim = dir.resolve("resources").resolve("app").resolve("bin").resolve(base);
                Path preferred = preferWindowsImage(shim);
                if (preferred != null && hasWindowsExecutableExtension(preferred.getFileName().toString())) return preferred;
            }
        }
        return null;
    }

    @Nullable
    static Path findMacApp(String executable, List<Path> searchRoots) {
        String base = stripKnownExtension(Path.of(executable).getFileName().toString());
        if (base.isEmpty()) return null;
        String[] names = distinct(base + ".app", capitalize(base) + ".app");
        for (Path root : searchRoots) {
            if (root == null) continue;
            for (String name : names) {
                Path app = root.resolve(name);
                if (Files.isDirectory(app)) return app;
            }
        }
        return null;
    }

    private static List<String> openCommand(String application, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add("open");
        command.add("-a");
        command.add(application);
        command.addAll(args);
        return command;
    }

    static boolean hasWindowsExecutableExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : WINDOWS_EXTS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private static boolean needsWindowsShell(String launch) {
        String name = Path.of(launch).getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".cmd") || name.endsWith(".bat") || !hasWindowsExecutableExtension(name);
    }

    private static boolean isCmdExecutable(String executable) {
        String name = Path.of(executable).getFileName().toString().toLowerCase(Locale.ROOT);
        return name.equals("cmd.exe") || name.equals("cmd");
    }

    static String stripKnownExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".app")) return fileName.substring(0, fileName.length() - 4);
        for (String ext : WINDOWS_EXTS) {
            if (lower.endsWith(ext)) return fileName.substring(0, fileName.length() - ext.length());
        }
        return fileName;
    }

    static String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean looksLikeMacAppName(String executable) {
        String base = stripKnownExtension(Path.of(executable).getFileName().toString()).toLowerCase(Locale.ROOT);
        return base.equals("cursor") || base.equals("typora") || base.equals("code") || base.endsWith(".app");
    }

    static String appName(String executable) {
        return capitalize(stripKnownExtension(Path.of(executable).getFileName().toString()));
    }

    private static String[] distinct(String... values) {
        List<String> unique = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank() && unique.stream().noneMatch(existing -> existing.equalsIgnoreCase(value))) {
                unique.add(value);
            }
        }
        return unique.toArray(String[]::new);
    }
}
