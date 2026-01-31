package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteService;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Move to 菜单，支持批量移动 TopicLine 到指定 Topic/Group
 * 菜单结构: Move to > Topic > [Groups..., Ungrouped]
 */
public class TopicLineMoveToGroupAction extends ActionGroup
{
    private final List<TopicLine> topicLines;

    /**
     * 单个 TopicLine 移动
     */
    public TopicLineMoveToGroupAction(TopicLine topicLine)
    {
        this(Collections.singletonList(topicLine));
    }
    
    /**
     * 批量 TopicLine 移动
     */
    public TopicLineMoveToGroupAction(List<TopicLine> topicLines)
    {
        super(CodeReadingNoteBundle.message("action.move.to"), true);
        this.topicLines = topicLines;
    }
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null && !topicLines.isEmpty());
        // 显示移动数量
        if (topicLines.size() > 1) {
            e.getPresentation().setText(CodeReadingNoteBundle.message("action.batch.move.to", topicLines.size()));
        } else {
            e.getPresentation().setText(CodeReadingNoteBundle.message("action.move.to"));
        }
    }

    @NotNull
    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        if (e.getProject() == null) {
            return AnAction.EMPTY_ARRAY;
        }
        
        CodeReadingNoteService service = CodeReadingNoteService.getInstance(e.getProject());
        Iterator<Topic> iterator = service.getTopicList().iterator();

        ArrayList<AnAction> actions = new ArrayList<>();
        while (iterator.hasNext()) {
            Topic topic = iterator.next();
            // 每个 Topic 是一个子菜单，包含其 Groups
            actions.add(new TopicSubMenu(topicLines, topic));
        }
        return actions.toArray(new AnAction[0]);
    }
    
    /**
     * Topic 子菜单，显示 Groups 和 Ungrouped 选项
     */
    private static class TopicSubMenu extends ActionGroup {
        private final List<TopicLine> topicLines;
        private final Topic topic;
        
        public TopicSubMenu(List<TopicLine> topicLines, Topic topic) {
            super(topic.name(), true);
            this.topicLines = topicLines;
            this.topic = topic;
        }
        
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
        
        @NotNull
        @Override
        public AnAction[] getChildren(AnActionEvent e) {
            ArrayList<AnAction> actions = new ArrayList<>();
            
            // 添加各个 Group
            for (TopicGroup group : topic.getGroups()) {
                actions.add(new MoveToGroupAction(topicLines, topic, group));
            }
            
            // 添加分隔符（如果有 Groups）
            if (!topic.getGroups().isEmpty()) {
                actions.add(new com.intellij.openapi.actionSystem.Separator());
            }
            
            // 添加 Ungrouped 选项
            actions.add(new MoveToUngroupedAction(topicLines, topic));
            
            return actions.toArray(new AnAction[0]);
        }
    }
    
    /**
     * 移动到指定 Group
     */
    private static class MoveToGroupAction extends AnAction {
        private final List<TopicLine> topicLines;
        private final Topic targetTopic;
        private final TopicGroup targetGroup;
        
        public MoveToGroupAction(List<TopicLine> topicLines, Topic targetTopic, TopicGroup targetGroup) {
            super(targetGroup.name());
            this.topicLines = topicLines;
            this.targetTopic = targetTopic;
            this.targetGroup = targetGroup;
        }
        
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            for (TopicLine line : topicLines) {
                // 从原位置移除
                line.topic().removeLine(line);
                
                // 创建新的 TopicLine 并添加到目标 Group
                TopicLine newLine = TopicLine.createByAction(
                    e.getProject(),
                    targetTopic,
                    line.file(),
                    line.line(),
                    line.note()
                );
                targetTopic.addLineToGroup(newLine, targetGroup.name());
            }
        }
    }
    
    /**
     * 移动到 Ungrouped
     */
    private static class MoveToUngroupedAction extends AnAction {
        private final List<TopicLine> topicLines;
        private final Topic targetTopic;
        
        public MoveToUngroupedAction(List<TopicLine> topicLines, Topic targetTopic) {
            super(CodeReadingNoteBundle.message("action.move.to.ungrouped"));
            this.topicLines = topicLines;
            this.targetTopic = targetTopic;
        }
        
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            for (TopicLine line : topicLines) {
                // 从原位置移除
                line.topic().removeLine(line);
                
                // 创建新的 TopicLine 并添加到 Ungrouped
                TopicLine newLine = TopicLine.createByAction(
                    e.getProject(),
                    targetTopic,
                    line.file(),
                    line.line(),
                    line.note()
                );
                targetTopic.addLine(newLine);
            }
        }
    }
}
