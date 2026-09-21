package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigAutoSyncScheduler;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.AIConfigSyncAdapter;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Modal dialog for AI config sync conflicts.
 * Modeled after SyncConflictDialog — provides Pull / Force Push / Cancel actions.
 */
public class AIConfigSyncConflictDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(AIConfigSyncConflictDialog.class);

    private final Project project;
    private Resolution selectedResolution = Resolution.CANCEL;

    public enum Resolution { PULL, PUSH, CANCEL }

    private AIConfigSyncConflictDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        setTitle(CodeReadingNoteBundle.message("aiconfig.autosync.conflict.dialog.title"));
        init();
    }

    public static void show(@NotNull Project project) {
        AIConfigSyncConflictDialog dialog = new AIConfigSyncConflictDialog(project);
        if (dialog.showAndGet()) {
            handleResolution(project, dialog.selectedResolution);
        } else {
            handleResolution(project, Resolution.CANCEL);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(JBUI.Borders.empty(10));

        JBLabel messageLabel = new JBLabel(
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.dialog.message"));
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
        panel.add(messageLabel, BorderLayout.NORTH);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(JBUI.Borders.empty(8, 0));

        detailsPanel.add(new JBLabel("📊 " +
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.dialog.remote.hint")));
        detailsPanel.add(Box.createVerticalStrut(6));
        detailsPanel.add(new JBLabel("💻 " +
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.dialog.local.hint")));

        panel.add(detailsPanel, BorderLayout.CENTER);

        JPanel warningPanel = new JPanel(new BorderLayout(0, 4));
        warningPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));

        JBLabel warningLabel = new JBLabel("⚠️ " +
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.dialog.warning"));
        warningLabel.setForeground(new Color(255, 140, 0));
        warningPanel.add(warningLabel, BorderLayout.NORTH);

        JBLabel hintLabel = new JBLabel(
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.dialog.cancel.hint"));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1.0f));
        hintLabel.setForeground(JBColor.GRAY);
        warningPanel.add(hintLabel, BorderLayout.CENTER);

        panel.add(warningPanel, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(520, 200));
        return panel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        Action pullAction = new DialogWrapperAction(
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.action.pull")) {
            @Override
            protected void doAction(ActionEvent e) {
                selectedResolution = Resolution.PULL;
                close(OK_EXIT_CODE);
            }
        };

        Action pushAction = new DialogWrapperAction(
                CodeReadingNoteBundle.message("aiconfig.autosync.conflict.action.push")) {
            @Override
            protected void doAction(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        getContentPane(),
                        CodeReadingNoteBundle.message("aiconfig.autosync.conflict.push.confirm"),
                        CodeReadingNoteBundle.message("aiconfig.autosync.conflict.push.confirm.title"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    selectedResolution = Resolution.PUSH;
                    close(OK_EXIT_CODE);
                }
            }
        };

        return new Action[]{pullAction, pushAction, getCancelAction()};
    }

    private static void handleResolution(@NotNull Project project, @NotNull Resolution resolution) {
        AIConfigAutoSyncScheduler scheduler = AIConfigAutoSyncScheduler.getInstance(project);

        switch (resolution) {
            case PULL:
                executePull(project, scheduler);
                break;
            case PUSH:
                executeForcePush(project, scheduler);
                break;
            case CANCEL:
                break;
        }
    }

    private static void executePull(@NotNull Project project, @NotNull AIConfigAutoSyncScheduler scheduler) {
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        AIConfigSyncAdapter adapter = AIConfigSyncAdapter.getInstance(project);

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project, CodeReadingNoteBundle.message("aiconfig.autosync.conflict.pulling"), false) {

            private SyncResult result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                result = adapter.pullAIConfigs(config, project.getName());
            }

            @Override
            public void onSuccess() {
                if (result != null && result.isSuccess()) {
                    scheduler.resumeAutoSync();
                    Messages.showInfoMessage(project,
                            CodeReadingNoteBundle.message("aiconfig.autosync.conflict.resolved.pull"),
                            CodeReadingNoteBundle.message("aiconfig.autosync.conflict.resolved.title"));
                } else {
                    String msg = result != null ? result.getMessage() : "Unknown error";
                    Messages.showErrorDialog(project, msg,
                            CodeReadingNoteBundle.message("aiconfig.sync.pull.failed.title"));
                }
            }
        });
    }

    private static void executeForcePush(@NotNull Project project, @NotNull AIConfigAutoSyncScheduler scheduler) {
        SyncConfig config = SyncSettings.getInstance().getSyncConfig();
        AIConfigSyncAdapter adapter = AIConfigSyncAdapter.getInstance(project);

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project, CodeReadingNoteBundle.message("aiconfig.autosync.conflict.pushing"), false) {

            private SyncResult result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                result = adapter.pushAIConfigs(config, project.getName(), true);
            }

            @Override
            public void onSuccess() {
                if (result != null && result.isSuccess()) {
                    scheduler.resumeAutoSync();
                    Messages.showInfoMessage(project,
                            CodeReadingNoteBundle.message("aiconfig.autosync.conflict.resolved.push"),
                            CodeReadingNoteBundle.message("aiconfig.autosync.conflict.resolved.title"));
                } else {
                    String msg = result != null ? result.getMessage() : "Unknown error";
                    scheduler.pauseAutoSync();
                    Messages.showErrorDialog(project, msg,
                            CodeReadingNoteBundle.message("aiconfig.sync.push.failed.title"));
                }
            }
        });
    }
}
