package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MyBookmarkListener implements BookmarksListener {

    private Project myProject;

    public MyBookmarkListener(Project project) {
        myProject = project;
        project.getMessageBus().connect().subscribe(BookmarksListener.TOPIC, this);
    }

    @Override
    public void bookmarkAdded(@NotNull Bookmark bookmark) {
        System.out.println("Bookmark added: " + bookmark.getFile().getPath() + " at line " + bookmark.getLine());
        // 在这里添加你的自定义逻辑
        extracted();
    }

    @Override
    public void bookmarkRemoved(@NotNull Bookmark bookmark) {
        System.out.println("Bookmark removed: " + bookmark.getFile().getPath() + " at line " + bookmark.getLine());
        // 在这里添加你的自定义逻辑
        extracted();
    }

    @Override
    public void bookmarkChanged(@NotNull Bookmark bookmark) {
        System.out.println("Bookmark changed: " + bookmark.getFile().getPath() + " at line " + bookmark.getLine());
        // 在这里添加你的自定义逻辑
        extracted();
    }
    @Override
    public void bookmarksOrderChanged() {
        extracted();
    }

    private void extracted() {
        Collection<Bookmark> allBookmarks = BookmarkManager.getInstance(myProject)
            .getAllBookmarks();
        for (Bookmark bookmark : allBookmarks) {
            System.out.println(bookmark.getDescription());
        }
    }
}
