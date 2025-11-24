package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarksManager;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.openapi.project.Project;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MyBookmarkListener {

    private Project myProject;

    public MyBookmarkListener(Project project) {
        myProject = project;
        // 新的书签系统使用BookmarksManager，监听方式可能需要调整
        // 这里暂时保留基本结构，具体监听逻辑可能需要根据新API调整
    }

    public void bookmarkAdded(@NotNull Bookmark bookmark) {
        System.out.println("Bookmark added");
        // 在这里添加你的自定义逻辑
        extracted();
    }

    public void bookmarkRemoved(@NotNull Bookmark bookmark) {
        System.out.println("Bookmark removed");
        // 在这里添加你的自定义逻辑
        extracted();
    }

    public void bookmarkChanged(@NotNull Bookmark bookmark) {
        System.out.println("Bookmark changed");
        // 在这里添加你的自定义逻辑
        extracted();
    }

    public void bookmarksOrderChanged() {
        extracted();
    }

    private void extracted() {
        BookmarksManager bookmarksManager = BookmarksManager.getInstance(myProject);
        List<BookmarkGroup> groups = bookmarksManager.getGroups();
        for (BookmarkGroup group : groups) {
            List<Bookmark> bookmarks = group.getBookmarks();
            for (Bookmark bookmark : bookmarks) {
                String description = group.getDescription(bookmark);
                System.out.println(description != null ? description : "No description");
            }
        }
    }
}
