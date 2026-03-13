# Search Feature Design

## Goal

Add full-text search to the RCV Bible reader app. Users can search across all 31,103 verses (and optionally footnotes), filter by testament or current book, and navigate directly to any result.

## Navigation & Entry Points

**Reading ↔ Search:** The app uses a two-pane horizontal layout in `MainActivity`. The reading screen sits at offset 0; the search screen sits just off-screen to the right. The swipe gesture is detected in `MainActivity` (not in `ReadingScreen`) using `detectHorizontalDragGestures` on the outer `Box`. A swipe-left gesture (≥60dp threshold) slides the search screen into view; swipe-right returns to reading. `ReadingScreen` no longer contains any swipe gesture detection — its existing `pointerInput` swipe block is removed entirely. This avoids gesture conflicts between the child and parent.

**Settings:** The existing swipe-left-opens-settings gesture is intentionally removed — swipe-left now opens search. The `SettingsPanel` side-drawer is replaced by a `SettingsDialog` (a standard Compose `Dialog`). A ⚙ icon button at the bottom-right of the reading screen is the only way to open settings. The dialog contains the same theme and text size controls as the current panel. `SettingsDialog` is called from within `ReadingScreen`, receiving the same `settings: UserSettings`, `onThemeChange`, `onTextSizeChange`, and `onDismiss` parameters that `SettingsPanel` currently receives — the wiring through `ReadingScreen`'s parameter list is unchanged.

**Result navigation:** Tapping a result calls `readingViewModel.navigateTo(bookId, chapter)` and slides back to the reading screen. The search screen preserves its state (query, filters, scroll position) regardless of how the user exits — both swiping right and tapping Cancel preserve the current search state. Cancel is purely a navigation action (equivalent to swipe-right); it does not clear the query or results.

## Search Screen Layout

Top to bottom:
1. **Search bar** — auto-focused when screen opens. Gold magnifying glass icon. "Cancel" button dismisses search and returns to reading.
2. **Scope chips** — `All · OT · NT · This Book`. Pill-shaped, gold-highlighted when selected. "This Book" is disabled when `currentBookId` is null.
3. **Footnotes toggle** — off by default. Row with label "Include footnotes" and a toggle switch.
4. **Status row** — while `isSearching = true`, shows a `CircularProgressIndicator`. Once results arrive, shows small muted result count text (e.g. "14 results"). Hidden when query is empty.
5. **Results list** — `LazyColumn` of `SearchResultItem` composables.
6. **Empty state** — open book icon + "Type to search across 31,103 verses" shown before any query.

## Search Result Item

Each row shows:
- **Reference** in gold, small caps: e.g. `John 1:14`
- **Text** with matched words highlighted (gold tint background, bold weight) using `AnnotatedString`. For verse results this is the verse text; for footnote results this is the footnote body text, with the footnote keyword (if non-null) shown as a gold prefix matching `FootnoteSection.kt`'s existing style.
- Tap navigates to that verse's chapter in the reading screen.

## Search Algorithm

Input: raw query string (e.g. `"grace and truth"`).

All short-query logic and token stripping lives in `BibleRepository.search()` — `SearchViewModel` passes the raw query string unchanged. The repository strips tokens shorter than 2 characters before building FTS5 query strings. It also strips FTS5 special characters (`"`, `*`, `(`, `)`, `-`) from individual tokens to prevent malformed queries that would crash at runtime. If no tokens remain after stripping, an empty list is returned immediately without hitting the database.

The repository runs three FTS5 queries in sequence and merges results:

| Tier | FTS5 query | Description |
|------|-----------|-------------|
| 1 | `"grace and truth"` (quoted) | Exact phrase match — highest rank |
| 2 | `grace AND truth` | All words present, any order |
| 3 | `grace OR truth` | Any words present, ranked by `bm25()` |

Each tier's results exclude verse IDs already found in higher tiers. Final list = tier1 + tier2 + tier3, capped at 200 total results across the merged list.

When footnote search is enabled, the same 3-tier logic runs against `footnotes_fts` and results are appended after verse results.

## Scope Filtering

A `WHERE` clause is appended to all queries based on the selected scope chip:
- **All** — no filter
- **OT** — `AND b.testament = 'OT'`
- **NT** — `AND b.testament = 'NT'`
- **This Book** — `AND v.book_id = :currentBookId`

`currentBookId` is kept current in `SearchViewModel` via `fun setCurrentBookId(id: Int?)`. `MainActivity` calls this on the main thread by observing `readingViewModel.uiState` via `collectAsStateWithLifecycle` — this runs on the main thread by definition, so no threading concern.

## Database Changes

Two FTS5 virtual tables are added to `import_bible_data.py` and the pre-built `bible.db`:

```sql
CREATE VIRTUAL TABLE verses_fts USING fts5(text, content=verses, content_rowid=id);
INSERT INTO verses_fts(verses_fts) VALUES('rebuild');

CREATE VIRTUAL TABLE footnotes_fts USING fts5(content, content=footnotes, content_rowid=id);
INSERT INTO footnotes_fts(footnotes_fts) VALUES('rebuild');
```

Content tables keep the FTS index in sync with the source rows. Since the database is read-only at runtime (pre-built asset), no triggers are needed.

## New Files

| File | Responsibility |
|------|---------------|
| `data/model/SearchResult.kt` | Data class: `bookId`, `bookName`, `abbreviation`, `chapter`, `verseNumber`, `text`, `keyword: String?` (non-null for footnote results — the footnote keyword shown as gold prefix), `tier: Int` (1/2/3 — merge ordering only, not rendered) |
| `data/db/SearchDao.kt` | Three `@RawQuery` methods that accept a pre-built `SupportSQLiteQuery` and return `List<SearchResult>`. Room's `@RawQuery` is used because FTS5 `MATCH` syntax cannot be expressed in `@Query` parameters (note: no compile-time SQL validation — queries must be manually verified). Annotated `@RawQuery(observedEntities = [Verse::class, Book::class])`. Query strings are constructed in `BibleRepository`, not in the DAO. |
| `ui/search/SearchViewModel.kt` | State backed by `MutableStateFlow`: `query: MutableStateFlow<String>`, `scope: MutableStateFlow<SearchScope>` (enum: ALL/OT/NT/THIS_BOOK), `includeFootnotes: MutableStateFlow<Boolean>`, `results: MutableStateFlow<List<SearchResult>>`, `isSearching: MutableStateFlow<Boolean>`. Search is triggered automatically with a **300ms debounce** by combining the flows: `combine(query, scope, includeFootnotes) {...}.debounce(300).collectIn(viewModelScope)`. `fun setCurrentBookId(id: Int?)` updates a private `MutableStateFlow<Int?>` that feeds into the combined flow. |
| `ui/search/SearchScreen.kt` | Full-page search composable: bar, chips, toggle, status row, results list |
| `ui/search/SearchResultItem.kt` | Single result row with highlighted text; renders `keyword` prefix in gold if non-null |
| `ui/settings/SettingsDialog.kt` | `SettingsPanel` content wrapped in a Compose `Dialog` |

## Modified Files

| File | Change |
|------|--------|
| `data/db/BibleDatabase.kt` | Register `SearchDao`. FTS5 virtual tables (`verses_fts`, `footnotes_fts`) are **not** Room entities and are not added to the `entities` array. Room does not validate non-entity tables in a pre-built asset database (`exportSchema = false` is already set), so no schema conflict occurs. |
| `data/repository/BibleRepository.kt` | Add `suspend fun search(query: String, scope: SearchScope, includeFootnotes: Boolean, currentBookId: Int?): List<SearchResult>`. This method owns all query logic: token stripping, `SupportSQLiteQuery` construction, calling all three DAO methods, deduplicating by verse ID, merging in tier order, capping at 200 total results. |
| `ui/reading/ReadingScreen.kt` | Remove `pointerInput` swipe gesture block entirely; replace `SettingsPanel(...)` call with `SettingsDialog(...)`; add ⚙ `IconButton` overlaid at bottom-right of the screen inside the outer `Box` (not inside `Scaffold`, not a FAB — just a `Box` content alignment at `Alignment.BottomEnd`) |
| `MainActivity.kt` | Now instantiates both `ReadingViewModel` and `SearchViewModel` (in addition to the existing `SettingsViewModel`) and passes them down to their respective composables so all three screens share the same ViewModel instances. Wraps reading and search screens in a two-pane `Box` with horizontal `offset` animation driven by `searchVisible: Boolean` state. Swipe gesture detected here via `detectHorizontalDragGestures`. Collects `readingViewModel.uiState` via `collectAsStateWithLifecycle` and calls `searchViewModel.setCurrentBookId(uiState.currentBook?.id)` on each emission. |
| `buildscripts/import_bible_data.py` | Add FTS5 table creation and `rebuild` after all verse/footnote inserts |

## What Is Out of Scope

- Search history / saved searches
- Highlighting search terms in the reading screen after navigation
- Search within footnote keywords (footnote search covers footnote body text only)
- Fuzzy/phonetic matching
