package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.ui.TopicTreePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.util.function.Supplier;

/**
 * Action to toggle between collapse all and expand all for topics, groups, and ungrouped folders
 * 一键切换收缩/展开所有Topic、分组和未分组文件夹的操作
 */
public class CollapseAllAction extends CommonAnAction {
    
    private final Supplier<TopicTreePanel> topicTreePanelSupplier;

    public CollapseAllAction(Supplier<TopicTreePanel> topicTreePanelSupplier) {
        super(
            CodeReadingNoteBundle.message("action.toggle.collapse"),
            CodeReadingNoteBundle.message("action.toggle.collapse.description"),
            AllIcons.Actions.Collapseall
        );
        this.topicTreePanelSupplier = topicTreePanelSupplier;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        TopicTreePanel treePanel = topicTreePanelSupplier.get();
        if (treePanel == null) return;
        
        // 确保在EDT线程中执行UI操作
        SwingUtilities.invokeLater(() -> {
            // 检查当前状态：如果所有节点都收缩了，则展开；否则收缩
            if (treePanel.areAllNodesCollapsed()) {
                // 所有节点都收缩了，执行展开操作
                treePanel.expandAllGroups();
            } else {
                // 有节点是展开的，执行收缩操作
                treePanel.collapseAllGroups();
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        TopicTreePanel treePanel = topicTreePanelSupplier.get();
        
        // 只有在有项目且有tree panel时才启用
        boolean enabled = project != null && treePanel != null;
        e.getPresentation().setEnabled(enabled);
        
        // 动态更新按钮文本和图标，根据当前状态显示下一步操作
        if (enabled && treePanel.areAllNodesCollapsed()) {
            e.getPresentation().setText(CodeReadingNoteBundle.message("action.expand.all"));
            e.getPresentation().setDescription(CodeReadingNoteBundle.message("action.expand.all.description"));
            e.getPresentation().setIcon(AllIcons.Actions.Expandall);
        } else {
            e.getPresentation().setText(CodeReadingNoteBundle.message("action.collapse.all"));
            e.getPresentation().setDescription(CodeReadingNoteBundle.message("action.collapse.all.description"));
            e.getPresentation().setIcon(AllIcons.Actions.Collapseall);
        }
    }
}
