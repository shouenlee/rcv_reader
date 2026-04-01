# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

Bible reader (Recovery Version) available as an Android app and a static website. Same `bible.db` (66 books, 31K verses, 15K footnotes) powers both. No network access required after initial load.

## Build Commands

```bash
# Android (requires JDK 17)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug

# Web (local dev)
cp app/src/main/assets/bible.db web/bible.db
cd web && python3 -m http.server 8000
# Open http://localhost:8000
# Smoke tests: http://localhost:8000/test.html

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

Two separate SharedPreferences stores:
- `"rcv_reader"` — last-read position (book ID + chapter) + serialized back/forward nav stacks, managed by `ReadingViewModel`
- `"rcv_reader_settings"` — user preferences (theme mode, text size), managed by `SettingsViewModel`

### Key files

| File | What it does |
|------|-------------|
| `app/.../MainActivity.kt` | Entry point. Creates `SettingsViewModel`, resolves theme mode (System/Light/Dark), wraps `ReadingScreen` in `RCVReaderTheme`. |
| `app/.../data/model/Book.kt` | Room entity. Fields: id, abbreviation, name, testament, chapterCount. Uses `@ColumnInfo` for snake_case DB mapping. |
| `app/.../data/model/Verse.kt` | Room entity. `hasFootnotes` is Boolean (maps to INTEGER 0/1). Has `@ForeignKey` to Book and `@Index` named `idx_verses_lookup`. |
| `app/.../data/model/Footnote.kt` | Room entity. `keyword` is nullable. Has `@ForeignKey` to Book and `@Index` named `idx_footnotes_lookup`. |
| `app/.../data/db/BibleDatabase.kt` | Room singleton. `createFromAsset("bible.db")`, `fallbackToDestructiveMigration()`. |
| `app/.../data/db/BookDao.kt` | `getAllBooks(): Flow<List<Book>>`, `getBookById(): suspend` |
| `app/.../data/db/VerseDao.kt` | `getVersesForChapter(): Flow<List<Verse>>` |
| `app/.../data/db/FootnoteDao.kt` | `getFootnotesForVerse(): suspend List<Footnote>` |
| `app/.../data/db/SearchDao.kt` | Raw FTS5 queries via `@RawQuery`. Used by `BibleRepository.search()` for verse and footnote full-text search. |
| `app/.../data/db/BookmarkDatabase.kt` | Separate Room singleton for user bookmarks. Stored in `bookmarks.db` (not `bible.db`). Version 2, `fallbackToDestructiveMigration()`. |
| `app/.../data/db/BookmarkDao.kt` | Queries bookmarked verse/footnote IDs by book+chapter (for per-chapter highlight), plus CRUD for `Bookmark` rows. |
| `app/.../data/model/SearchResult.kt` | Plain data class (not a Room entity). Fields: id, bookId, bookName, abbreviation, chapter, verseNumber, text, keyword, tier (1=exact phrase / 2=all words / 3=any word). |
| `app/.../data/model/SearchScope.kt` | Enum: `ALL`, `OT`, `NT`, `THIS_BOOK`. |
| `app/.../data/model/Bookmark.kt` | Room entity (`bookmarks` table). `type` is `"verse"` or `"footnote"`. Stores denormalized bookName, abbreviation, chapter, verseNumber, previewText, and optional keyword for footnote bookmarks. |
| `app/.../data/repository/BibleRepository.kt` | Facade over BookDao, VerseDao, FootnoteDao, SearchDao. `search()` runs tiered FTS5 queries (exact phrase → all words → any word), interleaves verse + footnote results, caps at 200. `sanitizeQuery()` strips FTS5 special chars and tokens < 2 chars. |
| `app/.../data/repository/BookmarkRepository.kt` | `toggleVerseBookmark()` / `toggleFootnoteBookmark()` (insert or delete). `getBookmarkedVerseIds()` / `getBookmarkedFootnoteIds()` return `Flow<Set<Int>>` for the current chapter. `getAllBookmarks()` returns all rows as Flow. |
| `app/.../ui/reading/ReadingViewModel.kt` | State: books, currentBook, currentChapter, verses, expandedVerseId, expandedFootnotes, previousChapter, nextChapter, pendingBook, bookmarkedVerseIds, bookmarkedFootnoteIds, scrollToVerseNumber, autoExpandFootnoteVerseNumber, backStack, forwardStack. Key methods: `navigateTo()`, `navigateBack()`, `navigateForward()`, `navigateToVerse()`, `selectBook()`, `toggleVerse()`, `toggleVerseBookmark()`, `toggleFootnoteBookmark()`, `clearScrollTarget()`. Back/forward stacks capped at 50, serialized to SharedPreferences as `"bookId:chapter"` pairs joined by `\|`. |
| `app/.../ui/reading/ReadingScreen.kt` | Main composable. Animated collapsing top nav (2-row expanded → 1-row compact on scroll via `compactProgress`). LazyColumn of VerseItems. Back/forward history buttons bottom-left. Settings icon bottom-right → opens `SettingsDialog`. Long-press verse/footnote → toggles bookmark. `navigateToVerse()` scrolls to a verse and optionally auto-expands its footnotes. |
| `app/.../ui/reading/VerseItem.kt` | Verse row. Superscript verse number, gold dot for footnotes, tap to expand. Gold left border when expanded. Long-press to bookmark. Only tappable if `hasFootnotes`. |
| `app/.../ui/reading/FootnoteSection.kt` | AnimatedVisibility. Gold keyword prefix, muted content text. Long-press to bookmark individual footnotes. |
| `app/.../ui/navigation/NavigationBottomSheet.kt` | ModalBottomSheet with TabRow (Books/Chapters). Books: 3-col grid, OT/NT sections. Chapters: 6-col square grid. Selecting a book auto-switches to Chapters tab. Accepts `initialTab` param so chapter button opens directly to chapters. |
| `app/.../ui/bookmarks/BookmarkViewModel.kt` | Lists all bookmarks via `BookmarkRepository`. `expandedBookmarkId` for accordion expansion. `deleteBookmark()`. |
| `app/.../ui/bookmarks/BookmarkScreen.kt` | Full-screen bookmark list. Tapping a bookmark calls `ReadingViewModel.navigateToVerse()` to jump there. |
| `app/.../ui/bookmarks/BookmarkItem.kt` | Single bookmark row. Shows book/chapter/verse reference, preview text, keyword for footnote bookmarks. Swipe or button to delete. |
| `app/.../ui/search/SearchViewModel.kt` | `SearchUiState` with query, scope, includeFootnotes, results, isSearching. Debounces input 300ms before querying. Scope: ALL/OT/NT/THIS_BOOK. |
| `app/.../ui/search/SearchScreen.kt` | Search UI. Text field, scope chips, footnote toggle, result list. Tapping a result calls `ReadingViewModel.navigateToVerse()`. |
| `app/.../ui/search/SearchResultItem.kt` | Single search result row showing reference and matched text. |
| `app/.../ui/settings/UserSettings.kt` | Data class + enums: `ThemeMode` (SYSTEM/LIGHT/DARK), `TextSize` (SMALL 15sp/MEDIUM 17sp/LARGE 20sp). |
| `app/.../ui/settings/SettingsViewModel.kt` | `AndroidViewModel` managing `UserSettings` via `rcv_reader_settings` SharedPreferences. Exposes `StateFlow<UserSettings>`. |
| `app/.../ui/settings/SettingsDialog.kt` | Modal `Dialog` (not a drawer). Theme mode chips and text size chips with gold accent styling. Opened from settings icon in `ReadingScreen`. |
| `app/.../ui/theme/Color.kt` | Light: bg #FAF8F4, primary #8B6914. Dark: bg #1A1715, primary #C49B5E. Shared: GoldAccent, VerseDotColor, FootnoteHighlight. |
| `app/.../ui/theme/Type.kt` | Serif body text (16sp/30sp line height). Sans-serif for titles/labels. |
| `app/.../ui/theme/Theme.kt` | `RCVReaderTheme` composable. Accepts `darkTheme` param (resolved in `MainActivity` from `SettingsViewModel`). |
| `buildscripts/import_bible_data.py` | Parses raw Verses/ and Footnotes/ dirs into SQLite. Handles 4 verse format variants. BOOK_MAP has 66 entries. Psalms superscriptions are verse_number=0. |
| `buildscripts/test_import.py` | 15 pytest tests covering book mapping, verse extraction, footnote parsing, integration. |

## Critical Constraints

1. **Room schema sync**: The pre-built `bible.db` schema MUST exactly match Room's generated SQL (backtick-quoted identifiers, explicit `ON UPDATE NO ACTION ON DELETE NO ACTION` on foreign keys, no DEFAULT clauses, exact index names). If you change Room entities, update `CREATE_TABLES_SQL` in `import_bible_data.py` and regenerate.

2. **Index names matter**: Room validates index names. `Verse` entity declares `@Index(name = "idx_verses_lookup")` and `Footnote` declares `@Index(name = "idx_footnotes_lookup")`. These must match the database.

3. **JDK 17 required**: Kotlin 2.1.0 + AGP 8.7.3 requires JDK 17. JDK 25 causes ASM ClassReader errors.

4. **Verse expansion uses verse ID** (not verse_number) for robustness. The `expandedVerseId` in `ReadingUiState` maps to `Verse.id`.

5. **Book selection is two-step**: `selectBook()` sets `pendingBook`, navigation deferred until chapter is picked. This prevents loading chapter 1 prematurely.

6. **FTS5 virtual tables required**: `bible.db` must contain `verses_fts` and `footnotes_fts` virtual tables. Search uses `@RawQuery` with BM25 ranking (`ORDER BY bm25(...)`). If you regenerate `bible.db`, ensure these tables are included in `CREATE_TABLES_SQL` in `import_bible_data.py`.

7. **Two Room databases**: `bible.db` (read-only asset, `BibleDatabase`) and `bookmarks.db` (user-writable, `BookmarkDatabase`) are separate singleton instances. Never mix their DAOs.

## Web Version Key Files

| File | What it does |
|------|-------------|
| `web/index.html` | Entry point. Import map for Preact/HTM CDN. Inline loading screen with progress bar. Loads sql.js UMD from jsdelivr. |
| `web/db.js` | sql.js init with streaming progress. Exports `initDb()`, `getBooks()`, `getVersesForChapter()`, `getFootnotesForVerse()`. |
| `web/hooks/useReading.js` | State hook mirroring ReadingViewModel. `navigateTo()`, `selectBook()`, `toggleVerse()`, `computeAdjacentChapter()`. Persists to localStorage with Safari try/catch. |
| `web/components/ReadingScreen.js` | Main layout. Desktop sidebar + mobile trigger bar + verse list + navigation modal. |
| `web/components/VerseItem.js` | Verse row. CSS class toggling for expansion (not AnimatedVisibility). |
| `web/components/FootnoteSection.js` | Gold keyword prefix, muted content. |
| `web/components/NavigationModal.js` | Mobile slide-up modal with Books/Chapters tabs. Exports shared sub-components (SectionLabel, BookGrid, ChaptersGrid). |
| `web/components/SidebarNavigation.js` | Desktop tabbed sidebar. Reuses sub-components from NavigationModal. |
| `web/sw.js` | Service worker. Cache-first for bible.db + CDN. Stale-while-revalidate for app code. Posts UPDATE_AVAILABLE message. |
| `web/app.js` | Bootstrap: initDb → hide loading → render App. Error/retry UI. SW registration + update banner listener. |
| `.github/workflows/deploy.yml` | Copies bible.db to web/ and deploys to GitHub Pages on push to main. |

## Web Critical Constraints

1. **Single Preact instance**: The import map uses `?external=preact` on `htm/preact` to prevent htm from bundling its own Preact copy. Without this, hooks crash.
2. **sql.js must use UMD build**: esm.sh's ESM transform of sql.js injects Node.js `fs` polyfills that crash in browsers. Load `sql-wasm.js` from jsdelivr via `<script>` tag instead.
3. **Footnote queries use verse_number, not verse.id**: IDs are auto-incremented globally and diverge from verse_number after Genesis 1. `toggleVerse()` passes `verse.verse_number` to `getFootnotesForVerse()`.
4. **bible.db is a deploy artifact**: Not committed in `web/` (listed in `web/.gitignore`). Copied from `app/src/main/assets/` by the GitHub Actions workflow at deploy time.

## Tech Versions

**Android:** Kotlin 2.1.0, AGP 8.7.3, KSP 2.1.0-1.0.29, Compose BOM 2024.12.01, Room 2.6.1, Lifecycle 2.8.7, compileSdk 35, minSdk 26, JDK 17.

**Web:** Preact 10.25.4, HTM 3.1.1, sql.js 1.12.0 (all CDN, no npm). ES modules via import map. No build step.
