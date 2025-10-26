package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.AutoFixService;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.FixResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.LineOffsetDetector;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.OffsetInfo;
import jp.kitabatakep.intellij.plugins.codereadingnote.remark.BookmarkUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 修复单个 TopicLine 的行号错位
 * 增强版：使用新的 AutoFixService
 */
public class FixLineRemarkAction extends CommonAnAction
{
    Project project;
    Function<Void, Pair<Topic, TopicLine>> fetcher;
    
    public FixLineRemarkAction(Project project, Function<Void, Pair<Topic, TopicLine>> fetcher) {
        super("Fix Line Offset", "修复该行的行号错位", AllIcons.Actions.RefactoringBulb);
        this.project = project;
        this.fetcher = fetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            if (e.getProject() == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            
            // 检测是否需要修复
            Pair<Topic, TopicLine> payload = fetcher.apply(null);
            if (payload == null || payload.getSecond() == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            
            TopicLine topicLine = payload.getSecond();
            OffsetInfo info = LineOffsetDetector.getInstance().detectOffset(project, topicLine);

            // 始终可见且可点；如果没有需要修复，会在点击后提示“无需修复”
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(true);

            // 提示信息：若检测到偏移，增强菜单文案；否则使用默认文案
            String description = info.getOffsetDescription();
            if (description != null && !description.isEmpty()) {
                e.getPresentation().setText("Fix Line Offset (" + description + ")");
                e.getPresentation().setDescription("修复行号错位: " + description);
            } else {
                e.getPresentation().setText("Fix Line Offset");
                e.getPresentation().setDescription("修复行号错位");
            }
        } catch (Exception ex) {
            // 检测失败：显示禁用项，避免整菜单为空
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            e.getPresentation().setText("Fix Line Offset (检测失败)");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Pair<Topic, TopicLine> payload = fetcher.apply(null);
        if (payload == null || payload.getSecond() == null) {
            return;
        }
        
        TopicLine topicLine = payload.getSecond();
        
        // 使用新的 AutoFixService
        FixResult result = AutoFixService.getInstance().fixLine(project, topicLine);
        
        // 显示结果
        if (result.isSuccess() && result.hasFixed()) {
            Messages.showInfoMessage(
                    project,
                    result.getSummary(),
                    "修复完成"
            );
        } else if (!result.isSuccess()) {
            Messages.showWarningDialog(
                    project,
                    result.getMessage() != null ? result.getMessage() : "修复失败",
                    "修复失败"
            );
        }
    }
}
