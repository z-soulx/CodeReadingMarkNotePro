package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.CodeReadingNoteBundle;
import jp.kitabatakep.intellij.plugins.codereadingnote.workspace.AIWorkspaceCommand;
import jp.kitabatakep.intellij.plugins.codereadingnote.workspace.AIWorkspaceCommandService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Command catalog with one reusable edit form. Run uses focused or silent (unfocused) IDEA Terminal. */
public final class AIWorkspaceCommandsDialog extends DialogWrapper {
    private final Project project; private final List<AIWorkspaceCommand> commands;
    private DefaultListModel<AIWorkspaceCommand> model; private JList<AIWorkspaceCommand> list;
    private JTextField id, name, executable, args, workdir; private JCheckBox enabled; private JComboBox<String> mode, fileSource; private AIWorkspaceCommand editing;
    public AIWorkspaceCommandsDialog(@NotNull Project project) {
        super(project, true);
        this.project = project;
        try { commands = new ArrayList<>(AIWorkspaceCommandService.getInstance(project).load()); }
        catch (Exception e) { throw new RuntimeException(e); }
        setTitle(CodeReadingNoteBundle.message("aiworkspace.commands.manage"));
        setModal(false);
        init();
    }
    @Override protected Action @NotNull [] createActions() { return new Action[0]; }
    @Override protected @Nullable JComponent createSouthPanel() { return null; }
    @Override protected @Nullable JComponent createCenterPanel() {
        JPanel root=new JPanel(new BorderLayout(10,8));
        model=new DefaultListModel<>(); commands.forEach(model::addElement); list=new JList<>(model); list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setCellRenderer(new DefaultListCellRenderer(){ public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){ super.getListCellRendererComponent(l,v,i,s,f); AIWorkspaceCommand c=(AIWorkspaceCommand)v; setText((c.enabled?"":"[disabled] ")+display(c)+" ("+c.id+")"); return this; }}); list.addListSelectionListener(e->{if(!e.getValueIsAdjusting()) load(list.getSelectedValue());}); JScrollPane listScroll=new JScrollPane(list); listScroll.setPreferredSize(new Dimension(300,0)); root.add(listScroll,BorderLayout.WEST);
        JPanel form=new JPanel(new GridBagLayout()); form.setBorder(JBUI.Borders.empty(4)); id=new JTextField(24); name=new JTextField(24); executable=new JTextField(24); args=new JTextField(24); workdir=new JTextField(24); enabled=new JCheckBox(CodeReadingNoteBundle.message("aiworkspace.commands.enabled"),true); mode=new JComboBox<>(new String[]{CodeReadingNoteBundle.message("aiworkspace.commands.mode.terminal"),CodeReadingNoteBundle.message("aiworkspace.commands.mode.silent")}); fileSource=new JComboBox<>(new String[]{CodeReadingNoteBundle.message("aiworkspace.commands.file.source.project"),CodeReadingNoteBundle.message("aiworkspace.commands.file.source.editor")});
        row(form,0,"aiworkspace.commands.id",id); row(form,1,"aiworkspace.commands.name",name); row(form,2,"aiworkspace.commands.executable",executable); row(form,3,"aiworkspace.commands.args",args); row(form,4,"aiworkspace.commands.workdir",workdir); row(form,5,"aiworkspace.commands.mode",mode); row(form,6,"aiworkspace.commands.file.source",fileSource); GridBagConstraints chk=g(1,7); chk.gridwidth=2; form.add(enabled,chk);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); JButton add=new JButton(CodeReadingNoteBundle.message("aiworkspace.commands.add")), save=new JButton(CodeReadingNoteBundle.message("aiworkspace.commands.save")), del=new JButton(CodeReadingNoteBundle.message("aiworkspace.commands.delete")), run=new JButton(CodeReadingNoteBundle.message("aiworkspace.commands.run")); add.addActionListener(e->clear()); save.addActionListener(e->save()); del.addActionListener(e->delete()); run.addActionListener(e->run()); buttons.add(add); buttons.add(save); buttons.add(del); buttons.add(run); GridBagConstraints act=g(0,8); act.gridwidth=3; form.add(buttons,act); root.add(form,BorderLayout.CENTER); root.setBorder(JBUI.Borders.empty(6));
        Dimension layoutPreferred = root.getPreferredSize();
        root.setPreferredSize(new Dimension(
                Math.max(JBUI.scale(900), layoutPreferred.width),
                Math.max(JBUI.scale(500), layoutPreferred.height)));
        clear(); return root;
    }
    private void row(JPanel p,int r,String key,JComponent c){GridBagConstraints l=g(0,r);l.anchor=GridBagConstraints.WEST;p.add(new JLabel(CodeReadingNoteBundle.message(key)),l);GridBagConstraints f=g(1,r);f.weightx=1;f.fill=GridBagConstraints.HORIZONTAL;f.gridwidth=2;p.add(c,f);}
    private GridBagConstraints g(int x,int y){GridBagConstraints c=new GridBagConstraints();c.gridx=x;c.gridy=y;c.insets=JBUI.insets(4);return c;}
    private void clear(){editing=null;id.setText("");name.setText("");executable.setText("");args.setText("");workdir.setText("");enabled.setSelected(true);mode.setSelectedIndex(0);fileSource.setSelectedIndex(0);list.clearSelection();}
    private void load(AIWorkspaceCommand c){if(c==null)return;editing=c;id.setText(c.id);name.setText(display(c));executable.setText(c.executable);args.setText(c.args==null?"":String.join(" ",c.args));workdir.setText(c.workingDirectory==null?"":c.workingDirectory);enabled.setSelected(c.enabled);mode.setSelectedIndex("silent".equalsIgnoreCase(c.executionMode)?1:0);fileSource.setSelectedIndex("editor".equalsIgnoreCase(AIWorkspaceCommandService.resolveFileSource(c.fileSource))?1:0);}
    private void save(){AIWorkspaceCommand c=editing==null?new AIWorkspaceCommand():editing;c.id=id.getText().trim();c.name=name.getText().trim();c.displayName=c.name;c.executable=executable.getText().trim();String a=args.getText().trim();c.args=a.isEmpty()?new ArrayList<>():new ArrayList<>(Arrays.asList(a.split("\\s+")));c.workingDirectory=workdir.getText().trim();c.enabled=enabled.isSelected();c.confirmEachTime=false;c.executionMode=mode.getSelectedIndex()==1?"silent":"terminal";c.openTerminal=!"silent".equals(c.executionMode);c.fileSource=fileSource.getSelectedIndex()==1?"editor":"project";if(c.id.isEmpty()||c.name.isEmpty()||c.executable.isEmpty()){Messages.showWarningDialog(project,CodeReadingNoteBundle.message("aiworkspace.commands.required"),CodeReadingNoteBundle.message("aiworkspace.commands.manage"));return;}try{AIWorkspaceCommandService.getInstance(project).upsert(c);if(editing==null){commands.add(c);model.addElement(c);}list.setSelectedValue(c,true);}catch(Exception e){Messages.showErrorDialog(project,e.getMessage(),CodeReadingNoteBundle.message("aiworkspace.error.title"));}}
    private void delete(){AIWorkspaceCommand c=list.getSelectedValue();if(c==null)return;try{if(AIWorkspaceCommandService.getInstance(project).delete(c.id)){commands.remove(c);model.removeElement(c);clear();}}catch(Exception e){Messages.showErrorDialog(project,e.getMessage(),CodeReadingNoteBundle.message("aiworkspace.error.title"));}}
    private void run(){
        AIWorkspaceCommand c=list.getSelectedValue(); if(c==null||!c.enabled)return;
        var result=AIWorkspaceCommandService.getInstance(project).runConfigured(c);
        if(result.exitCode()!=0) Messages.showErrorDialog(project,result.output(),CodeReadingNoteBundle.message("aiworkspace.error.title"));
    }
    private static String display(AIWorkspaceCommand c){return c.displayName==null||c.displayName.isBlank()?c.name:c.displayName;}
}
