package jp.kitabatakep.intellij.plugins.codereadingnote.sync.workspace;

import com.intellij.openapi.application.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.sync.github.GitHubSyncConfig;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public final class NotesSyncUi {
    private NotesSyncUi() {}
    static String m(String key, Object... args) { return CodeReadingNoteBundle.message("notes.sync." + key, args); }
    private static JLabel label(String key, Object... args) {
        JLabel label = new JLabel(m(key, args)); label.putClientProperty("notes.sync.key", key); label.putClientProperty("notes.sync.args", args); return label;
    }
    private static void refreshLabels(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel l && l.getClientProperty("notes.sync.key") instanceof String key)
                l.setText(m(key, (Object[]) l.getClientProperty("notes.sync.args")));
            if (c instanceof Container child) refreshLabels(child);
        }
    }
    static void later(Project p, Runnable r) { ApplicationManager.getApplication().invokeLater(() -> { if (!p.isDisposed()) r.run(); }, ModalityState.any()); }
    static String name(Project p, TopicList l) { return WorkspaceNotesService.getInstance(p).displayName(l.context()); }
    public static void open(Project project) { new Overview(project).show(); }
    public static void run(Project project, TopicList list, WorkspaceNotesSyncCoordinator.Operation op) {
        if (list == null) { open(project); return; }
        WorkspaceNotesSyncCoordinator service = WorkspaceNotesSyncCoordinator.getInstance();
        service.submit(project, list, op, null).thenAccept(result -> later(project, () -> {
            if ("unbound".equals(result.status())) { configure(project, list, () -> run(project, list, op)); return; }
            present(project, list, result);
        }));
    }
    private static void present(Project project, TopicList list, WorkspaceNotesSyncCoordinator.Result result) {
        if (result.review() != null && Set.of("conflict", "first", "missing", "remote", "pending", "stale").contains(result.status())) {
            new ReviewDialog(project, list, result).show();
        } else if (!result.success()) Messages.showErrorDialog(project,
                m("result", name(project, list), WorkspaceNotesSyncCoordinator.text(result.status())), m("title"));
        else Messages.showInfoMessage(project, m("result", name(project, list), WorkspaceNotesSyncCoordinator.text(result.status())), m("title"));
    }
    public static void configure(Project project, TopicList list, Runnable after) {
        configure(project, list, null, after);
    }
    private static void configure(Project project, TopicList list, NotesSyncBinding.Policy suggested, Runnable after) {
        SyncConfig raw = SyncSettings.getInstance().getSyncConfig();
        if (!(raw instanceof GitHubSyncConfig config) || !config.isEnabled() || config.validate() != null) {
            Messages.showErrorDialog(project, WorkspaceNotesSyncCoordinator.text("config"), m("title")); return;
        }
        WorkspaceNotesSyncCoordinator service = WorkspaceNotesSyncCoordinator.getInstance();
        service.inspect(project, list).thenAccept(view -> later(project, () -> {
            if ("storage".equals(view.status())) { Messages.showErrorDialog(project, WorkspaceNotesSyncCoordinator.text("storage"), m("title")); return; }
            new BindingDialog(project, list, config.clone(), view.binding(), suggested, after).show();
        }));
    }
    private static JComboBox<NotesSyncBinding.Policy> policies() {
        JComboBox<NotesSyncBinding.Policy> box = new JComboBox<>(NotesSyncBinding.Policy.values());
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? "" : m("policy." + value), index, selected, focus);
            }
        }); return box;
    }
    private static final class BindingDialog extends DialogWrapper {
        private final Project project;
        private final TopicList list;
        private final GitHubSyncConfig config;
        private final Runnable after;
        private final JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        private final JComboBox<String> remote = new JComboBox<>();
        private final JComboBox<NotesSyncBinding.Policy> policy = policies();
        private final JSpinner interval = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        private final JCheckBox shared = new JCheckBox(m("allow.shared"));
        private final JLabel notice = label("binding.help");
        BindingDialog(Project project, TopicList list, GitHubSyncConfig config, NotesSyncBinding old, NotesSyncBinding.Policy suggested, Runnable after) {
            super(project); this.project = project; this.list = list; this.config = config; this.after = after;
            setTitle(m("settings.for", name(project, list)));
            remote.setEditable(true);
            String localName = java.nio.file.Path.of(name(project, list)).getFileName().toString();
            String candidate = old == null ? NotesSyncBinding.legacyId(localName) : old.remoteId();
            if (old == null && list.context().root().equals(jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceDiscovery.identity(java.nio.file.Path.of(project.getBasePath())))) candidate = NotesSyncBinding.legacyId(project.getName());
            remote.addItem(candidate); remote.setSelectedItem(candidate);
            boolean root = list.context().root().equals(jp.kitabatakep.intellij.plugins.codereadingnote.notesworkspace.WorkspaceDiscovery.identity(java.nio.file.Path.of(project.getBasePath())));
            policy.setSelectedItem(old != null ? old.policy() : root && config.isAutoSync() ? NotesSyncBinding.Policy.PUSH : NotesSyncBinding.Policy.MANUAL);
            if (suggested != null) policy.setSelectedItem(suggested);
            interval.setValue(old == null ? 5 : old.intervalMinutes());
            form.add(label("destination", config.getRepository(), config.getBranch(), config.getBasePath()));
            form.add(label("remote.id")); form.add(remote); form.add(notice);
            form.add(label("policy")); form.add(policy); form.add(label("interval")); form.add(interval); form.add(shared);
            init(); setOKButtonText(m("save"));
            ApplicationManager.getApplication().getMessageBus().connect(getDisposable()).subscribe(
                    jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings.LANGUAGE_CHANGED, () -> {
                        setTitle(m("settings.for", name(project, list))); setOKButtonText(m("save"));
                        shared.setText(m("allow.shared")); refreshLabels(form); policy.repaint();
                    });
            WorkspaceNotesSyncCoordinator.getInstance().remoteProjects().whenComplete((items, error) -> later(project, () -> {
                if (isDisposed()) return;
                if (error != null) { notice.putClientProperty("notes.sync.key", "directory.failed"); notice.setText(m("directory.failed")); return; }
                Object selected = remote.getSelectedItem();
                for (String item : items) if (!Objects.equals(item, selected)) remote.addItem(item);
                remote.setSelectedItem(selected);
            }));
        }
        @Override protected JComponent createCenterPanel() { form.setPreferredSize(new Dimension(560, 320)); return form; }
        @Override protected void doOKAction() {
            try {
                NotesSyncBinding binding = NotesSyncBinding.create(config, String.valueOf(remote.getEditor().getItem()).trim(),
                        (NotesSyncBinding.Policy) policy.getSelectedItem(), (Integer) interval.getValue());
                setOKActionEnabled(false);
                WorkspaceNotesSyncCoordinator.getInstance().bind(project, list, binding, shared.isSelected()).thenAccept(result -> later(project, () -> {
                    if (isDisposed()) return;
                    setOKActionEnabled(true);
                    if (!Set.of("first", "pending").contains(result.status())) { setErrorText(WorkspaceNotesSyncCoordinator.text(result.status())); return; }
                    close(OK_EXIT_CODE); if (after != null) after.run();
                }));
            } catch (IllegalArgumentException invalid) { setErrorText(m("binding.invalid")); }
        }
    }
    private static final class ReviewDialog extends DialogWrapper {
        private final Project project;
        private final TopicList list;
        private final WorkspaceNotesSyncCoordinator.Result result;
        private JPanel content;
        private Action localAction, remoteAction;
        private JLabel help;
        ReviewDialog(Project p, TopicList list, WorkspaceNotesSyncCoordinator.Result result) {
            super(p); this.project = p; this.list = list; this.result = result;
            setTitle(m("review.for", name(p, list))); init();
            ApplicationManager.getApplication().getMessageBus().connect(getDisposable()).subscribe(
                    jp.kitabatakep.intellij.plugins.codereadingnote.settings.LanguageSettings.LANGUAGE_CHANGED, () -> {
                        setTitle(m("review.for", name(p, list))); refreshLabels(content);
                        help.setText(m("review.help", WorkspaceNotesSyncCoordinator.text(result.status())));
                        localAction.putValue(Action.NAME, m("use.local")); remoteAction.putValue(Action.NAME, m("use.remote"));
                    });
        }
        @Override protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8)); content = panel;
            help = new JLabel(m("review.help", WorkspaceNotesSyncCoordinator.text(result.status())));
            panel.add(help, BorderLayout.NORTH);
            JTextArea local = new JTextArea(NotesSyncXml.write(result.review().local().topics())); local.setEditable(false);
            JTextArea remote = new JTextArea(result.review().remote().exists() ? result.review().remote().xml() : m("remote.absent")); remote.setEditable(false);
            JPanel left = new JPanel(new BorderLayout()); left.add(label("local"), BorderLayout.NORTH); left.add(new JScrollPane(local));
            JPanel right = new JPanel(new BorderLayout()); right.add(label("remote"), BorderLayout.NORTH); right.add(new JScrollPane(remote));
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right); split.setResizeWeight(.5);
            panel.add(split); panel.setPreferredSize(new Dimension(850, 450)); return panel;
        }
        @Override protected Action[] createActions() {
            Action local = new AbstractAction(m("use.local")) { @Override public void actionPerformed(ActionEvent e) { resolve(WorkspaceNotesSyncCoordinator.Operation.USE_LOCAL); } };
            Action remote = new AbstractAction(m("use.remote")) { @Override public void actionPerformed(ActionEvent e) { resolve(WorkspaceNotesSyncCoordinator.Operation.USE_REMOTE); } };
            remote.setEnabled(result.review().remote().exists());
            localAction = local; remoteAction = remote;
            return new Action[]{local, remote, getCancelAction()};
        }
        private void resolve(WorkspaceNotesSyncCoordinator.Operation op) {
            close(OK_EXIT_CODE);
            WorkspaceNotesSyncCoordinator.getInstance().submit(project, list, op, result.review()).thenAccept(r -> later(project, () -> present(project, list, r)));
        }
    }
    private static final class Overview extends DialogWrapper {
        private final Project project;
        private final WorkspaceNotesSyncCoordinator service;
        private final Set<TopicList> checked = new HashSet<>();
        private List<TopicList> rows = List.of();
        private final Map<TopicList, WorkspaceNotesSyncCoordinator.Operation> failed = new HashMap<>();
        private final JTable table;
        private final AbstractTableModel model;
        private final JTextField search = new JTextField();
        private final JLabel selectionCount = new JLabel();
        private final JComboBox<String> filter = new JComboBox<>(new String[]{"all", "conflict", "first", "unbound", "pending", "remote", "synced", "storage", "network"});
        private final JPanel panel = new JPanel(new BorderLayout(8, 8));
        private final List<JButton> buttons = new ArrayList<>();
        private boolean busy;
        private final javax.swing.Timer timer;
        Overview(Project p) {
            super(p); project = p; service = WorkspaceNotesSyncCoordinator.getInstance(); service.attach(p);
            rows = WorkspaceNotesService.getInstance(p).projects();
            model = new AbstractTableModel() {
                @Override public int getRowCount() { return rows.size(); }
                @Override public int getColumnCount() { return 7; }
                @Override public String getColumnName(int column) { return m(new String[]{"select", "project", "remote.id", "policy", "status", "checked", "synced"}[column]); }
                @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
                @Override public boolean isCellEditable(int row, int column) { return column == 0 && !busy; }
                @Override public void setValueAt(Object value, int row, int column) { if (Boolean.TRUE.equals(value)) checked.add(rows.get(row)); else checked.remove(rows.get(row)); fireTableCellUpdated(row, column); }
                @Override public Object getValueAt(int row, int column) {
                    TopicList list = rows.get(row); var view = service.view(list);
                    return switch (column) {
                        case 0 -> checked.contains(list); case 1 -> name(p, list);
                        case 2 -> view.binding() == null ? "" : view.binding().remoteId();
                        case 3 -> view.binding() == null ? m("policy.MANUAL") : m("policy." + view.binding().policy());
                        case 4 -> WorkspaceNotesSyncCoordinator.text(view.status());
                        case 5 -> date(view.checked()); default -> date(view.synced());
                    };
                }
            };
            table = new JTable(model); table.setAutoCreateRowSorter(true);
            JPanel top = new JPanel(new BorderLayout(8, 8)); top.add(search); top.add(filter, BorderLayout.EAST);
            top.add(selectionCount, BorderLayout.WEST);
            search.setToolTipText(m("search"));
            filter.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                    return super.getListCellRendererComponent(l, "all".equals(v) ? m("all") : WorkspaceNotesSyncCoordinator.text(String.valueOf(v)), i, s, f);
                }
            });
            panel.add(top, BorderLayout.NORTH); panel.add(new JScrollPane(table));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT)); panel.add(actions, BorderLayout.SOUTH);
            button(actions, "select.all", () -> { checked.addAll(rows); model.fireTableDataChanged(); });
            button(actions, "push", () -> batch(WorkspaceNotesSyncCoordinator.Operation.PUSH, selected(), 0));
            button(actions, "pull", () -> batch(WorkspaceNotesSyncCoordinator.Operation.PULL, selected(), 0));
            button(actions, "check", () -> batch(WorkspaceNotesSyncCoordinator.Operation.CHECK, selected(), 0));
            button(actions, "settings", () -> configureNext(selected(), 0));
            button(actions, "policy", this::bulkPolicy);
            button(actions, "resolve", () -> { List<TopicList> targets = selected(); if (targets.size() == 1) run(p, targets.get(0), WorkspaceNotesSyncCoordinator.Operation.CHECK); });
            button(actions, "retry", () -> {
                List<Map.Entry<TopicList, WorkspaceNotesSyncCoordinator.Operation>> retry = List.copyOf(failed.entrySet());
                for (var entry : retry) service.submit(p, entry.getKey(), entry.getValue(), null).thenAccept(r -> later(p, () -> { if (r.success()) failed.remove(entry.getKey()); }));
            });
            panel.setPreferredSize(new Dimension(1080, 440)); setTitle(m("title")); init();
            timer = new javax.swing.Timer(1000, e -> refresh()); timer.start();
        }
        private static String date(long value) { return value == 0 ? "" : java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(new Date(value)); }
        private void button(JPanel p, String key, Runnable action) { JButton b = new JButton(m(key)); b.setName(key); b.addActionListener(e -> action.run()); buttons.add(b); p.add(b); }
        private List<TopicList> selected() {
            List<TopicList> result = rows.stream().filter(checked::contains).toList();
            if (result.isEmpty() && table.getSelectedRow() >= 0) return List.of(rows.get(table.convertRowIndexToModel(table.getSelectedRow())));
            if (result.isEmpty()) setErrorText(m("select.required")); else setErrorText(null);
            return result;
        }
        private void refresh() {
            if (isDisposed()) return;
            TopicList focused = table.getSelectedRow() >= 0 ? rows.get(table.convertRowIndexToModel(table.getSelectedRow())) : null;
            rows = WorkspaceNotesService.getInstance(project).projects(); checked.retainAll(rows);
            model.fireTableDataChanged();
            @SuppressWarnings("unchecked") TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            String query = search.getText().toLowerCase(Locale.ROOT), state = String.valueOf(filter.getSelectedItem());
            sorter.setRowFilter(new RowFilter<>() { @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                TopicList list = rows.get(entry.getIdentifier());
                return name(project, list).toLowerCase(Locale.ROOT).contains(query) && (state.equals("all") || service.view(list).status().equals(state));
            }});
            int modelRow = focused == null ? -1 : rows.indexOf(focused);
            int viewRow = modelRow < 0 ? -1 : table.convertRowIndexToView(modelRow);
            if (viewRow >= 0) table.setRowSelectionInterval(viewRow, viewRow);
            selectionCount.setText(m("selected.count", checked.size()));
            setTitle(m("title")); for (JButton b : buttons) { b.setText(m(b.getName())); b.setEnabled(!busy); }
            for (int i = 0; i < model.getColumnCount(); i++) table.getColumnModel().getColumn(i).setHeaderValue(model.getColumnName(i));
            table.getTableHeader().repaint(); filter.repaint();
        }
        private void configureNext(List<TopicList> lists, int i) {
            if (i < lists.size()) configure(project, lists.get(i), () -> configureNext(lists, i + 1));
        }
        private void bulkPolicy() {
            JComboBox<NotesSyncBinding.Policy> policy = policies();
            if (JOptionPane.showConfirmDialog(panel, policy, m("policy"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            NotesSyncBinding.Policy choice = (NotesSyncBinding.Policy) policy.getSelectedItem();
            for (TopicList list : selected()) {
                var binding = service.view(list).binding();
                if (binding == null) { configure(project, list, choice, null); continue; }
                service.bind(project, list, new NotesSyncBinding(binding.repository(), binding.branch(), binding.basePath(), binding.remoteId(), choice, binding.intervalMinutes()), true)
                        .thenAccept(r -> later(project, () -> { if (!Set.of("first", "pending").contains(r.status())) present(project, list, r); }));
            }
        }
        private void batch(WorkspaceNotesSyncCoordinator.Operation op, List<TopicList> targets, int index) {
            if (isDisposed() || index >= targets.size()) { busy = false; return; }
            busy = true; TopicList list = targets.get(index);
            service.submit(project, list, op, null).thenAccept(result -> later(project, () -> {
                if (!result.success()) failed.put(list, op); else failed.remove(list);
                batch(op, targets, index + 1);
            }));
        }
        @Override protected Action[] createActions() { return new Action[]{getCancelAction()}; }
        @Override protected JComponent createCenterPanel() { return panel; }
        @Override protected void dispose() { if (timer != null) timer.stop(); super.dispose(); }
    }
}
