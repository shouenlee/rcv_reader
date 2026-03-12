# RCV Bible Reader — Design Spec

## Overview

An offline Android app for reading the Recovery Version (RCV) of the Bible with inline expandable footnotes. The app bundles all 66 books of the Bible (1,190 chapter files) and ~15,000 footnotes into a pre-built SQLite database shipped in the APK.

**Target:** Android (Kotlin, Jetpack Compose)
**Version:** 1.0 — read-only, no search, no bookmarks, fully offline

---

## Data Model (SQLite + Room)

### Source Data

- **Verses:** `Verses/{Book}/{Chapter}` — one file per chapter, all verses inside
  - Format: `{verse_number}→{Book}. {Chapter}:{Verse} {verse_text}`
- **Footnotes:** `Footnotes/{Book}/{Chapter}/{Verse}/{NoteNum}` — one file per footnote
  - Format: `{number}→{Book}. {Chapter}:{Verse}.{NoteNum} - {keyword}: {footnote_text}`
- 66 books, plain text, ASCII encoding

### Database Schema

```sql
CREATE TABLE books (
    id INTEGER PRIMARY KEY,          -- 1-66, canonical order
    abbreviation TEXT NOT NULL,      -- "Gen", "Matt" (matches folder names)
    name TEXT NOT NULL,              -- "Genesis", "Matthew"
    testament TEXT NOT NULL,         -- "OT" or "NT"
    chapter_count INTEGER NOT NULL   -- 50, 28, etc.
);

CREATE TABLE verses (
    id INTEGER PRIMARY KEY,
    book_id INTEGER NOT NULL REFERENCES books(id),
    chapter INTEGER NOT NULL,
    verse_number INTEGER NOT NULL,
    text TEXT NOT NULL,              -- verse text only (no reference prefix)
    has_footnotes INTEGER NOT NULL DEFAULT 0  -- precomputed for UI dot indicator
);

CREATE TABLE footnotes (
    id INTEGER PRIMARY KEY,
    book_id INTEGER NOT NULL REFERENCES books(id),
    chapter INTEGER NOT NULL,
    verse_number INTEGER NOT NULL,
    footnote_number INTEGER NOT NULL,   -- ordering within a verse
    keyword TEXT NOT NULL,              -- "In", "beginning", "deep"
    content TEXT NOT NULL               -- full footnote text
);

-- Read performance indexes
CREATE INDEX idx_verses_lookup ON verses(book_id, chapter);
CREATE INDEX idx_footnotes_lookup ON footnotes(book_id, chapter, verse_number);

-- FTS5 tables for future search (created empty, populated later)
CREATE VIRTUAL TABLE verses_fts USING fts5(text, content=verses, content_rowid=id);
CREATE VIRTUAL TABLE footnotes_fts USING fts5(keyword, content, content=footnotes, content_rowid=id);
```

### Key Decisions

- **`has_footnotes` on verses** — avoids a JOIN to show the dot indicator on the reading screen
- **FTS5 tables created now** — zero runtime cost, ready for future search feature
- **`keyword` column** — extracted from footnote prefix (e.g., "In:" → "In"), useful for search and display
- **Canonical `books.id`** — 1=Genesis through 66=Revelation, drives sort order everywhere

### Build-Time Import

A Python script (`buildscripts/import_bible_data.py`) runs during the build process:
1. Reads all files from `Verses/` and `Footnotes/` directories
2. Parses the `→` delimited format
3. Strips the reference prefix from verse text (e.g., "Gen. 1:1 " removed, keeping only the verse body)
4. Extracts keyword from footnote text (text before the first `:`)
5. Writes a `bible.db` SQLite file
6. The `.db` file is placed in `app/src/main/assets/` and bundled in the APK

---

## Architecture

```
app/
├── data/
│   ├── db/
│   │   ├── BibleDatabase.kt        -- Room database, opens from assets
│   │   ├── BookDao.kt              -- getAllBooks(), getBookById()
│   │   ├── VerseDao.kt             -- getVersesForChapter(bookId, chapter)
│   │   └── FootnoteDao.kt          -- getFootnotesForVerse(bookId, chapter, verseNum)
│   ├── model/
│   │   ├── Book.kt                 -- Room entity
│   │   ├── Verse.kt                -- Room entity
│   │   └── Footnote.kt             -- Room entity
│   └── repository/
│       └── BibleRepository.kt      -- single data access layer
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                -- light + dark, follows system
│   │   ├── Color.kt                -- warm gold/brown palette
│   │   └── Type.kt                 -- serif typography (Georgia / Noto Serif)
│   ├── reading/
│   │   ├── ReadingScreen.kt        -- main screen: toolbar + verse list
│   │   ├── ReadingViewModel.kt     -- current book/chapter state, verse loading
│   │   ├── VerseItem.kt            -- single verse row, tap to expand
│   │   └── FootnoteSection.kt      -- expanded footnote list for a verse
│   └── navigation/
│       ├── BookPickerDropdown.kt   -- book grid, OT/NT grouped
│       └── ChapterPickerDropdown.kt -- chapter number grid
├── buildscripts/
│   └── import_bible_data.py        -- raw files → SQLite
└── assets/
    └── bible.db                    -- pre-built database
```

### Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Database:** Room (wrapping SQLite)
- **Async:** Kotlin Coroutines + Flow
- **State:** ViewModel + StateFlow
- **No DI framework** — manual construction, appropriate for single-screen app
- **Min SDK:** 26 (Android 8.0)

### Key Decisions

- **Single Activity, single screen** — no Jetpack Navigation needed
- **Room opens pre-built DB from assets** — using `createFromAsset("bible.db")`
- **ViewModel holds:** current book, current chapter, list of verses, expanded verse ID, loaded footnotes
- **Repository exposes Flows** — UI reacts to data changes automatically

---

## UI Design

### Visual Theme

- **Light + Dark mode**, follows system setting automatically
- **Serif typography** — Georgia or Noto Serif for verse text, system sans-serif for UI chrome
- **Warm palette:**
  - Light: off-white background (#FAF8F4), dark brown text (#2C2C2C), gold accents (#8B6914)
  - Dark: warm dark background (#1A1715), parchment text (#D4C5A9), gold accents (#C49B5E)
- **Line height:** 1.8-2.0x for comfortable reading
- **Generous padding** around verse text

### Reading Screen Layout

```
┌─────────────────────────────┐
│  [← Gen 49]                 │  ← previous chapter button (subtle, top-left)
│                             │
│    [Genesis ▾]  [1 ▾]       │  ← toolbar dropdowns, centered
│                             │
│  ¹ In the beginning God     │
│  created the heavens and    │
│  the earth. •               │  ← gold dot = has footnotes
│                             │
│  ² But the earth became     │
│  waste and emptiness, and   │
│  darkness was on the        │
│  surface of the deep... •   │
│                             │
│  ³ And God said, Let there  │
│  be light; and there was    │
│  light. •                   │
│                             │
│       [Next: Genesis 2 →]   │  ← next chapter button at bottom
└─────────────────────────────┘
```

### Verse with Expanded Footnotes

```
┌─────────────────────────────┐
│  ▎ ¹ In the beginning God   │  ← highlighted with gold left border
│  ▎ created the heavens and  │     and subtle gold background tint
│  ▎ the earth. •             │
│  ▎                          │
│  ▎ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │  ← dashed separator
│  ▎ In: The Bible, composed  │
│  ▎ of two testaments...     │  ← footnote keyword in gold, text in muted color
│  ▎                          │
│  ▎ beginning: The beginning │
│  ▎ of the heavens and the   │
│  ▎ earth was the beginning  │
│  ▎ of time...               │
│  ▎                          │
│  ▎ +3 more footnotes        │  ← or show all, TBD based on length
│  └──────────────────────────│
│                             │
│  ² But the earth became...  │  ← next verse pushed down
```

### Book Picker Dropdown

```
┌─────────────────────────────┐
│  OLD TESTAMENT              │
│  ┌─────┬─────┬─────┐       │
│  │ Gen │ Exo │ Lev │       │
│  ├─────┼─────┼─────┤       │
│  │ Num │Deut │Josh │       │
│  ├─────┼─────┼─────┤       │
│  │Judg │Ruth │1Sam │       │
│  └─────┴─────┴─────┘       │
│  ...                        │
│  NEW TESTAMENT              │
│  ┌─────┬─────┬─────┐       │
│  │Matt │Mark │Luke │       │
│  ├─────┼─────┼─────┤       │
│  │John │Acts │ Rom │       │
│  └─────┴─────┴─────┘       │
│  ...                        │
└─────────────────────────────┘
```

### Chapter Picker Dropdown

```
┌─────────────────────────────┐
│  ┌───┬───┬───┬───┬───┐     │
│  │ 1 │ 2 │ 3 │ 4 │ 5 │     │
│  ├───┼───┼───┼───┼───┤     │
│  │ 6 │ 7 │ 8 │ 9 │10 │     │
│  ├───┼───┼───┼───┼───┤     │
│  │11 │12 │...│   │50 │     │
│  └───┴───┴───┴───┴───┘     │
└─────────────────────────────┘
```

---

## Interaction Flow

### App Launch
1. Open to Genesis 1 (first launch) or last-read chapter (SharedPreferences)
2. Verses load from SQLite via Room
3. Verses with footnotes display a small gold dot indicator
4. All footnotes collapsed by default

### Reading
- Vertical scroll (LazyColumn) through verses
- Theme follows system light/dark automatically

### Tapping a Verse (with footnotes)
1. Verse background highlights with subtle gold tint + left border
2. Footnotes expand below the verse with smooth animation
3. Each footnote: **keyword** in gold, commentary in muted text color
4. Tap same verse again → collapse
5. Expanding a different verse → previous one collapses (single expansion)

### Tapping a Verse (without footnotes)
- No response — no visual feedback

### Book/Chapter Navigation
1. Tap book dropdown → scrollable grid of 66 books (OT/NT sections)
2. Tap a book → dropdown closes, chapter dropdown auto-opens
3. Tap chapter number → chapter loads, scroll resets to top
4. Toolbar always shows current book + chapter

### Sequential Navigation
- Bottom of chapter: "Next: [Book] [Chapter] →" button
- Top of chapter: "← [Book] [Chapter]" link
- Wraps across book boundaries (end of Malachi → Matthew 1)
- First chapter of Genesis has no "previous"; last chapter of Revelation has no "next"

### Persistence
- Last-read book + chapter saved to SharedPreferences on every navigation
- Restored on app launch

---

## Scope Boundaries (v1)

### In Scope
- Read all 66 books with verse display
- Tap-to-expand footnotes per verse
- Book/chapter toolbar dropdown navigation
- Previous/next chapter navigation
- Light + dark theme (system follow)
- Fully offline, all data bundled
- Pre-built SQLite database with FTS5 tables (empty)

### Out of Scope (future versions)
- Full-text search
- Bookmarks / highlights
- Cross-reference navigation (tapping verse refs in footnotes)
- Reading plans
- Font size / spacing settings
- Share verse functionality
- Landscape / tablet split-view layout
