# CLAUDE.md

## What This Project Is

Offline Android Bible reader app (Recovery Version). Kotlin + Jetpack Compose + Room. All 66 books, 31K verses, 15K footnotes bundled in a pre-built SQLite database shipped in the APK. No network access.

## Build Commands

```bash
# Build (requires JDK 17)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug

# Regenerate bible.db from raw data (requires Verses/ and Footnotes/ dirs)
python3 buildscripts/import_bible_data.py

# Run Python tests
python3 -m pytest buildscripts/test_import.py -v
```

## Architecture

Single Activity, single screen, no navigation library. Data flows one way:

```
Room DB (assets/bible.db)
  → DAOs (Flow/suspend)
    → BibleRepository
      → ReadingViewModel (StateFlow<ReadingUiState>)
        → ReadingScreen (Compose)
```

### Key files

| File | What it does |
|------|-------------|
| `app/.../MainActivity.kt` | Entry point. Wraps ReadingScreen in RCVReaderTheme. |
| `app/.../data/model/Book.kt` | Room entity. Fields: id, abbreviation, name, testament, chapterCount. Uses `@ColumnInfo` for snake_case DB mapping. |
| `app/.../data/model/Verse.kt` | Room entity. `hasFootnotes` is Boolean (maps to INTEGER 0/1). Has `@ForeignKey` to Book and `@Index` named `idx_verses_lookup`. |
| `app/.../data/model/Footnote.kt` | Room entity. `keyword` is nullable. Has `@ForeignKey` to Book and `@Index` named `idx_footnotes_lookup`. |
| `app/.../data/db/BibleDatabase.kt` | Room singleton. `createFromAsset("bible.db")`, `fallbackToDestructiveMigration()`. |
| `app/.../data/db/BookDao.kt` | `getAllBooks(): Flow<List<Book>>`, `getBookById(): suspend` |
| `app/.../data/db/VerseDao.kt` | `getVersesForChapter(): Flow<List<Verse>>` |
| `app/.../data/db/FootnoteDao.kt` | `getFootnotesForVerse(): suspend List<Footnote>` |
| `app/.../data/repository/BibleRepository.kt` | Thin facade over all three DAOs. |
| `app/.../ui/reading/ReadingViewModel.kt` | State: books, currentBook, currentChapter, verses, expandedVerseId, expandedFootnotes, previousChapter, nextChapter, pendingBook. Key methods: `navigateTo()`, `selectBook()`, `toggleVerse()`. Persists last position to SharedPreferences. Cancels previous `versesJob` before starting new collection. Uses `Flow.first()` in init (not collect) to avoid re-navigation. |
| `app/.../ui/reading/ReadingScreen.kt` | Main composable. Prev/next chapter links at top, book name + chapter pill trigger bar, LazyColumn of VerseItems, next chapter button at bottom. Opens NavigationBottomSheet. |
| `app/.../ui/reading/VerseItem.kt` | Verse row. Superscript verse number, gold dot for footnotes, tap to expand. Gold left border when expanded. Only clickable if `hasFootnotes`. |
| `app/.../ui/reading/FootnoteSection.kt` | AnimatedVisibility. Gold keyword prefix, muted content text. |
| `app/.../ui/navigation/NavigationBottomSheet.kt` | ModalBottomSheet with TabRow (Books/Chapters). Books: 3-col grid, OT/NT sections. Chapters: 6-col square grid. Selecting a book auto-switches to Chapters tab. Accepts `initialTab` param so chapter button opens directly to chapters. |
| `app/.../ui/theme/Color.kt` | Light: bg #FAF8F4, primary #8B6914. Dark: bg #1A1715, primary #C49B5E. Shared: GoldAccent, VerseDotColor, FootnoteHighlight. |
| `app/.../ui/theme/Type.kt` | Serif body text (16sp/30sp line height). Sans-serif for titles/labels. |
| `app/.../ui/theme/Theme.kt` | `RCVReaderTheme` composable. Follows `isSystemInDarkTheme()`. |
| `buildscripts/import_bible_data.py` | Parses raw Verses/ and Footnotes/ dirs into SQLite. Handles 4 verse format variants. BOOK_MAP has 66 entries. Psalms superscriptions are verse_number=0. |
| `buildscripts/test_import.py` | 15 pytest tests covering book mapping, verse extraction, footnote parsing, integration. |

## Critical Constraints

1. **Room schema sync**: The pre-built `bible.db` schema MUST exactly match Room's generated SQL (backtick-quoted identifiers, explicit `ON UPDATE NO ACTION ON DELETE NO ACTION` on foreign keys, no DEFAULT clauses, exact index names). If you change Room entities, update `CREATE_TABLES_SQL` in `import_bible_data.py` and regenerate.

2. **Index names matter**: Room validates index names. `Verse` entity declares `@Index(name = "idx_verses_lookup")` and `Footnote` declares `@Index(name = "idx_footnotes_lookup")`. These must match the database.

3. **JDK 17 required**: Kotlin 2.1.0 + AGP 8.7.3 requires JDK 17. JDK 25 causes ASM ClassReader errors.

4. **Verse expansion uses verse ID** (not verse_number) for robustness. The `expandedVerseId` in `ReadingUiState` maps to `Verse.id`.

5. **Book selection is two-step**: `selectBook()` sets `pendingBook`, navigation deferred until chapter is picked. This prevents loading chapter 1 prematurely.

## Tech Versions

Kotlin 2.1.0, AGP 8.7.3, KSP 2.1.0-1.0.29, Compose BOM 2024.12.01, Room 2.6.1, Lifecycle 2.8.7, compileSdk 35, minSdk 26, JDK 17.
