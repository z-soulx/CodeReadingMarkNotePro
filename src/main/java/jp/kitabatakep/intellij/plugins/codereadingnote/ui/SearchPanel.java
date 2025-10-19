package jp.kitabatakep.intellij.plugins.codereadingnote.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import jp.kitabatakep.intellij.plugins.codereadingnote.search.SearchService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索面板组件
 * 包含搜索框和搜索结果展示
 */
public class SearchPanel extends JPanel {
    private final Project project;
    private final JBTextField searchField;
    private final JBList<SearchResultItem> resultList;
    private final DefaultListModel<SearchResultItem> resultListModel;
    private final JLabel statusLabel;
    
    private List<Topic> topics = new ArrayList<>();
    private SearchResultListener resultListener;
    
    /**
     * 搜索结果项
     */
    public static class SearchResultItem {
        private final SearchService.SearchResult result;
        
        public SearchResultItem(SearchService.SearchResult result) {
            this.result = result;
        }
        
        public SearchService.SearchResult getResult() {
            return result;
        }
        
        @Override
        public String toString() {
            TopicLine line = result.getTopicLine();
            Topic topic = result.getTopic();
            TopicGroup group = result.getGroup();
            
            // 格式：[Topic名称] > [Group名称] > 中文注释 (类名:行号) - 相似度
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            
            // Topic名称
            sb.append("<span style='color:#6897BB;'>[").append(topic.name()).append("]</span>");
            
            // Group名称（如果有）
            if (group != null) {
                sb.append(" <span style='color:#9876AA;'>▸ [").append(group.name()).append("]</span>");
            } else {
                sb.append(" <span style='color:#808080;'>▸ [Ungrouped]</span>");
            }
            
            // 主要内容
            sb.append(" ▸ ");
            
            // 中文注释（高亮）
            String note = line.note();
            if (note != null && !note.isEmpty()) {
                sb.append("<b>").append(note).append("</b>");
            }
            
            // 类名和行号
            sb.append(" <span style='color:#808080;'>(");
            sb.append(line.pathForDisplay()).append(":").append(line.line());
            sb.append(")</span>");
            
            // 相似度评分
            int scorePercent = (int) (result.getScore() * 100);
            String scoreColor = scorePercent > 70 ? "#50FA7B" : (scorePercent > 40 ? "#FFB86C" : "#FF5555");
            sb.append(" <span style='color:").append(scoreColor).append(";'>")
              .append(scorePercent).append("%</span>");
            
            sb.append("</html>");
            return sb.toString();
        }
    }
    
    /**
     * 搜索结果监听器
     */
    public interface SearchResultListener {
        void onResultSelected(SearchService.SearchResult result);
    }
    
    public SearchPanel(Project project) {
        this.project = project;
        
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建搜索框面板
        JPanel searchBoxPanel = new JPanel(new BorderLayout(5, 0));
        
        // 搜索图标
        JLabel searchIcon = new JLabel(AllIcons.Actions.Search);
        searchBoxPanel.add(searchIcon, BorderLayout.WEST);
        
        // 搜索输入框
        searchField = new JBTextField();
        searchField.getEmptyText().setText("Search TopicLine notes... (Supports Pinyin/Fuzzy search)");
        searchBoxPanel.add(searchField, BorderLayout.CENTER);
        
        // 清除按钮
        JButton clearButton = new JButton(AllIcons.Actions.Close);
        clearButton.setPreferredSize(new Dimension(20, 20));
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setToolTipText("Clear search");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            clearResults();
        });
        searchBoxPanel.add(clearButton, BorderLayout.EAST);
        
        add(searchBoxPanel, BorderLayout.NORTH);
        
        // 创建结果列表
        resultListModel = new DefaultListModel<>();
        resultList = new JBList<>(resultListModel);
        resultList.setCellRenderer(new SearchResultCellRenderer());
        
        // 添加鼠标监听
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 双击：跳转到代码
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    navigateToCode();
                }
                // 右键：显示上下文菜单
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    int index = resultList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        resultList.setSelectedIndex(index);
                        showContextMenu(e);
                    }
                }
            }
        });
        
        // 添加回车键监听
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateToCode();
                }
            }
        });
        
        JBScrollPane scrollPane = new JBScrollPane(resultList);
        add(scrollPane, BorderLayout.CENTER);
        
        // 状态标签
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(JBColor.GRAY);
        add(statusLabel, BorderLayout.SOUTH);
        
        // 添加搜索框监听器
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                performSearch();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                performSearch();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                performSearch();
            }
        });
        
        // 搜索框回车键监听
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && resultListModel.getSize() > 0) {
                    resultList.setSelectedIndex(0);
                    navigateToCode();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && resultListModel.getSize() > 0) {
                    resultList.requestFocus();
                    resultList.setSelectedIndex(0);
                }
            }
        });
    }
    
    /**
     * 显示右键上下文菜单
     */
    private void showContextMenu(MouseEvent e) {
        SearchResultItem selectedItem = resultList.getSelectedValue();
        if (selectedItem == null) {
            return;
        }
        
        JPopupMenu popupMenu = new JPopupMenu();
        
        // Menu item 1: Navigate to code
        JMenuItem navigateToCodeItem = new JMenuItem("Navigate to Code", AllIcons.Actions.EditSource);
        navigateToCodeItem.addActionListener(event -> navigateToCode());
        popupMenu.add(navigateToCodeItem);
        
        // Menu item 2: Locate in tree view
        JMenuItem locateInTreeItem = new JMenuItem("Locate in Tree View (Switch to Tree)", AllIcons.General.Locate);
        locateInTreeItem.addActionListener(event -> locateInTreeView());
        popupMenu.add(locateInTreeItem);
        
        popupMenu.show(resultList, e.getX(), e.getY());
    }
    
    /**
     * 跳转到代码位置
     */
    private void navigateToCode() {
        SearchResultItem selectedItem = resultList.getSelectedValue();
        if (selectedItem == null) {
            return;
        }
        
        TopicLine line = selectedItem.getResult().getTopicLine();
        
        // 使用TopicLine的navigate方法跳转到代码
        if (line != null && line.canNavigate()) {
            line.navigate(true);
        }
    }
    
    /**
     * 在树视图中定位（原有的联动功能）
     */
    private void locateInTreeView() {
        SearchResultItem selectedItem = resultList.getSelectedValue();
        if (selectedItem == null) {
            return;
        }
        
        // 调用原有的resultListener来触发树视图联动
        if (resultListener != null) {
            resultListener.onResultSelected(selectedItem.getResult());
        }
    }
    
    /**
     * 设置要搜索的Topic列表
     */
    public void setTopics(List<Topic> topics) {
        this.topics = topics != null ? topics : new ArrayList<>();
    }
    
    /**
     * 设置搜索结果监听器
     */
    public void setResultListener(SearchResultListener listener) {
        this.resultListener = listener;
    }
    
    /**
     * 执行搜索
     */
    private void performSearch() {
        String query = searchField.getText().trim();
        
        if (query.isEmpty()) {
            clearResults();
            return;
        }
        
        // 在后台线程执行搜索
        SwingUtilities.invokeLater(() -> {
            try {
                List<SearchService.SearchResult> results = SearchService.search(topics, query);
                
                // 更新UI
                SwingUtilities.invokeLater(() -> {
                    resultListModel.clear();
                    
                    for (SearchService.SearchResult result : results) {
                        resultListModel.addElement(new SearchResultItem(result));
                    }
                    
                    // 更新状态
                    if (results.isEmpty()) {
                        statusLabel.setText("No matching results found");
                    } else {
                        statusLabel.setText("Found " + results.size() + " result(s)");
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Search error: " + ex.getMessage());
                });
            }
        });
    }
    
    /**
     * 清除搜索结果
     */
    private void clearResults() {
        resultListModel.clear();
        statusLabel.setText(" ");
    }
    
    /**
     * 获取搜索框焦点
     */
    public void focusSearchField() {
        searchField.requestFocus();
    }
    
    /**
     * 搜索结果单元格渲染器
     */
    private static class SearchResultCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                     int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof SearchResultItem) {
                label.setText(value.toString());
                label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            }
            
            return label;
        }
    }
}

