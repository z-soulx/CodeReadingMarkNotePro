# Search Feature User Guide

## Overview

The plugin now includes a powerful intelligent search feature that supports:
- ✅ Direct text matching
- ✅ Pinyin search (initial letters and full pinyin)
- ✅ Fuzzy search (based on edit distance)
- ✅ Lightweight vector-style search (character overlap similarity)

## How to Use

### 1. Open Search Interface

1. Open the plugin tool window
2. Click the **"Search"** tab
3. Type keywords in the search box

### 2. Search Examples

Assuming you have the following TopicLine notes:
- "如家的1"
- "如家的2"
- "如家酒店服务"

**Direct Match:**
- Input `如家` → Matches all results containing "如家"

**Pinyin Initials:**
- Input `rjd` → Matches "如家的1", "如家的2"

**Full Pinyin:**
- Input `rujia` → Matches "如家"

**Fuzzy Match:**
- Input `如家1` → Also matches "如家的2" (similar edit distance)

### 3. Interaction Methods

#### Method 1: Double-click to Navigate to Code
- **Double-click** on a search result → Immediately opens the corresponding code file in the editor and navigates to the specific line

#### Method 2: Right-click Context Menu
- **Right-click** on a search result → Shows context menu:
  - **Navigate to Code** 📝 - Opens code in editor
  - **Locate in Tree View (Switch to Tree)** 📍 - Expands and highlights the corresponding TopicLine in tree view

#### Method 3: Keyboard Shortcuts
- **Enter** key - Navigate to code
- **↓** key (in search box) - Move focus to result list

### 4. Search Result Display

Each search result shows the following information:
```
[Topic Name] ▸ [Group Name] ▸ Note (File Path:Line) Similarity%
```

- **Topic Name** - Displayed in blue
- **Group Name** - Displayed in purple (or gray "Ungrouped" if not in a group)
- **Note** - Displayed in bold
- **File Path:Line** - Displayed in gray
- **Similarity Score** - Color-coded:
  - 🟢 Green (>70%) - High match
  - 🟠 Orange (40-70%) - Medium match
  - 🔴 Red (<40%) - Low match

### 5. Status Bar Information

The status bar below the search box displays:
- Number of results: `Found X result(s)`
- No results: `No matching results found`
- Error messages (if any): `Search error: ...`

## Technical Features

### Similarity Algorithm

The search service uses multiple algorithms for comprehensive scoring:

1. **Exact Match** (100 points) - Text is identical
2. **Contains Match** (60-100 points) - Scored based on position and coverage
3. **Pinyin Match** (50-55 points) - Pinyin initials or full pinyin match
4. **Edit Distance** (0-40 points) - Levenshtein distance algorithm
5. **Character Overlap** (0-30 points) - Character overlap ratio

The highest score is used as the similarity rating.

### Real-time Search

- Search is triggered automatically as you type
- Results are sorted by similarity in descending order
- Only displays results with score > 10%

## Notes

1. **Search Scope**: Only searches TopicLine notes (Chinese annotations), not the code itself
2. **Performance**: With many TopicLines, search may take a moment (executed asynchronously in background)
3. **Auto-update**: Search data automatically updates when Topics or TopicLines change

## FAQ

**Q: Why can't I find certain content?**
A: Ensure the TopicLine has a note (annotation); search only matches the note field.

**Q: Double-click doesn't work?**
A: Ensure the corresponding code file still exists and the path is correct.

**Q: How to clear the search?**
A: Click the ✕ button on the right side of the search box, or directly delete text in the search box.

## Keyboard Shortcuts Summary

| Action | Shortcut |
|--------|----------|
| Navigate to code | Double-click or Enter |
| Show context menu | Right-click |
| Move to result list | ↓ (in search box) |
| Clear search | Click ✕ button |

## UI Text Reference

- **Tab Names**: "Tree View" and "Search"
- **Search Box Placeholder**: "Search TopicLine notes... (Supports Pinyin/Fuzzy search)"
- **Clear Button Tooltip**: "Clear search"
- **Context Menu Items**:
  - "Navigate to Code"
  - "Locate in Tree View (Switch to Tree)"
- **Status Messages**:
  - "Found X result(s)"
  - "No matching results found"
  - "Search error: ..."
- **Result Display**: "[Topic] ▸ [Group/Ungrouped] ▸ Note (Path:Line) Score%"

