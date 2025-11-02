package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.JBUI;
import jp.kitabatakep.intellij.plugins.codereadingnote.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.actions.*;
import jp.kitabatakep.intellij.plugins.codereadingnote.search.SearchService;

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
    private SearchPanel searchPanel; // 搜索面板
    
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
        searchPanel = new SearchPanel(project);
        
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
        
        // Set up search panel listener
        searchPanel.setResultListener(result -> {
            // 当搜索结果被选中时，在树中高亮并显示详情
            Topic topic = result.getTopic();
            TopicGroup group = result.getGroup();
            TopicLine line = result.getTopicLine();
            
            // 在树中选择对应的节点
            if (group != null) {
                topicTreePanel.selectGroupLine(group, line);
            } else {
                topicTreePanel.selectUngroupedLine(topic, line);
            }
            
            // 在详情面板中显示
            selectedTopic = topic;
            selectedGroup = group;
            selectedTopicLine = line;
            topicDetailPanel.setTopicLine(line);
            
            // 切换到树视图标签页
            JTabbedPane tabbedPane = findTabbedPane();
            if (tabbedPane != null) {
                tabbedPane.setSelectedIndex(0); // 切换到第一个标签页（树视图）
            }
        });
        
        // 更新搜索面板的数据源
        updateSearchData();
    }
    
    // 查找TabbedPane（用于搜索结果跳转）
    private JTabbedPane findTabbedPane() {
        Component parent = getParent();
        while (parent != null) {
            if (parent instanceof JTabbedPane) {
                return (JTabbedPane) parent;
            }
            if (parent instanceof JComponent) {
                for (Component child : ((JComponent) parent).getComponents()) {
                    if (child instanceof JTabbedPane) {
                        return (JTabbedPane) child;
                    }
                }
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    // 更新搜索面板的数据源
    private void updateSearchData() {
        if (searchPanel != null && service != null) {
            searchPanel.setTopics(service.getTopicList().getTopics());
        }
    }
    
    private void setupLayout() {
        // 创建主视图（树视图）
        JBSplitter splitPane = new JBSplitter(0.15f);
        splitPane.setSplitterProportionKey(AppConstants.appName + "ManagementPanel.splitter");
        splitPane.setHonorComponentsMinimumSize(false);

        splitPane.setFirstComponent(topicTreePanel);
        splitPane.setSecondComponent(topicDetailPanel);
        
        // 创建选项卡面板
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("Tree View", splitPane);
        tabbedPane.addTab("Search", searchPanel);

        add(actionToolBar(), BorderLayout.PAGE_START);
        add(tabbedPane, BorderLayout.CENTER);
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
                updateSearchData(); // 更新搜索数据
            }

            @Override
            public void topicRemoved(Topic topic)
            {
                topicTreePanel.loadTopics();
                topicDetailPanel.clear();
                updateSearchData(); // 更新搜索数据
            }

            @Override
            public void topicsLoaded() {
                topicTreePanel.loadTopics();
                updateSearchData(); // 更新搜索数据
            }
        });
        
        // Listen for topic changes to refresh the tree
        messageBus.connect().subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
            @Override
            public void lineAdded(Topic topic, TopicLine line) {
                topicTreePanel.refreshTopic(topic);
                updateSearchData(); // 更新搜索数据
            }

            @Override
            public void lineRemoved(Topic topic, TopicLine line) {
                topicTreePanel.refreshTopic(topic);
                updateSearchData(); // 更新搜索数据
            }
        });
    }

    private JComponent actionToolBar()
    {
        DefaultActionGroup actions = new DefaultActionGroup();
        actions.add(new TopicAddAction());
        actions.add(new TopicRenameAction((v) -> getSelectedTopic()));
        actions.add(new TopicRemoveAction(project, (v) -> getSelectedTopic()));
        
        // Group-related actions
        actions.addSeparator();
        actions.add(new GroupAddAction(() -> getSelectedTopic()));
        actions.add(new GroupRenameAction(() -> getSelectedGroup()));
        actions.add(new GroupRemoveAction(() -> getSelectedGroup()));
        actions.add(new LineToGroupMoveAction(() -> getSelectedTopicLine()));
        
        // Tree operations - 树操作
        actions.addSeparator();
        actions.add(new CollapseAllAction(() -> topicTreePanel)); // 一键全收缩按钮
        
        actions.addSeparator();
        actions.add(new FixRemarkAction(project));
        actions.add(new FixTopicRemarkAction(project,(v) -> getSelectedTopic()));
        actions.addSeparator();
        actions.add(new ExportAction());
        actions.add(new ImportAction());
        
        // Sync actions
        actions.addSeparator();
        actions.add(new SyncPushAction());
        actions.add(new SyncPullAction());

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(AppConstants.appName, actions, true);
        
        // 修复警告：设置toolbar的target component，确保actions能正确获取上下文
        // 使用topicTreePanel作为target，因为大部分actions都依赖于tree的选择状态
        actionToolbar.setTargetComponent(topicTreePanel);
        
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
