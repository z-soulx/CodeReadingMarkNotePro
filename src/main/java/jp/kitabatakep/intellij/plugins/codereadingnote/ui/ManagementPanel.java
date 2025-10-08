package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.*;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;

public class ManagementPanel extends JPanel
{
    private Project project;
    private CodeReadingNoteService service;
    
    // New tree-based UI components
    private TopicTreePanel topicTreePanel;
    private TopicDetailPanel topicDetailPanel;
    
    // Current selection state
    private Topic selectedTopic;
    private TopicGroup selectedGroup;
    private TopicLine selectedTopicLine;

    public ManagementPanel(Project project)
    {
        super(new BorderLayout());
        this.project = project;
        service = CodeReadingNoteService.getInstance(project);
        
        initComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initComponents() {
        topicTreePanel = new TopicTreePanel(project);
        topicDetailPanel = new TopicDetailPanel(project);
        
        // Set up tree selection listener
        topicTreePanel.setSelectionListener(new TopicTreePanel.TopicTreeSelectionListener() {
            @Override
            public void onTopicSelected(Topic topic) {
                selectedTopic = topic;
                selectedGroup = null;
                selectedTopicLine = null;
                topicDetailPanel.setTopic(topic);
            }

            @Override
            public void onGroupSelected(TopicGroup group) {
                selectedTopic = group.getParentTopic();
                selectedGroup = group;
                selectedTopicLine = null;
                topicDetailPanel.setGroup(group);
            }

            @Override
            public void onUngroupedLinesSelected(Topic topic) {
                // 新增：处理点击Ungrouped Lines文件夹的情况
                // 显示该Topic的未分组TopicLine列表
                selectedTopic = topic;
                selectedGroup = null;
                selectedTopicLine = null;
                topicDetailPanel.setUngroupedLines(topic);
            }

            @Override
            public void onTopicLineSelected(TopicLine topicLine) {
                selectedTopic = topicLine.topic();
                selectedGroup = null;
                selectedTopicLine = topicLine;
                topicDetailPanel.setTopicLine(topicLine);
            }

            @Override
            public void onSelectionCleared() {
                selectedTopic = null;
                selectedGroup = null;
                selectedTopicLine = null;
                topicDetailPanel.clear();
            }
        });
    }
    
    private void setupLayout() {
        JBSplitter splitPane = new JBSplitter(0.15f);
        splitPane.setSplitterProportionKey(AppConstants.appName + "ManagementPanel.splitter");
        splitPane.setHonorComponentsMinimumSize(false);

        splitPane.setFirstComponent(topicTreePanel);
        splitPane.setSecondComponent(topicDetailPanel);

        add(actionToolBar(), BorderLayout.PAGE_START);
        add(splitPane);
    }
    
    private void setupEventHandlers() {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(TopicListNotifier.TOPIC_LIST_NOTIFIER_TOPIC, new TopicListNotifier()
        {
            @Override
            public void topicAdded(Topic topic)
            {
                topicTreePanel.loadTopics();
                topicTreePanel.selectTopic(topic);
            }

            @Override
            public void topicRemoved(Topic topic)
            {
                topicTreePanel.loadTopics();
                topicDetailPanel.clear();
            }

            @Override
            public void topicsLoaded() {
                topicTreePanel.loadTopics();
            }
        });
        
        // Listen for topic changes to refresh the tree
        messageBus.connect().subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
            @Override
            public void lineAdded(Topic topic, TopicLine line) {
                topicTreePanel.refreshTopic(topic);
            }

            @Override
            public void lineRemoved(Topic topic, TopicLine line) {
                topicTreePanel.refreshTopic(topic);
            }
        });
    }

    private JComponent actionToolBar()
    {
        DefaultActionGroup actions = new DefaultActionGroup();
        actions.add(new TopicAddAction());
        actions.add(new TopicRenameAction((v) -> getSelectedTopic()));
        actions.add(new TopicRemoveAction(project, (v) -> getSelectedTopic()));
        
        // Subgroup-related actions
        actions.addSeparator();
        actions.add(new GroupAddAction(() -> getSelectedTopic()));
        actions.add(new GroupRenameAction(() -> getSelectedGroup()));
        actions.add(new GroupRemoveAction(() -> getSelectedGroup()));
        actions.add(new LineToGroupMoveAction(() -> getSelectedTopicLine()));
        
        actions.addSeparator();
        actions.add(new FixRemarkAction(project));
        actions.add(new FixTopicRemarkAction(project,(v) -> getSelectedTopic()));
        actions.addSeparator();
        actions.add(new ExportAction());
        actions.add(new ImportAction());

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(AppConstants.appName, actions, true);
        actionToolbar.setReservePlaceAutoPopupIcon(false);
        actionToolbar.setMinimumButtonSize(new Dimension(20, 20));

        JComponent toolBar = actionToolbar.getComponent();
        toolBar.setBorder(JBUI.Borders.merge(toolBar.getBorder(), JBUI.Borders.emptyLeft(12), true));
        toolBar.setOpaque(false);
        return toolBar;
    }
    
    // Getter methods for actions
    public Topic getSelectedTopic() {
        return selectedTopic;
    }
    
    public TopicGroup getSelectedGroup() {
        return selectedGroup;
    }
    
    public TopicLine getSelectedTopicLine() {
        return selectedTopicLine;
    }
    
    // Legacy method for backward compatibility
    private void clear()
    {
        selectedTopic = null;
        selectedGroup = null;
        selectedTopicLine = null;
        topicDetailPanel.clear();
    }
}
