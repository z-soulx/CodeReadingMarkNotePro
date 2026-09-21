package jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents one file's merge decision during AI config pull.
 * Each item has a category (from three-way analysis) and an action the user can toggle.
 */
public class AIConfigMergeItem {

    public enum Action {
        TAKE_REMOTE,
        KEEP_LOCAL,
        ADD,
        DELETE,
        SKIP
    }

    private final String relativePath;
    private final AIConfigMergeCategory category;
    private final Action defaultAction;
    private Action userAction;
    private final String localHash;
    private final String remoteHash;
    private final String baseHash;
    private final byte[] remoteContent;

    public AIConfigMergeItem(@NotNull String relativePath,
                             @NotNull AIConfigMergeCategory category,
                             @NotNull Action defaultAction,
                             @Nullable String localHash,
                             @Nullable String remoteHash,
                             @Nullable String baseHash,
                             @Nullable byte[] remoteContent) {
        this.relativePath = relativePath;
        this.category = category;
        this.defaultAction = defaultAction;
        this.userAction = defaultAction;
        this.localHash = localHash != null ? localHash : "";
        this.remoteHash = remoteHash != null ? remoteHash : "";
        this.baseHash = baseHash != null ? baseHash : "";
        this.remoteContent = remoteContent;
    }

    @NotNull
    public String getRelativePath() {
        return relativePath;
    }

    @NotNull
    public AIConfigMergeCategory getCategory() {
        return category;
    }

    @NotNull
    public Action getDefaultAction() {
        return defaultAction;
    }

    @NotNull
    public Action getUserAction() {
        return userAction;
    }

    public void setUserAction(@NotNull Action userAction) {
        this.userAction = userAction;
    }

    @NotNull
    public String getLocalHash() {
        return localHash;
    }

    @NotNull
    public String getRemoteHash() {
        return remoteHash;
    }

    @NotNull
    public String getBaseHash() {
        return baseHash;
    }

    @Nullable
    public byte[] getRemoteContent() {
        return remoteContent;
    }

    /**
     * Whether this item will cause a write/delete when applied (not skipped or kept as-is).
     */
    public boolean willModifyLocal() {
        return userAction == Action.TAKE_REMOTE
            || userAction == Action.ADD
            || userAction == Action.DELETE;
    }
}
