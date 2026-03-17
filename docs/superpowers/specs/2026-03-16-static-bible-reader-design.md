# Static Bible Reader — Design Spec

**Date:** 2026-03-16
**Status:** Approved
**Goal:** Build a static GitHub Pages version of the RCV Bible Reader Android app, providing wider browser-based access on any device.

## Overview

A single-page web application that replicates the Android Bible reader as a static site hostable on GitHub Pages. Uses Preact + HTM (no build step), sql.js for client-side SQLite queries against the existing `bible.db`, and a service worker for full offline capability.

## Architecture

```
bible.db (10MB, fetched once, cached by service worker)
  → sql.js (SQLite in WebAssembly, ~1MB)
    → db.js (query helpers: getBooks, getVersesForChapter, getFootnotesForVerse)
      → useReading() hook (state management, replaces ReadingViewModel)
        → Preact components (ReadingScreen, VerseItem, FootnoteSection, NavigationModal)
```

### Android → Web Mapping

| Android | Web |
|---------|-----|
| ReadingViewModel (StateFlow) | `useReading()` Preact hook (useState/useEffect) |
| Room DAOs (Flow/suspend) | `db.js` sync query functions via sql.js |
| SharedPreferences | `localStorage` |
| ModalBottomSheet | CSS modal (slide-up on mobile, sidebar on desktop) |
| AnimatedVisibility | CSS transitions (max-height / opacity) |
| LazyColumn | Native scrolling `<div>` |
| Compose theme | CSS custom properties with `prefers-color-scheme` |

## File Structure

```
web/
├── index.html              ← Single entry point (inline loading screen CSS)
├── style.css               ← All styles (light/dark theme via CSS custom properties)
├── app.js                  ← Preact app root, imports components
├── components/
│   ├── ReadingScreen.js    ← Main layout (header, verse list, nav triggers)
│   ├── VerseItem.js        ← Verse display with superscript number, gold dot, expansion
│   ├── FootnoteSection.js  ← Expandable footnote list (gold keyword + muted content)
│   └── NavigationModal.js  ← Book/chapter picker (modal on mobile, sidebar on desktop)
├── hooks/
│   └── useReading.js       ← State: currentBook, currentChapter, verses, expandedVerseId,
│                              expandedFootnotes, pendingBook, previousChapter, nextChapter.
│                              Persists last position to localStorage.
├── db.js                   ← sql.js initialization + query wrappers
├── theme.js                ← Color/typography constants (optional, CSS does most of the work)
├── sw.js                   ← Service worker for offline caching
└── bible.db                ← Copied from app/src/main/assets/
```

All JS files use ES modules with Preact + HTM imported from CDN (esm.sh or unpkg). No build step, no npm, no bundler.

## Responsive Layout

### Mobile (≤768px) — Matches Android Feel

- Full-width verse list
- Previous/next chapter links at top
- Book name + chapter pill trigger bar (centered)
- Tapping book name opens navigation modal (Books tab)
- Tapping chapter pill opens navigation modal (Chapters tab)
- Navigation modal slides up from bottom (CSS transform), mimicking Android BottomSheet
- Next chapter button at bottom of verse list

### Desktop (>768px) — Web-Adapted

- Persistent left sidebar (200px) with:
  - Book grid (3-column, OT/NT sections with gold labels)
  - Chapter grid (5-column, square buttons)
  - Current book/chapter highlighted in gold
- Main content area centered, max-width 680px for comfortable reading
- Previous/next chapter links at top of content area
- Book title centered above verses
- No modal needed — sidebar is always visible

### Navigation Modal (Mobile Only)

- Two tabs: Books | Chapters (gold tab indicator)
- Books panel: 3-column grid, OT/NT sections with uppercase gold labels
- Chapters panel: 6-column square grid
- Selecting a book auto-switches to Chapters tab (two-step selection, matching Android)
- Selecting a chapter closes modal and navigates
- Current book: gold tint background + gold border + bold text
- Current chapter: solid gold background + white text

## Theming

### Colors (CSS Custom Properties)

**Light mode** (default):
| Variable | Value | Usage |
|----------|-------|-------|
| `--bg` | `#FAF8F4` | Page background (warm cream) |
| `--surface` | `#EEE8DC` | Sidebar, pills, cards |
| `--primary` | `#8B6914` | Book title, nav links |
| `--gold` | `#C49B5E` | Accents: dots, keywords, borders, highlights |
| `--secondary` | `#998866` | Verse numbers, muted text |
| `--text` | `#2C2C2C` | Body text |

**Dark mode** (`@media (prefers-color-scheme: dark)`):
| Variable | Value | Usage |
|----------|-------|-------|
| `--bg` | `#1A1715` | Page background (dark brown) |
| `--surface` | `#2C2520` | Sidebar, pills, cards |
| `--primary` | `#C49B5E` | Book title, nav links |
| `--gold` | `#C49B5E` | Same gold accent |
| `--secondary` | `#8B7355` | Verse numbers, muted text |
| `--text` | `#D4C5A9` | Body text (light beige) |

Theme follows system preference automatically via `prefers-color-scheme`, matching Android's `isSystemInDarkTheme()`.

### Typography

- **Verse text**: `Georgia, 'Times New Roman', serif` — 17px, line-height 1.875 (30px)
- **Footnote keywords**: serif, bold, gold color
- **Footnote content**: serif, 12px, 70% opacity
- **Book title**: serif, 22px, semi-bold, primary color
- **UI elements** (nav links, pills, labels): `system-ui, -apple-system, sans-serif`
- **Chapter pill**: 14px, medium weight, 55% opacity

## Component Behavior

### VerseItem

- Superscript verse number (small, secondary color, baseline-shifted)
- Verse text in serif
- Gold dot indicator (`●`) if `has_footnotes === 1`, positioned after text
- Only clickable if verse has footnotes
- **Expanded state**: gold left border (3px), gold-tinted background (`--gold` at 12% opacity)
- **Collapsed → expanded**: CSS transition on max-height + opacity for footnote section
- Tapping an expanded verse collapses it; tapping a different verse collapses the previous and expands the new one (single expansion, matching Android)

### FootnoteSection

- Appears inside expanded VerseItem with a subtle top divider
- Each footnote: bold gold keyword (if present) followed by colon, then muted content text
- Keyword can be null (some footnotes have no keyword)

### useReading() Hook — State Management

State shape (mirrors Android's `ReadingUiState`):
```
{
  books: [],              // All 66 books, loaded once on init
  currentBook: null,      // Current Book object
  currentChapter: 1,      // Current chapter number
  verses: [],             // Verses for current chapter
  expandedVerseId: null,  // Which verse is expanded (by verse.id, not verse_number)
  expandedFootnotes: [],  // Footnotes for expanded verse
  previousChapter: null,  // { book, chapter } or null (Gen 1 → null)
  nextChapter: null,      // { book, chapter } or null (Rev 22 → null)
  pendingBook: null,      // Two-step selection: book chosen, waiting for chapter
}
```

Key methods:
- `navigateTo(bookId, chapter)` — Load verses, compute adjacent chapters, clear expansion, persist to localStorage, scroll to top
- `selectBook(book)` — Set pendingBook without loading verses (defers to chapter pick)
- `toggleVerse(verse)` — Expand/collapse. Only if `has_footnotes`. Fetches footnotes on expand.
- `computeAdjacentChapter(book, chapter, direction)` — Handles chapter wrapping and book transitions. Returns null at Bible boundaries (Gen 1 prev, Rev 22 next).

### Persistence

- `localStorage` key: `rcv_reader_last_position`
- Stores: `{ bookId: number, chapter: number }`
- Restored on page load; defaults to Genesis 1 if absent

## Database Layer (db.js)

Uses sql.js (SQLite compiled to WebAssembly) to query the existing `bible.db` file directly in the browser.

### Initialization

1. Load sql.js WASM from CDN
2. Fetch `bible.db` as ArrayBuffer
3. Open database with `new SQL.Database(new Uint8Array(buffer))`
4. Database instance stored in module scope, reused by all queries

### Queries

```js
getBooks()
// SELECT * FROM books ORDER BY id
// Returns: [{ id, abbreviation, name, testament, chapter_count }]

getVersesForChapter(bookId, chapter)
// SELECT * FROM verses WHERE book_id = ? AND chapter = ? ORDER BY verse_number
// Returns: [{ id, book_id, chapter, verse_number, text, has_footnotes }]

getFootnotesForVerse(bookId, chapter, verseNumber)
// SELECT * FROM footnotes WHERE book_id = ? AND chapter = ? AND verse_number = ? ORDER BY footnote_number
// Returns: [{ id, book_id, chapter, verse_number, footnote_number, keyword, content }]
```

All queries are synchronous (sql.js runs in-memory). No async needed after initial load.

## Loading Experience

### First Visit Sequence

1. **index.html** loads (~5KB) — inline CSS renders branded loading screen immediately
2. **Preact + HTM** fetched from CDN in parallel (~4KB gzipped)
3. **sql.js WASM** initialized (~1MB gzipped)
4. **bible.db** fetched (~10MB) — progress bar shown during download
5. **App renders** — last reading position restored from localStorage
6. **Service worker** installs in background, caches all assets

Total first-visit download: ~12MB. Time: ~3-6 seconds on broadband.

### Loading Screen

Inline in `index.html` (no external CSS dependency):
- "RCV Bible" title in serif, gold color
- "Recovery Version" subtitle, muted
- Thin gold progress bar (width animated via JS based on fetch progress)
- "Loading Bible data..." status text

## Offline Strategy (Service Worker)

### Cache-First Assets (immutable)
- `bible.db` — content never changes
- `sql-wasm.wasm` — versioned by CDN URL
- `preact`, `htm` — versioned by CDN URL

### Stale-While-Revalidate (app code)
- `index.html`, `style.css`, `*.js` files
- Serve cached version instantly, fetch update in background
- User gets updated version on next visit

### Return Visit Performance
- 0 network requests required
- App loads in <500ms from cache
- Fully functional offline (airplane mode, no wifi)

## GitHub Pages Deployment

The `web/` directory is self-contained and deployable as-is:
- Configure GitHub Pages to serve from `web/` directory (or copy to a `gh-pages` branch)
- No build step required — all files are static
- `bible.db` committed to the repo (10MB, within GitHub's 100MB file limit)

## Scope Exclusions

- No URL routing / deep linking (hash-based routing could be added later)
- No search functionality
- No text selection / copy features beyond browser defaults
- No bookmarking beyond last-read position
- No font size adjustment
- No manual light/dark toggle (follows system only)
