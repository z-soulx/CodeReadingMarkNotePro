package jp.kitabatakep.intellij.plugins.codereadingnote.workspace;

import java.util.ArrayList;
import java.util.List;

public class AIWorkspaceCommand {
    public String id = "";
    public String displayName = "";
    /** Backward-compatible alias for older command files. */
    public String name = "";
    public String executable = "";
    public List<String> args = new ArrayList<>();
    public String workingDirectory = "";
    public boolean enabled = true;
    /** When false, execute in an IDEA Terminal session without activating the tool window. */
    public boolean openTerminal = true;
    /** terminal (IDEA Terminal, focused) or silent (same Terminal session, no focus). */
    public String executionMode = "terminal";
    /**
     * Where $FilePath$ / $FileDir$ / $FileName$ come from:
     * project = Project tool window selection (falls back to editor);
     * editor = currently open editor file.
     * Missing/empty = project (migration-safe).
     */
    public String fileSource = "project";
    public boolean confirmEachTime = true;
    public AIWorkspaceCommand() { }
}
