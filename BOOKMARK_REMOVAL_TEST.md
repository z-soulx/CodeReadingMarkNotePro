# Bookmark Removal Bug Fix - Testing Guide

## Bug Description
**Issue:** When editing a TopicLine's line number (e.g., from line 53 to 55), the old bookmark at line 53 remained in the editor, and a new bookmark was created at line 55, resulting in duplicate bookmarks.

## Root Cause
The `BookmarkUtils.removeMachBookmark()` method was only calling `group.remove(bookmark)` to remove the bookmark from the BookmarkGroup, but NOT calling `BookmarksManager.remove(bookmark)`. 

In IntelliJ's new Bookmark API (com.intellij.ide.bookmark.*), you must perform BOTH operations:
1. `BookmarkGroup.remove(bookmark)` - removes from the group's internal list
2. `BookmarksManager.remove(bookmark)` - actually deletes the bookmark from the editor UI

## The Fix
Added the missing `manager.remove(bookmark)` call in `BookmarkUtils.removeMachBookmark()`:

```java
// Step 1: Remove from group
boolean removedFromGroup = group.remove(bookmark);

// Step 2: Remove from BookmarksManager (this actually deletes from editor)
manager.remove(bookmark);
```

## Testing Steps

### Prerequisites
1. Build the plugin: `./gradlew buildPlugin`
2. Run IDE with plugin: `./gradlew runIde`
3. Enable debug logging (optional): Add VM option `-Didea.log.debug.categories=jp.kitabatakep.intellij.plugins.codereadingnote`

### Test Case 1: Basic Line Number Edit
1. Open any Java/text file in the IDE
2. Create a new Topic in Code Reading Note tool window
3. Select a line (e.g., line 53) and add it to the topic
   - Right-click → "Add to Topic" → Select your topic
   - Verify a bookmark appears at line 53 in the editor gutter
4. In the Topic detail panel, right-click the TopicLine → "Edit Line Number"
5. Change from 53 to 55
6. Click OK
7. **Expected Result:**
   - ✅ Line 53 should have NO bookmark (old bookmark deleted)
   - ✅ Line 55 should have ONE bookmark (new bookmark created)
   - ✅ The CodeRemark comment should appear at line 55, not line 53

### Test Case 2: Multiple Line Number Changes
1. Create a TopicLine at line 10
2. Edit line number: 10 → 20 (verify old bookmark at line 10 is gone)
3. Edit line number again: 20 → 30 (verify old bookmark at line 20 is gone)
4. Edit line number again: 30 → 15 (verify old bookmark at line 30 is gone)
5. **Expected Result:** Only ONE bookmark should exist, always at the current line number

### Test Case 3: Edit Line Number with "Update Bookmark" Disabled
1. Create a TopicLine at line 50
2. Right-click → "Edit Line Number"
3. **Uncheck** "Update native bookmark"
4. Change to line 60
5. Click OK
6. **Expected Result:**
   - ✅ Old bookmark at line 50 should remain (because we disabled update)
   - ✅ No new bookmark at line 60
   - ✅ TopicLine in the list shows line 60

### Test Case 4: Batch Line Number Adjustment
1. Add multiple TopicLines to a topic (e.g., lines 10, 20, 30, 40)
2. Select all TopicLines (Ctrl+A or click multiple while holding Ctrl)
3. Right-click → "Batch Adjust Line Numbers"
4. Select "Add offset" and enter "+5"
5. Click OK
6. **Expected Result:**
   - ✅ Lines 10, 20, 30, 40 should have NO bookmarks (old bookmarks deleted)
   - ✅ Lines 15, 25, 35, 45 should have bookmarks (new bookmarks created)

### Test Case 5: Check IDE Log (Debug)
1. Enable debug logging: `Help` → `Diagnostic Tools` → `Debug Log Settings`
   - Add: `jp.kitabatakep.intellij.plugins.codereadingnote`
2. Edit a TopicLine's line number
3. Check log: `Help` → `Show Log in Explorer`
4. Search for bookmark-related log entries

**Expected Log Output:**
```
INFO - BookmarkUtils - Found bookmark to remove with UUID: xxx-xxx-xxx at line: 53
INFO - BookmarkUtils - Removed from group: true
INFO - BookmarkUtils - Removed from BookmarksManager
INFO - LineNumberUpdateService - Removed old bookmark at line 53: true
INFO - LineNumberUpdateService - Created new bookmark at line: 55
INFO - LineNumberUpdateService - Updated line number: file:///path/to/file.java:53 -> 55
```

## Verification Checklist

- [ ] Old bookmark is completely removed from editor gutter
- [ ] New bookmark appears at the correct new line number
- [ ] Only ONE bookmark exists per TopicLine
- [ ] CodeRemark (inline comment) moves to the new line
- [ ] No error messages in IDE log
- [ ] Bookmarks visible in IntelliJ's native Bookmarks tool window (View → Tool Windows → Bookmarks)
- [ ] Bookmark appears under "Code Reading Mark Note Pro" group

## Common Issues

### If old bookmark still remains:
1. Check IDE log for `"No bookmark found to remove with UUID..."` message
   - This means the UUID matching failed
   - Possible cause: TopicLine's bookmarkUid is null or doesn't match
2. Check if `group.remove()` returned false
   - This means the bookmark wasn't in the group
3. Verify `manager.remove(bookmark)` was called
   - If this line is missing, old bookmark will remain

### If new bookmark not created:
1. Check IDE log for `"Failed to recreate bookmark at new line: XX"`
2. Verify the file is still valid and open
3. Verify the new line number is within file bounds

### If CodeRemark doesn't move:
1. Check that the file is currently open in an editor
2. The `EditorUtils.removeLineCodeRemark()` uses the old line number (before modification)
3. The `EditorUtils.addLineCodeRemark()` uses the new line number (after modification)

## Technical Details

### IntelliJ Bookmark API (New)
- Package: `com.intellij.ide.bookmark.*`
- Key classes: `BookmarksManager`, `BookmarkGroup`, `Bookmark`
- Deletion requires TWO steps:
  1. `BookmarkGroup.remove(Bookmark)` - removes from group
  2. `BookmarksManager.remove(Bookmark)` - removes from editor

### Old API (Deprecated)
- Package: `com.intellij.ide.bookmarks.*` (note the 's')
- This plugin uses reflection to create bookmarks using the old API
- Then wraps them with the new API for management

## Files Modified in This Fix
- `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/remark/BookmarkUtils.java`
  - Method: `removeMachBookmark(TopicLine, Project)`
  - Added: `manager.remove(bookmark)` call

## Related Code Flow

When editing line number via `EditLineNumberDialog`:
1. User enters new line number and clicks OK
2. `EditLineNumberAction.actionPerformed()` is called
3. Calls `LineNumberUpdateService.updateLineNumber()`
4. Sequence:
   - `BookmarkUtils.removeMachBookmark()` - **removes old bookmark** ✅ FIXED
   - `EditorUtils.removeLineCodeRemark()` - removes old remark
   - `line.modifyLine(newLineNum)` - updates TopicLine object
   - `BookmarkUtils.addBookmark()` - creates new bookmark
   - `EditorUtils.addLineCodeRemark()` - adds new remark

## Success Criteria
✅ After editing a TopicLine from line 53 to 55:
- Line 53 has NO bookmark
- Line 55 has ONE bookmark
- CodeRemark appears at line 55 only
- No errors in IDE log
- Changes persist after restarting IDE

