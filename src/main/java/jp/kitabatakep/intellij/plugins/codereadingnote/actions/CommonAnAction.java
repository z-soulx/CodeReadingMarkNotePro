package jp.kitabatakep.intellij.plugins.codereadingnote.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;

import javax.swing.*;

public abstract class CommonAnAction extends AnAction {
    public CommonAnAction(String export, String export1, Icon export2) {
        super(export,export1,export2);
    }

    public CommonAnAction() {
    }


    @Override
    public ActionUpdateThread getActionUpdateThread() {
        // 如果操作需要在事件调度线程 (Event Dispatch Thread) 上执行，使用 EDT
        return ActionUpdateThread.EDT;
        // 如果操作可以在后台线程中执行，使用 BGT
        // return ActionUpdateThread.BGT;
    }
}
