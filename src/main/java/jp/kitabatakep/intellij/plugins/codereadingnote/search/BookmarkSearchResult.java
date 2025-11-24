package jp.kitabatakep.intellij.plugins.codereadingnote.search;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Bookmark 搜索结果包装类
 * 封装 IDEA 原生 Bookmark 的搜索结果信息
 */
public class BookmarkSearchResult implements Comparable<BookmarkSearchResult> {
    private final Bookmark bookmark;
    private final String description;
    private final String groupName;
    private final VirtualFile file;
    private final int line;
    private final double score;
    
    public BookmarkSearchResult(Bookmark bookmark, String description, String groupName, 
                                VirtualFile file, int line, double score) {
        this.bookmark = bookmark;
        this.description = description;
        this.groupName = groupName;
        this.file = file;
        this.line = line;
        this.score = score;
    }
    
    public Bookmark getBookmark() {
        return bookmark;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public VirtualFile getFile() {
        return file;
    }
    
    public int getLine() {
        return line;
    }
    
    public double getScore() {
        return score;
    }
    
    @Override
    public int compareTo(BookmarkSearchResult other) {
        // 按评分降序排序
        return Double.compare(other.score, this.score);
    }
}

