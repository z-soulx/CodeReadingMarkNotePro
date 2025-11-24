# Bug Fixes Summary

## Issues Addressed

### 1. NotSerializableException During Drag-and-Drop in TreeView

**Problem:**
When dragging `TopicLine` items in the TreeView, a `java.io.NotSerializableException: jp.kitabatakep.intellij.plugins.codereadingnote.TopicLine` exception was thrown. This occurred because the drag-and-drop system was attempting to serialize `TopicLine` and `Topic` objects, which do not implement `Serializable`.

**Root Cause:**
The `TopicLineTransferData` class implemented `Serializable`, but contained non-serializable objects (`TopicLine` and `Topic`). The `DataFlavor` was created using the standard constructor, which requires serialization for cross-JVM transfers.

**Solution:**
Changed to use a **JVM-local DataFlavor** that doesn't require serialization. This is the standard approach for drag-and-drop operations within the same JVM instance.

**Files Modified:**

1. **`src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/dnd/TopicLineTransferable.java`**
   - Changed `TOPIC_LINE_FLAVOR` from a simple `DataFlavor` constructor to use `DataFlavor.javaJVMLocalObjectMimeType`
   - Added static initializer block to properly construct the JVM-local DataFlavor
   
   ```java
   public static final DataFlavor TOPIC_LINE_FLAVOR;
   
   static {
       try {
           TOPIC_LINE_FLAVOR = new DataFlavor(
               DataFlavor.javaJVMLocalObjectMimeType + 
               ";class=" + TopicLineTransferData.class.getName()
           );
       } catch (ClassNotFoundException e) {
           throw new RuntimeException("Failed to create DataFlavor for TopicLine", e);
       }
   }
   ```

2. **`src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/dnd/TopicLineTransferData.java`**
   - Removed `implements Serializable` and `serialVersionUID`
   - Added documentation explaining why serialization is not used
   - The class now works as a simple data holder for JVM-local transfers

**Testing:**
After these changes, drag-and-drop operations in the TreeView should work without serialization errors. The data is transferred directly within the same JVM, avoiding any need for serialization.

---

### 2. Enhanced Logging for Bookmark Removal

**Problem:**
When editing a `TopicLine`'s line number, users reported that the old bookmark was not being deleted. This made debugging difficult as there was insufficient logging.

**Root Cause:**
The `removeMachBookmark` method was only calling `group.remove(bookmark)` to remove the bookmark from the BookmarkGroup, but **NOT** calling `BookmarksManager.remove(bookmark)` to actually delete the bookmark from the editor. In IntelliJ's new Bookmark API, you must remove from both the group AND the manager.

**Solution:**
1. Added detailed logging to track bookmark removal operations
2. **Fixed the core issue:** Added `manager.remove(bookmark)` call after `group.remove(bookmark)`
3. The bookmark must be removed from BOTH locations:
   - `BookmarkGroup.remove()` - removes from the group list
   - `BookmarksManager.remove()` - actually deletes the bookmark from the editor UI

**Files Modified:**

**`src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/remark/BookmarkUtils.java`**
```java
public static boolean removeMachBookmark(TopicLine _topicLine, Project project) {
    BookmarksManager manager = BookmarksManager.getInstance(project);
    BookmarkGroup group = manager.getGroup(AppConstants.appName);
    Bookmark bookmark = machBookmark(_topicLine, group);
    
    if (bookmark != null) {
        LOG.info("Found bookmark to remove with UUID: " + _topicLine.getBookmarkUid() + " at line: " + _topicLine.line());
        
        // IMPORTANT: Must remove from BOTH the group AND the manager
        // Step 1: Remove from group
        boolean removedFromGroup = group.remove(bookmark);
        LOG.info("Removed from group: " + removedFromGroup);
        
        // Step 2: Remove from BookmarksManager (this actually deletes the bookmark from editor)
        manager.remove(bookmark);
        LOG.info("Removed from BookmarksManager");
        
        return removedFromGroup;
    } else {
        LOG.warn("No bookmark found to remove with UUID: " + _topicLine.getBookmarkUid() + " at line: " + _topicLine.line());
    }
    return false;
}
```

**Expected Behavior:**
With the current implementation in `LineNumberUpdateService.updateLineNumber()`, the sequence is:

1. **Step 1:** Remove old bookmark (using UUID match, while `line.line()` is still old)
2. **Step 2:** Remove old remark (at old line number)
3. **Step 3:** Update `TopicLine` object to new line number (`line.modifyLine(newLineNum)`)
4. **Step 4:** Create new bookmark at new line number
5. **Step 5:** Add new remark at new line number

This sequence ensures that:
- The old bookmark is found and removed by UUID before the line number changes
- The old remark is removed using the old line number
- The new bookmark and remark are created at the new line number

**Debugging:**
Users can now check the IDE log to verify:
- Whether the old bookmark was found (if not, the UUID might be incorrect)
- Whether the removal operation succeeded
- The exact line numbers and UUIDs involved in the operation

---

## Verification Steps

### For NotSerializableException Fix:
1. Build and run the plugin
2. Create a Topic with Groups
3. Add TopicLines to different groups
4. Try dragging a TopicLine from one group to another in the TreeView
5. Verify no `NotSerializableException` is thrown
6. Verify the TopicLine is successfully moved to the target group

### For Bookmark Removal Logging:
1. Build and run the plugin with the `-Didea.log.debug.categories=jp.kitabatakep.intellij.plugins.codereadingnote` VM option for detailed logging
2. Create a TopicLine with a bookmark
3. Use the "Edit Line Number" action to change the line number
4. Check the IDE log (`Help > Show Log in Explorer`) for messages like:
   - "Found bookmark to remove with UUID: ..."
   - "Bookmark removal result: true"
   - "No bookmark found to remove..." (if there's an issue)
5. Verify in the editor that:
   - The old bookmark at the original line is gone
   - A new bookmark exists at the new line
   - The CodeRemark comment has moved to the new line

---

## Related Files

### Drag-and-Drop System:
- `TopicLineTransferable.java` - Handles data transfer with JVM-local flavor
- `TopicLineTransferData.java` - Data container (no longer serializable)
- `TopicTreeTransferHandler.java` - Tree drag-and-drop handler
- `TopicLineTransferHandler.java` - List drag-and-drop handler

### Bookmark Management:
- `BookmarkUtils.java` - Bookmark creation, matching, and removal
- `LineNumberUpdateService.java` - Line number update orchestration
- `EditorUtils.java` - CodeRemark management

### UI Components:
- `TopicTreePanel.java` - Tree view with drag-and-drop
- `TopicDetailPanel.java` - Detail view with drag-and-drop list

---

## Known Limitations

1. **Cross-Topic Drag-and-Drop:** Currently, dragging TopicLines between different Topics is not supported. Only moving within the same Topic is implemented.

2. **Bookmark Position Tracking:** Bookmarks are matched by UUID stored in the bookmark description. If the UUID is corrupted or missing, the old bookmark cannot be automatically removed.

3. **JVM-Local Transfer Only:** The drag-and-drop system now only works within the same IntelliJ instance. Cross-instance or cross-application drag-and-drop is not supported (but was never intended).

---

## Future Improvements

1. **Cross-Topic Move:** Implement support for moving TopicLines between different Topics
2. **Bookmark Health Check:** Add a periodic check to detect orphaned bookmarks and offer automatic cleanup
3. **Undo/Redo Support:** Implement proper undo/redo for drag-and-drop operations
4. **Visual Feedback:** Add better visual indicators during drag operations (e.g., ghost images, drop indicators)

---

## Testing Checklist

- [x] Fix NotSerializableException by using JVM-local DataFlavor
- [x] Add detailed logging for bookmark removal operations
- [ ] Verify drag-and-drop works in TreeView without exceptions
- [ ] Verify TopicLines are correctly moved between groups in UI
- [ ] Verify old bookmarks are removed when editing line numbers
- [ ] Verify new bookmarks are created at correct line numbers
- [ ] Verify CodeRemarks are updated correctly
- [ ] Test with multiple TopicLines selected (batch operations)
- [ ] Test edge cases (invalid line numbers, missing files, etc.)
