package jp.kitabatakep.intellij.plugins.codereadingnote.search;

import jp.kitabatakep.intellij.plugins.codereadingnote.Topic;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicGroup;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务类，实现智能搜索功能
 * 支持：
 * 1. 直接文本匹配
 * 2. 拼音搜索（拼音首字母和完整拼音）
 * 3. 模糊搜索（基于编辑距离）
 * 4. 轻量级向量式搜索（基于相似度评分）
 */
public class SearchService {
    
    /**
     * 搜索结果类
     */
    public static class SearchResult implements Comparable<SearchResult> {
        private final TopicLine topicLine;
        private final Topic topic;
        private final TopicGroup group; // 可能为null
        private final double score; // 相似度评分，越高越相关
        private final String matchedText; // 匹配到的文本
        
        public SearchResult(TopicLine topicLine, Topic topic, TopicGroup group, double score, String matchedText) {
            this.topicLine = topicLine;
            this.topic = topic;
            this.group = group;
            this.score = score;
            this.matchedText = matchedText;
        }
        
        public TopicLine getTopicLine() {
            return topicLine;
        }
        
        public Topic getTopic() {
            return topic;
        }
        
        public TopicGroup getGroup() {
            return group;
        }
        
        public double getScore() {
            return score;
        }
        
        public String getMatchedText() {
            return matchedText;
        }
        
        @Override
        public int compareTo(SearchResult other) {
            // 按评分降序排序
            return Double.compare(other.score, this.score);
        }
    }
    
    /**
     * 在所有Topic中搜索TopicLine
     * @param topics 要搜索的Topic列表
     * @param query 搜索关键词
     * @return 排序后的搜索结果列表
     */
    public static List<SearchResult> search(List<Topic> topics, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        query = query.trim();
        List<SearchResult> results = new ArrayList<>();
        
        for (Topic topic : topics) {
            // 搜索未分组的TopicLine
            for (TopicLine line : topic.getUngroupedLines()) {
                SearchResult result = matchLine(line, topic, null, query);
                if (result != null) {
                    results.add(result);
                }
            }
            
            // 搜索分组中的TopicLine
            for (TopicGroup group : topic.getGroups()) {
                for (TopicLine line : group.getLines()) {
                    SearchResult result = matchLine(line, topic, group, query);
                    if (result != null) {
                        results.add(result);
                    }
                }
            }
        }
        
        // 按评分排序
        Collections.sort(results);
        
        return results;
    }
    
    /**
     * 匹配单个TopicLine
     */
    private static SearchResult matchLine(TopicLine line, Topic topic, TopicGroup group, String query) {
        String note = line.note();
        if (note == null || note.isEmpty()) {
            return null;
        }
        
        double score = calculateSimilarity(note, query);
        
        // 只返回评分大于阈值的结果
        if (score > 0.1) {
            return new SearchResult(line, topic, group, score, note);
        }
        
        return null;
    }
    
    /**
     * 计算相似度评分
     * 综合考虑多种匹配方式，返回0-1之间的评分
     */
    private static double calculateSimilarity(String text, String query) {
        if (text == null || query == null) {
            return 0;
        }
        
        text = text.toLowerCase();
        query = query.toLowerCase();
        
        double maxScore = 0;
        
        // 1. 完全匹配 - 最高分
        if (text.equals(query)) {
            return 1.0;
        }
        
        // 2. 包含匹配 - 高分
        if (text.contains(query)) {
            // 根据匹配位置和占比计算分数
            int index = text.indexOf(query);
            double positionScore = 1.0 - (index / (double) text.length()) * 0.3; // 越靠前分数越高
            double coverageScore = query.length() / (double) text.length(); // 覆盖率越高分数越高
            maxScore = Math.max(maxScore, 0.6 + positionScore * 0.2 + coverageScore * 0.2);
        }
        
        // 3. 拼音首字母匹配 - 中等分数
        String firstLetters = PinyinUtils.getFirstLetters(text);
        if (firstLetters.contains(query)) {
            maxScore = Math.max(maxScore, 0.5);
        }
        
        // 4. 完整拼音匹配 - 中等分数
        String fullPinyin = PinyinUtils.getFullPinyin(text);
        if (fullPinyin.contains(query)) {
            maxScore = Math.max(maxScore, 0.55);
        }
        
        // 5. 编辑距离匹配 - 低分（用于模糊匹配）
        double editDistanceScore = calculateEditDistanceScore(text, query);
        maxScore = Math.max(maxScore, editDistanceScore);
        
        // 6. 字符重叠匹配 - 最低分（类似向量搜索的概念）
        double overlapScore = calculateCharacterOverlapScore(text, query);
        maxScore = Math.max(maxScore, overlapScore);
        
        return maxScore;
    }
    
    /**
     * 基于编辑距离计算评分
     * Levenshtein距离算法
     */
    private static double calculateEditDistanceScore(String text, String query) {
        int distance = levenshteinDistance(text, query);
        int maxLength = Math.max(text.length(), query.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        // 将距离转换为相似度评分
        double similarity = 1.0 - (distance / (double) maxLength);
        
        // 对于编辑距离匹配，给予较低的基础分
        return similarity * 0.4;
    }
    
    /**
     * Levenshtein距离算法实现
     */
    private static int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1;
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 计算字符重叠评分（类似向量相似度）
     * 统计query中的字符在text中出现的比例
     */
    private static double calculateCharacterOverlapScore(String text, String query) {
        if (query.isEmpty()) {
            return 0;
        }
        
        // 统计query中每个字符在text中的出现次数
        Map<Character, Integer> textCharCount = new HashMap<>();
        for (char ch : text.toCharArray()) {
            textCharCount.put(ch, textCharCount.getOrDefault(ch, 0) + 1);
        }
        
        int matchCount = 0;
        for (char ch : query.toCharArray()) {
            if (textCharCount.containsKey(ch) && textCharCount.get(ch) > 0) {
                matchCount++;
                textCharCount.put(ch, textCharCount.get(ch) - 1);
            }
        }
        
        double overlapRatio = matchCount / (double) query.length();
        
        // 字符重叠匹配给予较低的基础分
        return overlapRatio * 0.3;
    }
    
    /**
     * 高亮匹配的文本
     * @param text 原始文本
     * @param query 查询关键词
     * @return 带高亮标记的HTML文本
     */
    public static String highlightMatch(String text, String query) {
        if (text == null || query == null || query.isEmpty()) {
            return text;
        }
        
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // 如果直接包含，则高亮
        if (lowerText.contains(lowerQuery)) {
            int index = lowerText.indexOf(lowerQuery);
            StringBuilder result = new StringBuilder();
            result.append("<html>");
            result.append(text.substring(0, index));
            result.append("<b style='color:#FF6B6B;'>");
            result.append(text.substring(index, index + query.length()));
            result.append("</b>");
            result.append(text.substring(index + query.length()));
            result.append("</html>");
            return result.toString();
        }
        
        return text;
    }
}

