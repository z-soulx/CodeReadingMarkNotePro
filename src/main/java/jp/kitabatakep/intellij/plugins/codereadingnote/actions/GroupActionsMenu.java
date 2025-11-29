package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * 分组操作菜单（下拉菜单）
 * 整合所有分组相关的操作：新建、重命名、删除分组、移动TopicLine到分组
 */
public class GroupActionsMenu extends ActionGroup {
    
    private final Supplier<Topic> topicSupplier;
    private final Supplier<TopicGroup> groupSupplier;
    private final Supplier<TopicLine> lineSupplier;
    
    public GroupActionsMenu(Supplier<Topic> topicSupplier, 
                           Supplier<TopicGroup> groupSupplier,
                           Supplier<TopicLine> lineSupplier) {
        super(CodeReadingNoteBundle.message("action.group.menu"), 
              CodeReadingNoteBundle.message("action.group.menu.description"), 
              AllIcons.Actions.GroupBy);
        this.topicSupplier = topicSupplier;
        this.groupSupplier = groupSupplier;
        this.lineSupplier = lineSupplier;
        setPopup(true);
    }
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
    
    @NotNull
    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        return new AnAction[] {
            new GroupAddAction(topicSupplier),
            new GroupRenameAction(groupSupplier),
            new GroupRemoveAction(groupSupplier),
            new LineToGroupMoveAction(lineSupplier)
        };
    }
}

