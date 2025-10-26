package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.AutoFixService;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.FixResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.LineOffsetDetector;
import jp.kitabatakep.intellij.plugins.codereadingnote.autofix.OffsetInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

/**
 * 修复 Topic 下所有 TopicLine 的行号错位
 * 增强版：使用新的 AutoFixService
 */
public class FixTopicRemarkAction extends CommonAnAction
{
    private Project project;
    private Function<Void, Topic> topicFetcher;
    
    public FixTopicRemarkAction(Project project, Function<Void, Topic> topicFetcher) {
        super("Fix Topic Offset", "修复该 Topic 下所有行的错位", AllIcons.Actions.RefactoringBulb);
        this.project = project;
        this.topicFetcher = topicFetcher;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            if (e.getProject() == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            
            Topic topic = topicFetcher.apply(null);
            if (topic == null || topic.getLines().isEmpty()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            
            // 检测是否有需要修复的行
            Map<TopicLine, OffsetInfo> offsetMap = LineOffsetDetector.getInstance().detectTopic(project, topic);
            long offsetCount = offsetMap.values().stream()
                    .filter(info -> info.getStatus().needsFix())
                    .count();
            
            // 始终可见且可点；若无偏移，点击后会提示“无需修复”
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(true);
            
            if (offsetCount > 0) {
                e.getPresentation().setText(String.format("Fix Topic Offset (%d 行错位)", offsetCount));
            } else {
                e.getPresentation().setText("Fix Topic Offset");
            }
        } catch (Exception ex) {
            // 检测失败：显示禁用项，避免整菜单为空
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            e.getPresentation().setText("Fix Topic Offset (检测失败)");
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Topic topic = topicFetcher.apply(null);
        if (topic == null) {
            return;
        }
        
        // 使用新的 AutoFixService
        FixResult result = AutoFixService.getInstance().fixTopic(project, topic);
        
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
                        "该 Topic 下所有行已同步，无需修复",
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

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
