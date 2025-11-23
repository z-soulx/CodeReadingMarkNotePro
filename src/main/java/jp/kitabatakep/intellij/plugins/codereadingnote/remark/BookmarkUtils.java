package jp.kitabatakep.intellij.plugins.codereadingnote.remark;

import com.intellij.ide.bookmark.Bookmark;
import com.intellij.ide.bookmark.BookmarkGroup;
import com.intellij.ide.bookmark.BookmarkType;
import com.intellij.ide.bookmark.BookmarksManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import jp.kitabatakep.intellij.plugins.codereadingnote.AppConstants;
import jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BookmarkUtils {
    private static final Logger LOG = Logger.getInstance(BookmarkUtils.class);
    public static Bookmark addBookmark(Project project, @NotNull VirtualFile file, int line, String note, String uid) {

        Document document = FileDocumentManager.getInstance().getDocument(file);

        if (document != null && line < document.getLineCount()) {
            String description = note.substring(0, Math.min(note.length(), 10)).concat("$").concat(uid);
            
            com.intellij.ide.bookmarks.Bookmark bookmark = createBookmark(project, file, line, description);
            if (bookmark == null) {
                LOG.error("Failed to create old-style bookmark!");
                return null;
            }
            
//            Old BookmarkManager API was deprecated, now using BookmarksManager
//			Bookmark bookmark = new Bookmark(file.getPath(),line,note.substring(0, Math.min(note.length(), 20)));
            BookmarksManager instance = BookmarksManager.getInstance(project);
            com.intellij.ide.bookmark.Bookmark bookmark1 = instance.createBookmark(bookmark);
            if (bookmark1 == null) {
                LOG.error("Failed to create new-style bookmark!");
                return null;
            }
            
            if(null == instance.getGroup(AppConstants.appName)) {
                instance.addGroup(AppConstants.appName, false);
            }
            
            instance.getGroup(AppConstants.appName).add(bookmark1, BookmarkType.DEFAULT, bookmark.getDescription());
            
            return bookmark1;
            // 执行自定义逻辑
        }
        LOG.warn("Failed to create bookmark: document is null or line " + line + " is out of bounds (line=" + line + ", docLines=" + (document != null ? document.getLineCount() : "null") + ")");
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
        String targetUuid = _topicLine.getBookmarkUid();
        
        if (group == null) {
            return null;
        }
        
        for (com.intellij.ide.bookmark.Bookmark bookmark : group.getBookmarks()) {
            String description = group.getDescription(bookmark);
            String bookmarkUuid = StringUtils.extractUUID(description);
            
            if (bookmarkUuid != null && bookmarkUuid.equals(targetUuid)) {
                return bookmark;
            }
        }
        
        LOG.warn("No matching bookmark found for UUID: " + targetUuid + " at line: " + _topicLine.line());
        return null;
    }

    public static Bookmark machBookmark(TopicLine _topicLine, Project project) {
        BookmarkGroup group = BookmarksManager.getInstance(project).getGroup(AppConstants.appName);
        Bookmark bookmark = BookmarkUtils.machBookmark(_topicLine, group);
        return bookmark;
    }
    public static  boolean removeMachBookmark(TopicLine _topicLine, Project project) {
        BookmarksManager manager = BookmarksManager.getInstance(project);
        BookmarkGroup group = manager.getGroup(AppConstants.appName);
        Bookmark bookmark = machBookmark(_topicLine, group);
        
        if (bookmark != null) {
            // IMPORTANT: Must remove from BOTH the group AND the manager
            group.remove(bookmark);
            manager.remove(bookmark);
            return true;
        }
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
    
    /**
     * Find bookmark by UUID
     */
    @Nullable
    public static Bookmark findBookmarkByUuid(@NotNull Project project, @NotNull String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        
        BookmarksManager manager = BookmarksManager.getInstance(project);
        if (manager == null) {
            return null;
        }
        
        for (BookmarkGroup group : manager.getGroups()) {
            for (Bookmark bookmark : group.getBookmarks()) {
                String bookmarkUuid = getBookmarkUid(bookmark);
                if (uuid.equals(bookmarkUuid)) {
                    return bookmark;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get bookmark UUID from bookmark
     * Note: This method works with the new Bookmark API (com.intellij.ide.bookmark.Bookmark)
     */
    @Nullable
    public static String getBookmarkUid(@NotNull Bookmark bookmark) {
        // For the new Bookmark API, we need to get the project context differently
        // This is a simplified version - may need adjustment based on actual usage
        try {
            // Try to get UUID from bookmark attributes if available
            return bookmark.getAttributes().get("uuid") != null ? 
                bookmark.getAttributes().get("uuid").toString() : null;
        } catch (Exception e) {
            LOG.warn("Failed to get bookmark UUID", e);
            return null;
        }
    }
    
    /**
     * Set bookmark UUID
     * Note: This method works with the new Bookmark API (com.intellij.ide.bookmark.Bookmark)
     */
    public static void setBookmarkUid(@NotNull Bookmark bookmark, @NotNull String uuid) {
        try {
            bookmark.getAttributes().put("uuid", uuid);
        } catch (Exception e) {
            LOG.warn("Failed to set bookmark UUID", e);
        }
    }
    
    /**
     * Update bookmark description
     */
    public static boolean updateBookmarkDescription(@NotNull Project project, 
                                                   @NotNull String uuid, 
                                                   @NotNull String newDescription) {
        Bookmark bookmark = findBookmarkByUuid(project, uuid);
        if (bookmark == null) {
            return false;
        }
        
        try {
            BookmarksManager manager = BookmarksManager.getInstance(project);
            if (manager == null) {
                return false;
            }
            
            // Use reflection to update description
            java.lang.reflect.Method setDescriptionMethod = bookmark.getClass().getMethod("setDescription", String.class);
            setDescriptionMethod.invoke(bookmark, newDescription);
            
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to update bookmark description", e);
            return false;
        }
    }
    
    /**
     * Update bookmark line number by UUID
     */
    public static boolean updateBookmarkLine(@NotNull Project project, 
                                            @NotNull String uuid, 
                                            int newLineNumber) {
        Bookmark bookmark = findBookmarkByUuid(project, uuid);
        if (bookmark == null) {
            return false;
        }
        
        return updateBookmarkLine(project, bookmark, newLineNumber);
    }
    
    /**
     * Update bookmark line number
     * Note: For the new Bookmark API, updating line numbers may require different approach
     * This is a simplified placeholder that may need project-specific implementation
     */
    public static boolean updateBookmarkLine(@NotNull Project project, 
                                            @NotNull Bookmark bookmark, 
                                            int newLineNumber) {
        try {
            // For the new Bookmark API (com.intellij.ide.bookmark.Bookmark),
            // we might need to use BookmarksManager to update the bookmark
            BookmarksManager manager = BookmarksManager.getInstance(project);
            if (manager == null) {
                return false;
            }
            
            // Store UUID before any operations
            String uuid = getBookmarkUid(bookmark);
            
            // The new API might handle line updates differently
            // This is a placeholder - actual implementation may vary
            LOG.info("Updating bookmark to line " + newLineNumber + " for UUID: " + uuid);
            
            // For now, just update the UUID attribute to track the change
            if (uuid != null) {
                setBookmarkUid(bookmark, uuid);
            }
            
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to update bookmark line", e);
            return false;
        }
    }
}
