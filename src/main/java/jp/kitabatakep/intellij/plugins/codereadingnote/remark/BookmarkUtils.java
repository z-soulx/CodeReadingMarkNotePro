package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.ide.bookmark.BookmarkType;
import com.intellij.ide.bookmark.BookmarksManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BookmarkUtils {
    public static Bookmark addBookmark(Project project, @NotNull VirtualFile file, int line, String note, String uid) {

        Document document = FileDocumentManager.getInstance().getDocument(file);

        if (document != null && line < document.getLineCount()) {
            com.intellij.ide.bookmarks.Bookmark bookmark = createBookmark(project,file, line,
                    note.substring(0, Math.min(note.length(), 10)).concat("$").concat(uid));
//            Old BookmarkManager API was deprecated, now using BookmarksManager
//			Bookmark bookmark = new Bookmark(file.getPath(),line,note.substring(0, Math.min(note.length(), 20)));
            BookmarksManager instance = BookmarksManager.getInstance(project);
            com.intellij.ide.bookmark.Bookmark bookmark1 = instance.createBookmark(bookmark);
            if(null == instance.getGroup(AppConstants.appName)) instance.addGroup(AppConstants.appName,false);
            instance.getGroup(AppConstants.appName).add(bookmark1, BookmarkType.DEFAULT,bookmark.getDescription());
//			instance.getGroup(AppConstants.appName).add(bookmark1,BookmarkType.DEFAULT,note.substring(0, Math.min(note.length(), 20)));
            return bookmark1;
            // 执行自定义逻辑
        }
        return null;

    }
    public static com.intellij.ide.bookmarks.Bookmark createBookmark(Project project, VirtualFile file, int line, String description) {
        try {
            Constructor<com.intellij.ide.bookmarks.Bookmark> constructor = com.intellij.ide.bookmarks.Bookmark.class.getDeclaredConstructor(Project.class, VirtualFile.class, int.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(project, file, line, description);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Bookmark machBookmark(TopicLine _topicLine, BookmarkGroup group) {
        for (com.intellij.ide.bookmark.Bookmark bookmark : group.getBookmarks()) {
            String description = group.getDescription(bookmark);
            if (StringUtils.extractUUID(description).equals(_topicLine.getBookmarkUid())) {
                return bookmark;
            }
        }
        return null;
    }

    public static Bookmark machBookmark(TopicLine _topicLine, Project project) {
        BookmarkGroup group = BookmarksManager.getInstance(project).getGroup(AppConstants.appName);
        Bookmark bookmark = BookmarkUtils.machBookmark(_topicLine, group);
        return bookmark;
    }
    public static  boolean removeMachBookmark(TopicLine _topicLine, Project project) {
        BookmarkGroup group = BookmarksManager.getInstance(project).getGroup(AppConstants.appName);
        Bookmark bookmark = machBookmark(_topicLine, group);
        if (bookmark != null)
            return group.remove(bookmark);
        return false;
    }

    public static List<Bookmark> getAllBookmark(Project project) {
        BookmarkGroup group = BookmarksManager.getInstance(project).getGroup(AppConstants.appName);
        return group.getBookmarks();
    }

    public static BookmarkGroup getGroup(Project project) {
        BookmarkGroup group = BookmarksManager.getInstance(project).getGroup(AppConstants.appName);
        return group;
    }

    @NotNull
    public static Consumer<TopicLine> consumerLine(Map<String, Bookmark> collect) {
        return topicLine -> {
            Bookmark bookmark = collect.get(topicLine.getBookmarkUid());
            if (bookmark != null) {
                String newline = bookmark.getAttributes().get("line").toString();
                if (StringUtils.isNotEmpty(newline) && !newline.equals(String.valueOf(topicLine.line()))) {
                    topicLine.modifyLine(Integer.valueOf(newline));
                }
            }
        };
    }
    @NotNull
    public static Map<String, Bookmark> getStringBookmarkMap(Project project) {
        List<Bookmark> allBookmark = BookmarkUtils.getAllBookmark(project);
        BookmarkGroup group = BookmarkUtils.getGroup(project);
        return allBookmark.stream().collect(Collectors.toMap(r -> StringUtils.extractUUID(group.getDescription(r)), value -> value));
    }
}
