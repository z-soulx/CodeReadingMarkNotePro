package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.AutoFixService;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.FixResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FixRemarkAction extends CommonAnAction
{
    private Project project;
    public FixRemarkAction(Project project) {
        super("Fix All RemarkAction Offset", "FixRemarkAction", AllIcons.General.WarningDialog);
        this.project = project;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        if (project == null) {
            return;
        }
        
        // 使用新的 AutoFixService
        FixResult result = AutoFixService.getInstance().fixAll(project);
        
        // 显示结果
        if (result.isSuccess()) {
            if (result.hasFixed()) {
                Messages.showInfoMessage(
                        project,
                        result.getSummary(),
                        "修复完成"
                );
            } else {
                Messages.showInfoMessage(
                        project,
                        "所有 TopicLine 已同步，无需修复",
                        "无需修复"
                );
            }
        } else {
            Messages.showWarningDialog(
                    project,
                    result.getMessage() != null ? result.getMessage() : "修复失败",
                    "修复失败"
            );
        }
    }

}
