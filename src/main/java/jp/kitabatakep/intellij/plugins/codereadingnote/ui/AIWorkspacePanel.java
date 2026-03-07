package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.aiconfig.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
/**
 * Main panel for the "AI Workspace" tab, showing AI config files
 * with a tree on the left and a file preview on the right.
 */
public class AIWorkspacePanel extends JPanel {

    private final Project project;
    private final AIConfigTreePanel treePanel;
    private final JPanel previewPanel;
    private final JLabel statusLabel;
    private EditorEx currentEditor;

    public AIWorkspacePanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.treePanel = new AIConfigTreePanel(project);
        this.previewPanel = new JPanel(new BorderLayout());
        this.statusLabel = new JLabel();

        setupLayout();
        setupEventHandlers();
        updateStatus();
    }

    private void setupLayout() {
        // Toolbar
        JComponent toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);

        // Main content: split pane
        JBSplitter splitter = new JBSplitter(0.3f);
        splitter.setSplitterProportionKey(AppConstants.appName + "AIWorkspace.splitter");
        splitter.setHonorComponentsMinimumSize(false);
        splitter.setFirstComponent(treePanel);

        // Preview panel with placeholder
        showPlaceholder();
        splitter.setSecondComponent(previewPanel);

        add(splitter, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(2, 8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // Tree selection listener
        treePanel.setSelectionListener(new AIConfigTreePanel.SelectionListener() {
            @Override
            public void onFileSelected(@NotNull AIConfigEntry entry) {
                showFilePreview(entry);
            }

            @Override
            public void onDirectorySelected(@NotNull String dirPath) {
                showDirectoryInfo(dirPath);
            }

            @Override
            public void onSelectionCleared() {
                showPlaceholder();
            }
        });
    }

    @NotNull
    private JComponent createToolbar() {
        DefaultActionGroup actions = new DefaultActionGroup();

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.scan"),
            CodeReadingNoteBundle.message("aiconfig.action.scan.description"),
            AllIcons.Actions.Refresh
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AIConfigService.getInstance(project).rescan();
                treePanel.loadEntries();
                updateStatus();
            }
        });

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.add.path"),
            CodeReadingNoteBundle.message("aiconfig.action.add.path.description"),
            AllIcons.General.Add
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                addCustomPath();
            }
        });

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.create"),
            CodeReadingNoteBundle.message("aiconfig.action.create.description"),
            AllIcons.Actions.NewFolder
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                createNewAIConfig();
            }
        });

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.ignore"),
            CodeReadingNoteBundle.message("aiconfig.action.ignore.description"),
            AllIcons.Actions.Properties
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                editIgnorePatterns();
            }
        });

        actions.addSeparator();

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.tree.action.collapse.all"),
            CodeReadingNoteBundle.message("aiconfig.tree.action.collapse.all.description"),
            AllIcons.Actions.Collapseall
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treePanel.toggleExpandCollapse();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                boolean anyExpanded = treePanel.hasAnyExpanded();
                e.getPresentation().setIcon(anyExpanded ? AllIcons.Actions.Collapseall : AllIcons.Actions.Expandall);
                e.getPresentation().setText(anyExpanded
                    ? CodeReadingNoteBundle.message("aiconfig.tree.action.collapse.all")
                    : CodeReadingNoteBundle.message("aiconfig.tree.action.expand.all"));
                e.getPresentation().setDescription(anyExpanded
                    ? CodeReadingNoteBundle.message("aiconfig.tree.action.collapse.all.description")
                    : CodeReadingNoteBundle.message("aiconfig.tree.action.expand.all.description"));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        actions.addSeparator();

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.open.in.editor"),
            CodeReadingNoteBundle.message("aiconfig.action.open.in.editor.description"),
            AllIcons.Actions.EditSource
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AIConfigEntry entry = treePanel.getSelectedEntry();
                if (entry == null) return;
                openInEditor(entry);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(treePanel.getSelectedEntry() != null);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        // AI Config sync: independent manual push/pull
        actions.addSeparator();

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.sync.push"),
            CodeReadingNoteBundle.message("aiconfig.action.sync.push.description"),
            AllIcons.Actions.Upload
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                pushAIConfigs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig config =
                    jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings.getInstance().getSyncConfig();
                e.getPresentation().setEnabled(config.isEnabled());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        actions.add(new AnAction(
            CodeReadingNoteBundle.message("aiconfig.action.sync.pull"),
            CodeReadingNoteBundle.message("aiconfig.action.sync.pull.description"),
            AllIcons.Actions.Download
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                pullAIConfigs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig config =
                    jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings.getInstance().getSyncConfig();
                e.getPresentation().setEnabled(config.isEnabled());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
            AppConstants.appName + "AIWorkspace", actions, true);
        actionToolbar.setTargetComponent(this);
        actionToolbar.setReservePlaceAutoPopupIcon(false);
        actionToolbar.setMinimumButtonSize(new Dimension(20, 20));

        JComponent toolbarComponent = actionToolbar.getComponent();
        toolbarComponent.setBorder(JBUI.Borders.merge(toolbarComponent.getBorder(), JBUI.Borders.emptyLeft(8), true));
        toolbarComponent.setOpaque(false);

        return toolbarComponent;
    }

    private void showPlaceholder() {
        disposeCurrentEditor();
        previewPanel.removeAll();

        JLabel placeholder = new JLabel(CodeReadingNoteBundle.message("aiconfig.preview.placeholder"));
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.setForeground(UIManager.getColor("Label.disabledForeground"));
        previewPanel.add(placeholder, BorderLayout.CENTER);
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void showFilePreview(@NotNull AIConfigEntry entry) {
        disposeCurrentEditor();
        previewPanel.removeAll();

        String basePath = project.getBasePath();
        if (basePath == null) {
            showErrorInPreview();
            return;
        }

        File file = new File(basePath, entry.getRelativePath());
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(file.getAbsolutePath().replace('\\', '/'));

        if (vf == null || !vf.isValid()) {
            // Fallback: read content directly
            AIConfigRegistry registry = AIConfigService.getInstance(project).getRegistry();
            String content = registry.readFileContent(entry);
            if (content == null) {
                showErrorInPreview();
                return;
            }
            showContentInEditor(entry, content, true);
        } else {
            // Use VFS-backed document for editing support
            Document document = FileDocumentManager.getInstance().getDocument(vf);
            if (document == null) {
                AIConfigRegistry registry = AIConfigService.getInstance(project).getRegistry();
                String content = registry.readFileContent(entry);
                if (content == null) {
                    showErrorInPreview();
                    return;
                }
                showContentInEditor(entry, content, true);
            } else {
                showDocumentInEditor(entry, document, vf);
            }
        }

        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void showErrorInPreview() {
        previewPanel.removeAll();
        JLabel errorLabel = new JLabel(CodeReadingNoteBundle.message("aiconfig.preview.read.error"));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(errorLabel, BorderLayout.CENTER);
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void showContentInEditor(@NotNull AIConfigEntry entry, @NotNull String content, boolean readOnly) {
        JPanel headerPanel = createFileHeader(entry, readOnly);
        previewPanel.add(headerPanel, BorderLayout.NORTH);

        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(entry.getFileName());
        Document document = EditorFactory.getInstance().createDocument(content);
        if (readOnly) {
            document.setReadOnly(true);
        }
        currentEditor = (EditorEx) EditorFactory.getInstance().createEditor(document, project, fileType, readOnly);
        configureEditorSettings(currentEditor);
        previewPanel.add(currentEditor.getComponent(), BorderLayout.CENTER);
    }

    private void showDocumentInEditor(@NotNull AIConfigEntry entry, @NotNull Document document,
                                       @NotNull VirtualFile vf) {
        JPanel headerPanel = createFileHeader(entry, false);
        previewPanel.add(headerPanel, BorderLayout.NORTH);

        FileType fileType = vf.getFileType();
        currentEditor = (EditorEx) EditorFactory.getInstance().createEditor(document, project, fileType, false);
        configureEditorSettings(currentEditor);
        previewPanel.add(currentEditor.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    private JPanel createFileHeader(@NotNull AIConfigEntry entry, boolean readOnly) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.foreground"), 0, 0, 1, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JLabel pathLabel = new JLabel(entry.getRelativePath());
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.BOLD));
        leftPanel.add(pathLabel);

        JLabel typeLabel = new JLabel("[" + entry.getType().getDisplayName() + "]");
        typeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        leftPanel.add(typeLabel);

        if (readOnly) {
            JLabel roLabel = new JLabel("(read-only)");
            roLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            leftPanel.add(roLabel);
        }

        headerPanel.add(leftPanel, BorderLayout.WEST);

        // Open in editor button on the right
        JButton openBtn = new JButton(CodeReadingNoteBundle.message("aiconfig.action.open.in.editor"));
        openBtn.setFont(openBtn.getFont().deriveFont(Font.PLAIN, 11f));
        openBtn.addActionListener(e -> openInEditor(entry));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        rightPanel.add(openBtn);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private void configureEditorSettings(@NotNull EditorEx editor) {
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setGutterIconsShown(false);
        settings.setAdditionalLinesCount(2);
        settings.setAdditionalColumnsCount(2);
    }

    private void showDirectoryInfo(@NotNull String dirPath) {
        disposeCurrentEditor();
        previewPanel.removeAll();

        AIConfigRegistry registry = AIConfigService.getInstance(project).getRegistry();
        String pathPrefix = dirPath + "/";
        java.util.List<AIConfigEntry> files = new java.util.ArrayList<>();
        for (AIConfigEntry entry : registry.getEntries()) {
            if (entry.getRelativePath().startsWith(pathPrefix)) {
                files.add(entry);
            }
        }

        AIConfigType type = AIConfigType.detectType(pathPrefix);

        StringBuilder info = new StringBuilder();
        info.append("<html><body style='padding:16px;font-family:sans-serif;'>");
        info.append("<h2>").append(dirPath).append("/</h2>");
        info.append("<p><b>").append(CodeReadingNoteBundle.message("aiconfig.info.type")).append(":</b> ")
            .append(type.getDisplayName()).append("</p>");
        info.append("<p><b>").append(CodeReadingNoteBundle.message("aiconfig.info.files")).append(":</b> ")
            .append(files.size()).append("</p>");
        if (!files.isEmpty()) {
            info.append("<ul>");
            for (AIConfigEntry entry : files) {
                String tracked = entry.isTracked() ? " [sync]" : "";
                info.append("<li>").append(entry.getRelativePath().substring(pathPrefix.length()))
                    .append(tracked).append("</li>");
            }
            info.append("</ul>");
        }
        info.append("</body></html>");

        JLabel infoLabel = new JLabel(info.toString());
        infoLabel.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(infoLabel);
        scroll.setBorder(JBUI.Borders.empty());
        previewPanel.add(scroll, BorderLayout.CENTER);
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void pushAIConfigs() {
        doPushAIConfigs(false);
    }

    private void doPushAIConfigs(boolean forceAll) {
        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig config =
            jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings.getInstance().getSyncConfig();

        if (!config.isEnabled()) {
            com.intellij.openapi.ui.Messages.showWarningDialog(project,
                CodeReadingNoteBundle.message("message.sync.not.enabled"),
                CodeReadingNoteBundle.message("message.sync.not.enabled.title"));
            return;
        }
        String validationError = config.validate();
        if (validationError != null) {
            com.intellij.openapi.ui.Messages.showErrorDialog(project,
                CodeReadingNoteBundle.message("message.sync.config.error", validationError),
                CodeReadingNoteBundle.message("message.sync.config.error.title"));
            return;
        }

        AIConfigService aiService = AIConfigService.getInstance(project);
        boolean noTrackedFiles = aiService.getRegistry().getTrackedEntries().isEmpty();

        if (noTrackedFiles) {
            int confirm = com.intellij.openapi.ui.Messages.showOkCancelDialog(
                project,
                CodeReadingNoteBundle.message("aiconfig.sync.push.empty.confirm"),
                CodeReadingNoteBundle.message("aiconfig.action.sync.push"),
                CodeReadingNoteBundle.message("button.ok"),
                CodeReadingNoteBundle.message("button.cancel"),
                com.intellij.openapi.ui.Messages.getWarningIcon()
            );
            if (confirm != com.intellij.openapi.ui.Messages.OK) return;
        }

        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            new com.intellij.openapi.progress.Task.Backgroundable(
                project, CodeReadingNoteBundle.message("progress.pushing.ai.configs"), true) {

                private jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult result;

                @Override
                public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(CodeReadingNoteBundle.message("progress.pushing.ai.configs"));
                    AIConfigSyncAdapter adapter = AIConfigSyncAdapter.getInstance(project);
                    result = adapter.pushAIConfigs(config, project.getName(), forceAll);
                }

                @Override
                public void onSuccess() {
                    handlePushResult(result);
                }

                @Override
                public void onThrowable(@NotNull Throwable error) {
                    com.intellij.openapi.ui.Messages.showErrorDialog(project,
                        error.getMessage(),
                        CodeReadingNoteBundle.message("aiconfig.sync.push.failed.title"));
                }
            });
    }

    private void handlePushResult(@NotNull jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult result) {
        boolean noChanges = "ai.config.push.no.changes".equals(result.getMessage());

        jp.kitabatakep.intellij.plugins.codereadingnote.sync.FilePushReport report;
        if (noChanges) {
            report = new jp.kitabatakep.intellij.plugins.codereadingnote.sync.FilePushReport();
        } else {
            report = null;
            String reportData = result.getData();
            if (reportData != null && reportData.startsWith("{")) {
                try {
                    report = jp.kitabatakep.intellij.plugins.codereadingnote.sync.FilePushReport.fromJson(reportData);
                } catch (Exception ignored) { }
            }
            if (report == null) {
                report = new jp.kitabatakep.intellij.plugins.codereadingnote.sync.FilePushReport();
            }
        }

        PushReportDialog dialog = new PushReportDialog(project, report, result.isSuccess(), noChanges);
        dialog.show();

        if (dialog.getExitCode() == PushReportDialog.FORCE_PUSH_EXIT_CODE) {
            doPushAIConfigs(true);
        }
    }

    private void pullAIConfigs() {
        jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncConfig config =
            jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncSettings.getInstance().getSyncConfig();

        if (!config.isEnabled()) {
            com.intellij.openapi.ui.Messages.showWarningDialog(project,
                CodeReadingNoteBundle.message("message.sync.not.enabled"),
                CodeReadingNoteBundle.message("message.sync.not.enabled.title"));
            return;
        }
        String validationError = config.validate();
        if (validationError != null) {
            com.intellij.openapi.ui.Messages.showErrorDialog(project,
                CodeReadingNoteBundle.message("message.sync.config.error", validationError),
                CodeReadingNoteBundle.message("message.sync.config.error.title"));
            return;
        }

        int confirm = com.intellij.openapi.ui.Messages.showOkCancelDialog(
            project,
            CodeReadingNoteBundle.message("aiconfig.sync.pull.confirm"),
            CodeReadingNoteBundle.message("aiconfig.action.sync.pull"),
            CodeReadingNoteBundle.message("button.ok"),
            CodeReadingNoteBundle.message("button.cancel"),
            com.intellij.openapi.ui.Messages.getWarningIcon()
        );
        if (confirm != com.intellij.openapi.ui.Messages.OK) return;

        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            new com.intellij.openapi.progress.Task.Backgroundable(
                project, CodeReadingNoteBundle.message("progress.pulling.ai.configs"), true) {

                private jp.kitabatakep.intellij.plugins.codereadingnote.sync.SyncResult result;

                @Override
                public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(CodeReadingNoteBundle.message("progress.pulling.ai.configs"));
                    AIConfigSyncAdapter adapter = AIConfigSyncAdapter.getInstance(project);
                    result = adapter.pullAIConfigs(config, project.getName());
                }

                @Override
                public void onSuccess() {
                    treePanel.loadEntries();
                    updateStatus();
                    com.intellij.openapi.ui.Messages.showInfoMessage(project,
                        result.getUserMessage(),
                        result.isSuccess()
                            ? CodeReadingNoteBundle.message("aiconfig.sync.pull.success.title")
                            : CodeReadingNoteBundle.message("aiconfig.sync.pull.failed.title"));
                }

                @Override
                public void onThrowable(@NotNull Throwable error) {
                    com.intellij.openapi.ui.Messages.showErrorDialog(project,
                        error.getMessage(),
                        CodeReadingNoteBundle.message("aiconfig.sync.pull.failed.title"));
                }
            });
    }

    private void createNewAIConfig() {
        AIConfigCreateDialog dialog = new AIConfigCreateDialog(project);
        if (!dialog.showAndGet()) return;

        String basePath = project.getBasePath();
        if (basePath == null) return;

        String relativePath = dialog.getRelativePath();
        String content = dialog.getBoilerplateContent();

        try {
            File targetFile = new File(basePath, relativePath);
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.write(targetFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            AIConfigService aiService = AIConfigService.getInstance(project);
            aiService.rescan();
            treePanel.loadEntries();
            updateStatus();

            // Open the newly created file in editor
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                targetFile.getAbsolutePath().replace('\\', '/'));
            if (vf != null) {
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(vf, true);
            }
        } catch (Exception ex) {
            com.intellij.openapi.ui.Messages.showErrorDialog(project,
                ex.getMessage(),
                CodeReadingNoteBundle.message("aiconfig.create.error.title"));
        }
    }

    private void addCustomPath() {
        String input = com.intellij.openapi.ui.Messages.showInputDialog(
            project,
            CodeReadingNoteBundle.message("aiconfig.dialog.add.path.message"),
            CodeReadingNoteBundle.message("aiconfig.action.add.path"),
            null
        );
        if (input == null || input.trim().isEmpty()) return;

        String path = input.trim().replace('\\', '/');
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        String basePath = project.getBasePath();
        if (basePath == null) return;

        String absolutePath = (basePath + "/" + path).replace('\\', '/');
        VirtualFile target = LocalFileSystem.getInstance().refreshAndFindFileByPath(absolutePath);
        if (target == null || !target.isValid()) {
            com.intellij.openapi.ui.Messages.showWarningDialog(project,
                CodeReadingNoteBundle.message("aiconfig.dialog.add.path.not.found", path),
                CodeReadingNoteBundle.message("aiconfig.action.add.path"));
            return;
        }

        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();
        int beforeCount = registry.size();
        registry.addCustomPath(path);
        aiService.rescan();
        treePanel.loadEntries();
        updateStatus();

        int added = registry.size() - beforeCount;
        if (added > 0) {
            com.intellij.openapi.ui.Messages.showInfoMessage(project,
                CodeReadingNoteBundle.message("aiconfig.dialog.add.path.success", added, path),
                CodeReadingNoteBundle.message("aiconfig.action.add.path"));
        } else {
            com.intellij.openapi.ui.Messages.showInfoMessage(project,
                CodeReadingNoteBundle.message("aiconfig.dialog.add.path.no.files", path),
                CodeReadingNoteBundle.message("aiconfig.action.add.path"));
        }
    }

    private void editIgnorePatterns() {
        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();

        java.util.List<String> builtins = registry.getBuiltinIgnorePatterns();
        java.util.List<String> userPatterns = new java.util.ArrayList<>(registry.getUserIgnorePatterns());

        StringBuilder header = new StringBuilder();
        header.append(CodeReadingNoteBundle.message("aiconfig.ignore.builtin.label")).append("\n");
        for (String p : builtins) {
            header.append("  ").append(p).append("\n");
        }
        header.append("\n").append(CodeReadingNoteBundle.message("aiconfig.ignore.user.hint"));

        String currentValue = String.join("\n", userPatterns);

        String input = com.intellij.openapi.ui.Messages.showMultilineInputDialog(
            project,
            header.toString(),
            CodeReadingNoteBundle.message("aiconfig.action.ignore"),
            currentValue,
            null,
            null
        );
        if (input == null) return;

        java.util.List<String> newPatterns = new java.util.ArrayList<>();
        for (String line : input.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                newPatterns.add(trimmed);
            }
        }

        registry.setUserIgnorePatterns(newPatterns);
        aiService.rescan();
        treePanel.loadEntries();
        updateStatus();
    }

    private void openInEditor(@NotNull AIConfigEntry entry) {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        File file = new File(basePath, entry.getRelativePath());
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(file.getAbsolutePath().replace('\\', '/'));
        if (vf != null) {
            com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }

    private void disposeCurrentEditor() {
        if (currentEditor != null && !currentEditor.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(currentEditor);
            currentEditor = null;
        }
    }

    private void updateStatus() {
        AIConfigService aiService = AIConfigService.getInstance(project);
        AIConfigRegistry registry = aiService.getRegistry();
        int total = registry.size();
        long tracked = registry.getTrackedEntries().size();

        statusLabel.setText(CodeReadingNoteBundle.message("aiconfig.status.summary", total, tracked));
    }

    private void setupEventHandlers() {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(AIConfigNotifier.AI_CONFIG_TOPIC, new AIConfigNotifier() {
            @Override
            public void registryUpdated() {
                SwingUtilities.invokeLater(() -> {
                    treePanel.loadEntries();
                    updateStatus();
                });
            }

            @Override
            public void fileChanged(AIConfigEntry entry) {
                SwingUtilities.invokeLater(() -> {
                    // If the changed file is currently previewed, refresh
                    AIConfigEntry selected = treePanel.getSelectedEntry();
                    if (selected != null && selected.getRelativePath().equals(entry.getRelativePath())) {
                        showFilePreview(entry);
                    }
                    updateStatus();
                });
            }
        });
    }

    public void dispose() {
        disposeCurrentEditor();
    }
}
