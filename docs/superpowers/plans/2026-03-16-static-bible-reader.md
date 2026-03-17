# Static Bible Reader Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a static GitHub Pages Bible reader (Preact + HTM + sql.js) that mirrors the Android RCV Bible Reader app.

**Architecture:** Preact SPA with no build step. sql.js loads the existing `bible.db` client-side. Service worker enables full offline use. Responsive layout: sidebar on desktop, bottom-sheet modal on mobile.

**Tech Stack:** Preact 10.x + HTM 3.x (CDN, ES modules), sql.js 1.12.x (CDN), CSS custom properties, Service Worker API.

**Spec:** `docs/superpowers/specs/2026-03-16-static-bible-reader-design.md`

**Testing note:** This project has no npm, no bundler, no test runner. Each task includes browser-based verification steps. A lightweight `test.html` file provides automated smoke tests for the database layer.

---

## File Map

| File | Responsibility | Created in Task |
|------|---------------|-----------------|
| `web/index.html` | Entry point, inline loading screen, script imports | 1 |
| `web/style.css` | All styles: theme variables, responsive layout, animations | 2 |
| `web/db.js` | sql.js init, progress tracking, 3 query functions, error handling | 3 |
| `web/test.html` | Browser-based smoke tests for db.js | 3 |
| `web/hooks/useReading.js` | State management hook (mirrors ReadingViewModel) | 4 |
| `web/components/FootnoteSection.js` | Footnote list display | 5 |
| `web/components/VerseItem.js` | Verse row with expansion + footnotes | 6 |
| `web/components/NavigationModal.js` | Book/chapter picker (modal on mobile) | 7 |
| `web/components/SidebarNavigation.js` | Desktop sidebar: both grids visible simultaneously | 7 |
| `web/components/ReadingScreen.js` | Main layout, wires all components | 8 |
| `web/app.js` | App root, loading→app transition, update banner | 9 |
| `web/sw.js` | Service worker: cache-first + stale-while-revalidate | 10 |
| `.github/workflows/deploy.yml` | GitHub Actions: copy bible.db + deploy to Pages | 11 |
| `web/.gitignore` | Excludes `bible.db` (build artifact) | 1 |

---

### Task 1: Project Scaffold + Loading Screen

**Files:**
- Create: `web/index.html`
- Create: `web/.gitignore`

This task creates the entry point with an inline loading screen that renders immediately (no external CSS dependency for the loading state). The loading screen matches the spec: "RCV Bible" title, subtitle, progress bar.

- [ ] **Step 1: Create `web/.gitignore`**

```
bible.db
```

This excludes the database file since it's copied at deploy time from `app/src/main/assets/bible.db`.

- [ ] **Step 2: Create `web/index.html`**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#FAF8F4" media="(prefers-color-scheme: light)">
  <meta name="theme-color" content="#1A1715" media="(prefers-color-scheme: dark)">
  <title>RCV Bible</title>
  <!-- Import map: ensures ALL modules share a single Preact instance.
       Without this, htm/preact bundles its own copy of Preact, causing
       "Hook can only be invoked from render methods" errors. -->
  <script type="importmap">
  {
    "imports": {
      "preact": "https://esm.sh/preact@10.25.4",
      "preact/hooks": "https://esm.sh/preact@10.25.4/hooks",
      "htm/preact": "https://esm.sh/htm@3.1.1/preact?external=preact",
      "sql.js": "https://esm.sh/sql.js@1.12.0"
    }
  }
  </script>
  <link rel="stylesheet" href="style.css">
  <style>
    /* Inline loading screen — renders before style.css loads */
    #loading {
      position: fixed; inset: 0;
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      background: #FAF8F4; color: #2C2C2C;
      font-family: Georgia, 'Times New Roman', serif;
      z-index: 9999;
    }
    @media (prefers-color-scheme: dark) {
      #loading { background: #1A1715; color: #D4C5A9; }
    }
    #loading h1 { font-size: 24px; font-weight: 600; color: #8B6914; margin: 0 0 4px; }
    @media (prefers-color-scheme: dark) {
      #loading h1 { color: #C49B5E; }
    }
    #loading .subtitle { font-size: 12px; color: #998866; font-family: system-ui, sans-serif; margin: 0 0 32px; }
    #loading .progress-track { width: 200px; height: 3px; background: #EEE8DC; border-radius: 2px; overflow: hidden; }
    @media (prefers-color-scheme: dark) {
      #loading .progress-track { background: #2C2520; }
    }
    #loading .progress-bar { width: 0%; height: 100%; background: #C49B5E; border-radius: 2px; transition: width 0.3s; }
    #loading .status { font-size: 11px; color: #998866; font-family: system-ui, sans-serif; margin-top: 8px; }
    #loading .error { color: #c44; margin-top: 16px; font-size: 13px; font-family: system-ui, sans-serif; display: none; }
    #loading .retry-btn {
      margin-top: 12px; padding: 8px 20px; background: #C49B5E; color: #1A1715;
      border: none; border-radius: 6px; font-size: 13px; cursor: pointer; display: none;
      font-family: system-ui, sans-serif;
    }
  </style>
</head>
<body>
  <div id="loading">
    <h1>RCV Bible</h1>
    <p class="subtitle">Recovery Version</p>
    <div class="progress-track">
      <div class="progress-bar" id="progress-bar"></div>
    </div>
    <p class="status" id="loading-status">Loading Bible data...</p>
    <p class="error" id="loading-error"></p>
    <button class="retry-btn" id="retry-btn">Retry</button>
  </div>

  <div id="app"></div>

  <script type="module" src="app.js"></script>
</body>
</html>
```

- [ ] **Step 3: Verify in browser**

Copy `bible.db` locally for dev:
```bash
cp app/src/main/assets/bible.db web/bible.db
```

Open `web/index.html` via a local server (required for ES modules):
```bash
cd web && python3 -m http.server 8000
```

Open `http://localhost:8000`. Expected: see the loading screen (title, subtitle, empty progress bar). The page will show the loading screen indefinitely since `app.js` doesn't exist yet — that's correct.

- [ ] **Step 4: Commit**

```bash
git add web/index.html web/.gitignore
git commit -m "feat(web): add project scaffold with loading screen"
```

---

### Task 2: CSS Theme + Responsive Layout

**Files:**
- Create: `web/style.css`

All styles in one file: CSS custom properties for light/dark themes, typography, verse styling, footnote expansion animations, responsive layout (sidebar vs. modal), navigation modal styles.

Reference: `app/src/main/java/com/rcvreader/ui/theme/Color.kt` for exact hex values, `Type.kt` for font sizes/weights.

- [ ] **Step 1: Create `web/style.css`**

```css
/* === Theme Variables === */
:root {
  --bg: #FAF8F4;
  --surface: #EEE8DC;
  --primary: #8B6914;
  --gold: #C49B5E;
  --secondary: #998866;
  --text: #2C2C2C;
  --on-surface: #2C2C2C;
  --footnote-highlight: rgba(196, 155, 94, 0.12);
}

@media (prefers-color-scheme: dark) {
  :root {
    --bg: #1A1715;
    --surface: #2C2520;
    --primary: #C49B5E;
    --gold: #C49B5E;
    --secondary: #8B7355;
    --text: #D4C5A9;
    --on-surface: #D4C5A9;
    --footnote-highlight: rgba(196, 155, 94, 0.12);
  }
}

/* === Reset & Base === */
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

html, body {
  height: 100%;
  background: var(--bg);
  color: var(--text);
  font-family: Georgia, 'Times New Roman', serif;
  -webkit-font-smoothing: antialiased;
}

/* === Layout === */
.app-layout {
  display: flex;
  height: 100vh;
}

/* Sidebar (desktop only) */
.sidebar {
  display: none;
  width: 200px;
  flex-shrink: 0;
  background: var(--surface);
  border-right: 1px solid rgba(128, 128, 128, 0.1);
  overflow-y: auto;
  padding: 16px;
}

@media (min-width: 769px) {
  .sidebar { display: block; }
}

/* Main content area */
.main-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.content-inner {
  max-width: 680px;
  width: 100%;
  margin: 0 auto;
  padding: 0 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

@media (min-width: 769px) {
  .content-inner { padding: 0 32px; }
}

/* === Typography === */
.ui-text {
  font-family: system-ui, -apple-system, sans-serif;
}

/* === Chapter Nav Links (top) === */
.chapter-nav {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.chapter-nav a {
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 12px;
  line-height: 20px;
  color: var(--secondary);
  text-decoration: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  background: none;
  border: none;
}

.chapter-nav a:hover { opacity: 0.7; }

/* === Trigger Bar (book name + chapter pill) === */
.trigger-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 0 4px;
}

.book-name {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 22px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
}

.book-name:hover { opacity: 0.8; }

.chapter-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  background: var(--surface);
  border-radius: 8px;
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--on-surface);
  opacity: 0.55;
  cursor: pointer;
  border: none;
}

.chapter-pill:hover { opacity: 0.7; }

.dropdown-caret {
  font-size: 11px;
  color: var(--secondary);
  opacity: 0.6;
  margin-left: 4px;
}

/* Hide trigger bar on desktop (sidebar replaces it) */
@media (min-width: 769px) {
  .trigger-bar { display: none; }
}

/* Desktop: show centered book title */
.desktop-title {
  display: none;
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: var(--primary);
  margin: 0 0 20px;
  font-family: Georgia, 'Times New Roman', serif;
}

@media (min-width: 769px) {
  .desktop-title { display: block; }
}

/* === Verse List === */
.verse-list {
  flex: 1;
  padding-top: 8px;
}

/* === Verse Item === */
.verse-item {
  padding: 14px 20px;
  transition: background-color 0.2s, border-color 0.2s;
  border-left: 3px solid transparent;
}

.verse-item.has-footnotes {
  cursor: pointer;
}

.verse-item.has-footnotes:hover {
  background: rgba(128, 128, 128, 0.04);
}

.verse-item.expanded {
  background: var(--footnote-highlight);
  border-left-color: var(--gold);
}

.verse-number {
  font-size: 11px;
  color: var(--secondary);
  vertical-align: super;
  line-height: 0;
  margin-right: 3px;
  font-weight: normal;
}

.verse-text {
  font-size: 17px;
  line-height: 1.875; /* 30px at 16px base ≈ 31.875px at 17px */
}

.verse-dot {
  color: var(--gold);
  font-size: 12px;
  margin-left: 3px;
}

/* === Footnote Section === */
.footnote-section {
  overflow: hidden;
  max-height: 0;
  opacity: 0;
  transition: max-height 0.3s ease, opacity 0.25s ease;
}

.footnote-section.visible {
  max-height: 2000px;  /* large enough for any footnote set */
  opacity: 1;
}

.footnote-divider {
  height: 1px;
  background: var(--secondary);
  opacity: 0.3;
  margin: 8px 0;
}

.footnote-item {
  font-size: 12px;
  line-height: 20px;
  color: var(--text);
  opacity: 0.7;
  margin-bottom: 8px;
}

.footnote-keyword {
  color: var(--gold);
  font-weight: bold;
  opacity: 1;
}

/* === Next Chapter Button (bottom of verse list) === */
.next-chapter-btn {
  display: block;
  width: 100%;
  padding: 16px;
  text-align: center;
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 14px;
  font-weight: bold;
  color: var(--primary);
  background: none;
  border: none;
  cursor: pointer;
}

.next-chapter-btn:hover { opacity: 0.7; }

/* === Sidebar Internals === */
.section-label {
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  font-weight: 600;
  color: var(--gold);
  margin-bottom: 8px;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 20px;
}

.book-btn {
  padding: 10px 4px;
  text-align: center;
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface);
  background: var(--bg);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.book-btn:hover { opacity: 0.8; }

.book-btn.active {
  background: rgba(196, 155, 94, 0.2);
  border: 1.5px solid var(--gold);
  font-weight: bold;
  color: var(--primary);
}

.chapter-grid {
  display: grid;
  gap: 6px;
}

/* Sidebar: 5 columns */
.sidebar .chapter-grid {
  grid-template-columns: repeat(5, 1fr);
}

.chapter-btn {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--on-surface);
  background: var(--bg);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.chapter-btn:hover { opacity: 0.8; }

.chapter-btn.active {
  background: var(--gold);
  color: var(--surface);
  font-weight: bold;
}

/* === Navigation Modal (mobile only) === */
.modal-overlay {
  display: none;
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
  align-items: flex-end;
  justify-content: center;
}

.modal-overlay.open {
  display: flex;
}

.modal-sheet {
  width: 100%;
  max-height: 70vh;
  background: var(--surface);
  border-radius: 24px 24px 0 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.modal-overlay.open .modal-sheet {
  transform: translateY(0);
}

.modal-handle {
  width: 10%;
  height: 4px;
  background: var(--on-surface);
  opacity: 0.2;
  border-radius: 2px;
  margin: 12px auto 4px;
}

/* Tabs */
.modal-tabs {
  display: flex;
  border-bottom: 0.5px solid rgba(128, 128, 128, 0.1);
}

.modal-tab {
  flex: 1;
  padding: 12px;
  text-align: center;
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 14px;
  font-weight: normal;
  color: var(--on-surface);
  opacity: 0.5;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: opacity 0.15s, border-color 0.15s;
}

.modal-tab.active {
  opacity: 1;
  font-weight: 600;
  color: var(--primary);
  border-bottom-color: var(--gold);
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

/* Modal chapters: 6 columns */
.modal-content .chapter-grid {
  grid-template-columns: repeat(6, 1fr);
}

/* Hide modal on desktop */
@media (min-width: 769px) {
  .modal-overlay { display: none !important; }
}

/* === Update Banner === */
.update-banner {
  position: fixed;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 20px;
  background: var(--primary);
  color: var(--bg);
  font-family: system-ui, -apple-system, sans-serif;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  z-index: 2000;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  display: none;
}

.update-banner.visible { display: block; }
```

- [ ] **Step 2: Verify in browser**

Reload `http://localhost:8000`. The loading screen should now have the warm cream background from `style.css` (if it loaded before `app.js` errors). Open DevTools → toggle device toolbar to check mobile vs desktop breakpoints.

- [ ] **Step 3: Commit**

```bash
git add web/style.css
git commit -m "feat(web): add complete CSS theme with responsive layout"
```

---

### Task 3: Database Layer (db.js)

**Files:**
- Create: `web/db.js`
- Create: `web/test.html`

The database layer initializes sql.js, fetches `bible.db` with streaming progress, and exposes the 3 query functions matching the Android DAOs. Error handling includes retry support.

Reference: `app/src/main/java/com/rcvreader/data/db/BookDao.kt`, `VerseDao.kt`, `FootnoteDao.kt` for query signatures.

- [ ] **Step 1: Create `web/db.js`**

```js
import initSqlJs from 'sql.js';

let db = null;

/**
 * Initialize the database. Fetches bible.db with progress tracking.
 * @param {function} onProgress - Called with (loaded, total) bytes
 * @returns {Promise<void>}
 */
export async function initDb(onProgress) {
  const SQL = await initSqlJs({
    locateFile: () => 'https://cdn.jsdelivr.net/npm/sql.js@1.12.0/dist/sql-wasm.wasm'
  });

  const response = await fetch('bible.db');
  if (!response.ok) {
    throw new Error(`Failed to fetch bible.db: ${response.status} ${response.statusText}`);
  }

  const contentLength = response.headers.get('Content-Length');
  const total = contentLength ? parseInt(contentLength, 10) : 0;

  if (!response.body) {
    // Fallback for browsers without ReadableStream
    const buffer = await response.arrayBuffer();
    if (buffer.byteLength < 1_000_000) {
      throw new Error(`bible.db too small (${buffer.byteLength} bytes) — likely corrupted or incomplete`);
    }
    db = new SQL.Database(new Uint8Array(buffer));
    return;
  }

  const reader = response.body.getReader();
  const chunks = [];
  let loaded = 0;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    chunks.push(value);
    loaded += value.length;
    if (onProgress) onProgress(loaded, total); // total may be 0 if Content-Length absent
  }

  const buffer = new Uint8Array(loaded);
  let offset = 0;
  for (const chunk of chunks) {
    buffer.set(chunk, offset);
    offset += chunk.length;
  }

  if (buffer.byteLength < 1_000_000) {
    throw new Error(`bible.db too small (${buffer.byteLength} bytes) — likely corrupted or incomplete`);
  }

  db = new SQL.Database(buffer);
}

/**
 * @returns {Array<{id: number, abbreviation: string, name: string, testament: string, chapter_count: number}>}
 */
export function getBooks() {
  const results = db.exec('SELECT id, abbreviation, name, testament, chapter_count FROM books ORDER BY id');
  if (!results.length) return [];
  return results[0].values.map(([id, abbreviation, name, testament, chapter_count]) => ({
    id, abbreviation, name, testament, chapter_count
  }));
}

/**
 * @returns {Array<{id: number, book_id: number, chapter: number, verse_number: number, text: string, has_footnotes: number}>}
 */
export function getVersesForChapter(bookId, chapter) {
  const stmt = db.prepare(
    'SELECT id, book_id, chapter, verse_number, text, has_footnotes FROM verses WHERE book_id = ? AND chapter = ? ORDER BY verse_number'
  );
  stmt.bind([bookId, chapter]);
  const rows = [];
  while (stmt.step()) {
    rows.push(stmt.getAsObject());
  }
  stmt.free();
  return rows;
}

/**
 * @returns {Array<{id: number, book_id: number, chapter: number, verse_number: number, footnote_number: number, keyword: string|null, content: string}>}
 */
export function getFootnotesForVerse(bookId, chapter, verseNumber) {
  const stmt = db.prepare(
    'SELECT id, book_id, chapter, verse_number, footnote_number, keyword, content FROM footnotes WHERE book_id = ? AND chapter = ? AND verse_number = ? ORDER BY footnote_number'
  );
  stmt.bind([bookId, chapter, verseNumber]);
  const rows = [];
  while (stmt.step()) {
    rows.push(stmt.getAsObject());
  }
  stmt.free();
  return rows;
}
```

- [ ] **Step 2: Create `web/test.html` — browser smoke tests**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>db.js smoke tests</title>
  <style>
    body { font-family: monospace; padding: 20px; background: #1a1a1a; color: #ccc; }
    .pass { color: #4c4; }
    .fail { color: #c44; }
    pre { margin: 4px 0; }
  </style>
</head>
<body>
<h2>db.js smoke tests</h2>
<div id="results"></div>
<script type="module">
import { initDb, getBooks, getVersesForChapter, getFootnotesForVerse } from './db.js';

const el = document.getElementById('results');
let passed = 0, failed = 0;

function assert(name, condition, detail) {
  if (condition) {
    el.innerHTML += `<pre class="pass">✓ ${name}</pre>`;
    passed++;
  } else {
    el.innerHTML += `<pre class="fail">✗ ${name} — ${detail || 'assertion failed'}</pre>`;
    failed++;
  }
}

try {
  await initDb((loaded, total) => {});

  // Books
  const books = getBooks();
  assert('getBooks returns 66 books', books.length === 66, `got ${books.length}`);
  assert('First book is Genesis', books[0]?.name === 'Genesis', `got ${books[0]?.name}`);
  assert('Last book is Revelation', books[65]?.name === 'Revelation', `got ${books[65]?.name}`);
  assert('Genesis has 50 chapters', books[0]?.chapter_count === 50, `got ${books[0]?.chapter_count}`);
  assert('Book 40 is Matthew', books[39]?.name === 'Matthew', `got ${books[39]?.name}`);

  // Verses
  const gen1 = getVersesForChapter(1, 1);
  assert('Genesis 1 has 31 verses', gen1.length === 31, `got ${gen1.length}`);
  assert('Gen 1:1 text starts with "In the beginning"', gen1[0]?.text.startsWith('In the beginning'), `got "${gen1[0]?.text.slice(0, 30)}"`);
  assert('Gen 1:1 has footnotes', gen1[0]?.has_footnotes === 1, `got ${gen1[0]?.has_footnotes}`);

  // Footnotes — use verse_number, NOT verse id
  const fn = getFootnotesForVerse(1, 1, 1);
  assert('Gen 1:1 has footnotes', fn.length > 0, `got ${fn.length}`);
  assert('First footnote has a keyword', fn[0]?.keyword != null, `got ${fn[0]?.keyword}`);
  assert('First footnote has content', fn[0]?.content?.length > 10, `content length: ${fn[0]?.content?.length}`);

  // Edge case: verse_number vs id divergence
  const matt1 = getVersesForChapter(40, 1);
  assert('Matthew 1 has verses', matt1.length > 0, `got ${matt1.length}`);
  const matt1v1 = matt1[0];
  assert('Matt 1:1 verse_number is 1', matt1v1?.verse_number === 1, `got ${matt1v1?.verse_number}`);
  assert('Matt 1:1 id is NOT 1 (diverges from verse_number)', matt1v1?.id !== 1, `id=${matt1v1?.id}, should not be 1`);
  const mattFn = getFootnotesForVerse(40, 1, 1);
  assert('Matt 1:1 footnotes fetched by verse_number work', mattFn.length > 0, `got ${mattFn.length}`);

  el.innerHTML += `<pre>\n${passed} passed, ${failed} failed</pre>`;
} catch (e) {
  el.innerHTML += `<pre class="fail">FATAL: ${e.message}\n${e.stack}</pre>`;
}
</script>
</body>
</html>
```

- [ ] **Step 3: Copy bible.db and verify**

```bash
cp app/src/main/assets/bible.db web/bible.db
cd web && python3 -m http.server 8000
```

Open `http://localhost:8000/test.html`. Expected: all assertions pass (green checkmarks). Key validation: the Matt 1:1 test confirms `verse_number` vs `id` divergence is handled correctly.

- [ ] **Step 4: Commit**

```bash
git add web/db.js web/test.html
git commit -m "feat(web): add database layer with sql.js queries and smoke tests"
```

---

### Task 4: State Management Hook (useReading.js)

**Files:**
- Create: `web/hooks/useReading.js`

This is the web equivalent of `ReadingViewModel.kt`. It manages all app state: current book/chapter, verses, expanded verse, footnotes, pending book selection, and adjacent chapter computation. Persists last position to localStorage with Safari try/catch.

Reference: `app/src/main/java/com/rcvreader/ui/reading/ReadingViewModel.kt` lines 20-131 for exact logic.

- [ ] **Step 1: Create `web/hooks/useReading.js`**

```js
import { useState, useEffect, useCallback, useRef } from 'preact/hooks';
import { getBooks, getVersesForChapter, getFootnotesForVerse } from '../db.js';

const STORAGE_KEY = 'rcv_reader_last_position';

function loadPosition() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (_) { /* Safari private browsing or disabled */ }
  return null;
}

function savePosition(bookId, chapter) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ bookId, chapter }));
  } catch (_) { /* ignore */ }
}

function computeAdjacentChapter(allBooks, currentBook, currentChapter, direction) {
  const targetChapter = currentChapter + direction;
  if (targetChapter >= 1 && targetChapter <= currentBook.chapter_count) {
    return { book: currentBook, chapter: targetChapter };
  }
  const currentIndex = allBooks.findIndex(b => b.id === currentBook.id);
  const targetIndex = currentIndex + direction;
  if (targetIndex < 0 || targetIndex >= allBooks.length) return null;
  const targetBook = allBooks[targetIndex];
  const chapter = direction > 0 ? 1 : targetBook.chapter_count;
  return { book: targetBook, chapter };
}

export function useReading() {
  const [state, setState] = useState({
    books: [],
    currentBook: null,
    currentChapter: 1,
    verses: [],
    expandedVerseId: null,
    expandedFootnotes: [],
    previousChapter: null,
    nextChapter: null,
    pendingBook: null,
  });

  const allBooksRef = useRef([]);
  const scrollRef = useRef(null); // will be set by ReadingScreen

  // Initialize: load books, restore position
  useEffect(() => {
    const books = getBooks();
    allBooksRef.current = books;

    const saved = loadPosition();
    const bookId = saved?.bookId ?? 1;
    const chapter = saved?.chapter ?? 1;
    const book = books.find(b => b.id === bookId) || books[0];

    if (book) {
      const verses = getVersesForChapter(book.id, chapter);
      const prev = computeAdjacentChapter(books, book, chapter, -1);
      const next = computeAdjacentChapter(books, book, chapter, 1);
      setState({
        books,
        currentBook: book,
        currentChapter: chapter,
        verses,
        expandedVerseId: null,
        expandedFootnotes: [],
        previousChapter: prev,
        nextChapter: next,
        pendingBook: null,
      });
    } else {
      setState(s => ({ ...s, books }));
    }
  }, []);

  const navigateTo = useCallback((bookId, chapter) => {
    const books = allBooksRef.current;
    const book = books.find(b => b.id === bookId);
    if (!book) return;

    savePosition(bookId, chapter);

    const verses = getVersesForChapter(bookId, chapter);
    const prev = computeAdjacentChapter(books, book, chapter, -1);
    const next = computeAdjacentChapter(books, book, chapter, 1);

    setState(s => ({
      ...s,
      currentBook: book,
      currentChapter: chapter,
      verses,
      expandedVerseId: null,
      expandedFootnotes: [],
      previousChapter: prev,
      nextChapter: next,
      pendingBook: null,
    }));

    // Scroll to top
    if (scrollRef.current) {
      scrollRef.current.scrollTop = 0;
    }
  }, []);

  const selectBook = useCallback((book) => {
    setState(s => ({ ...s, pendingBook: book }));
  }, []);

  const toggleVerse = useCallback((verse) => {
    if (!verse.has_footnotes) return;

    // Read current state to decide action
    const currentExpanded = state.expandedVerseId;
    if (currentExpanded === verse.id) {
      setState(s => ({ ...s, expandedVerseId: null, expandedFootnotes: [] }));
    } else {
      // Compute footnotes OUTSIDE setState (no side effects in updater).
      // Uses verse_number, NOT verse.id — IDs diverge from verse_numbers after Gen 1.
      const footnotes = getFootnotesForVerse(verse.book_id, verse.chapter, verse.verse_number);
      setState(s => ({ ...s, expandedVerseId: verse.id, expandedFootnotes: footnotes }));
    }
  }, [state.expandedVerseId]);

  return { state, navigateTo, selectBook, toggleVerse, scrollRef };
}
```

- [ ] **Step 2: Verify module loads**

Temporarily add to the bottom of `test.html`'s script (or test in DevTools console):
```js
// Quick check: import the hook module to ensure no syntax errors
const mod = await import('./hooks/useReading.js');
console.log('useReading module loaded:', typeof mod.useReading === 'function');
```

Open browser DevTools console at `http://localhost:8000/test.html`. Expected: `useReading module loaded: true` (the Preact import will also load from CDN).

- [ ] **Step 3: Commit**

```bash
git add web/hooks/useReading.js
git commit -m "feat(web): add useReading state management hook"
```

---

### Task 5: FootnoteSection Component

**Files:**
- Create: `web/components/FootnoteSection.js`

Simplest component — renders a list of footnotes with gold keywords and muted content. No state, purely presentational.

Reference: `app/src/main/java/com/rcvreader/ui/reading/FootnoteSection.kt` lines 18-48.

- [ ] **Step 1: Create `web/components/FootnoteSection.js`**

```js
import { html } from 'htm/preact';

export function FootnoteSection({ footnotes }) {
  if (!footnotes || footnotes.length === 0) return null;

  return html`
    <div class="footnote-divider"></div>
    ${footnotes.map(fn => html`
      <div class="footnote-item" key=${fn.id}>
        ${fn.keyword != null && html`
          <span class="footnote-keyword">${fn.keyword}: </span>
        `}
        ${fn.content}
      </div>
    `)}
  `;
}
```

- [ ] **Step 2: Commit**

```bash
git add web/components/FootnoteSection.js
git commit -m "feat(web): add FootnoteSection component"
```

---

### Task 6: VerseItem Component

**Files:**
- Create: `web/components/VerseItem.js`

Renders a single verse row: superscript verse number, text, gold dot if footnotes exist. Clickable only if `has_footnotes`. When expanded, shows gold left border + tinted background + FootnoteSection.

Reference: `app/src/main/java/com/rcvreader/ui/reading/VerseItem.kt` lines 31-102.

- [ ] **Step 1: Create `web/components/VerseItem.js`**

```js
import { html } from 'htm/preact';
import { FootnoteSection } from './FootnoteSection.js';

export function VerseItem({ verse, isExpanded, footnotes, onClick }) {
  const hasFootnotes = verse.has_footnotes === 1;
  const classes = [
    'verse-item',
    hasFootnotes ? 'has-footnotes' : '',
    isExpanded ? 'expanded' : '',
  ].filter(Boolean).join(' ');

  return html`
    <div
      class=${classes}
      onClick=${hasFootnotes ? onClick : undefined}
    >
      <span class="verse-text">
        <sup class="verse-number">${verse.verse_number}</sup>
        ${' '}${verse.text}
        ${hasFootnotes && html`<span class="verse-dot">${' \u2022'}</span>`}
      </span>
      <div class=${'footnote-section' + (isExpanded ? ' visible' : '')}>
        <${FootnoteSection} footnotes=${footnotes} />
      </div>
    </div>
  `;
}
```

- [ ] **Step 2: Commit**

```bash
git add web/components/VerseItem.js
git commit -m "feat(web): add VerseItem component with expansion"
```

---

### Task 7: NavigationModal Component

**Files:**
- Create: `web/components/NavigationModal.js`

Two components: (1) `NavigationModal` — slide-up tabbed modal for mobile, and (2) `SidebarNavigation` — desktop sidebar showing both book and chapter grids simultaneously (no tabs). Shared sub-components: `SectionLabel`, `BookGrid`, `ChaptersGrid`.

Reference: `app/src/main/java/com/rcvreader/ui/navigation/NavigationBottomSheet.kt` lines 39-321.

- [ ] **Step 1: Create `web/components/NavigationModal.js`**

```js
import { html } from 'htm/preact';
import { useState, useEffect } from 'preact/hooks';

// --- Shared sub-components (used by both modal and sidebar) ---

export function SectionLabel({ text }) {
  return html`<div class="section-label">${text.toUpperCase()}</div>`;
}

export function BookGrid({ books, activeBook, onBookSelected }) {
  return html`
    <div class="book-grid">
      ${books.map(book => html`
        <button
          key=${book.id}
          class=${'book-btn' + (book.id === activeBook?.id ? ' active' : '')}
          onClick=${() => onBookSelected(book)}
        >${book.abbreviation}</button>
      `)}
    </div>
  `;
}

export function ChaptersGrid({ chapterCount, currentChapter, onChapterSelected }) {
  const chapters = Array.from({ length: chapterCount }, (_, i) => i + 1);
  return html`
    <div class="chapter-grid">
      ${chapters.map(ch => html`
        <button
          key=${ch}
          class=${'chapter-btn' + (ch === currentChapter ? ' active' : '')}
          onClick=${() => onChapterSelected(ch)}
        >${ch}</button>
      `)}
    </div>
  `;
}

// --- Mobile Modal (tabbed: Books OR Chapters) ---

/**
 * Modal wrapper — the slide-up sheet for mobile.
 */
export function NavigationModal({
  open, books, currentBook, pendingBook, currentChapter,
  initialTab, onBookSelected, onChapterSelected, onClose
}) {
  const [tab, setTab] = useState(initialTab || 0);

  // Sync initialTab when modal opens with a specific tab
  useEffect(() => {
    if (initialTab !== undefined) setTab(initialTab);
  }, [initialTab]);

  const activeBook = pendingBook || currentBook;
  const otBooks = books.filter(b => b.testament === 'OT');
  const ntBooks = books.filter(b => b.testament === 'NT');

  // When a pending book is set, don't highlight any chapter
  const displayChapter = pendingBook ? -1 : currentChapter;

  function handleBookSelect(book) {
    onBookSelected(book);
    setTab(1); // Switch to chapters tab
  }

  // Close on overlay click
  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) onClose();
  }

  // Close on Escape key
  useEffect(() => {
    if (!open) return;
    function handleKey(e) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [open, onClose]);

  return html`
    <div class=${'modal-overlay' + (open ? ' open' : '')} onClick=${handleOverlayClick}>
      <div class="modal-sheet">
        <div class="modal-handle"></div>
        <div class="modal-tabs">
          <button class=${'modal-tab' + (tab === 0 ? ' active' : '')} onClick=${() => setTab(0)}>Books</button>
          <button class=${'modal-tab' + (tab === 1 ? ' active' : '')} onClick=${() => setTab(1)}>Chapters</button>
        </div>
        <div class="modal-content">
          ${tab === 0 ? html`
            <${SectionLabel} text="Old Testament" />
            <${BookGrid} books=${otBooks} activeBook=${activeBook} onBookSelected=${handleBookSelect} />
            <${SectionLabel} text="New Testament" />
            <${BookGrid} books=${ntBooks} activeBook=${activeBook} onBookSelected=${handleBookSelect} />
          ` : html`
            <${SectionLabel} text="Select Chapter" />
            <${ChaptersGrid}
              chapterCount=${activeBook?.chapter_count || 1}
              currentChapter=${displayChapter}
              onChapterSelected=${onChapterSelected}
            />
          `}
        </div>
      </div>
    </div>
  `;
}
```

- [ ] **Step 2: Create `web/components/SidebarNavigation.js`**

Desktop sidebar shows BOTH book grid and chapter grid simultaneously (no tabs). Spec: "Persistent left sidebar (200px) with: Book grid, Chapter grid, Current book/chapter highlighted."

```js
import { html } from 'htm/preact';
import { SectionLabel, BookGrid, ChaptersGrid } from './NavigationModal.js';

/**
 * Desktop sidebar — shows both book and chapter grids simultaneously.
 * No tabs, no modal — always visible on screens >768px.
 */
export function SidebarNavigation({
  books, currentBook, pendingBook, currentChapter,
  onBookSelected, onChapterSelected
}) {
  const activeBook = pendingBook || currentBook;
  const otBooks = books.filter(b => b.testament === 'OT');
  const ntBooks = books.filter(b => b.testament === 'NT');

  // When a pending book is set, don't highlight any chapter
  const displayChapter = pendingBook ? -1 : currentChapter;

  return html`
    <${SectionLabel} text="Old Testament" />
    <${BookGrid} books=${otBooks} activeBook=${activeBook} onBookSelected=${onBookSelected} />
    <${SectionLabel} text="New Testament" />
    <${BookGrid} books=${ntBooks} activeBook=${activeBook} onBookSelected=${onBookSelected} />
    <${SectionLabel} text="Chapters" />
    <${ChaptersGrid}
      chapterCount=${activeBook?.chapter_count || 1}
      currentChapter=${displayChapter}
      onChapterSelected=${onChapterSelected}
    />
  `;
}
```

- [ ] **Step 3: Commit**

```bash
git add web/components/NavigationModal.js web/components/SidebarNavigation.js
git commit -m "feat(web): add NavigationModal and SidebarNavigation components"
```

---

### Task 8: ReadingScreen Component

**Files:**
- Create: `web/components/ReadingScreen.js`

Main layout composable. Wires together: sidebar (desktop), trigger bar (mobile), chapter nav links, verse list, next chapter button, navigation modal. This is the web equivalent of `ReadingScreen.kt`.

Reference: `app/src/main/java/com/rcvreader/ui/reading/ReadingScreen.kt` lines 39-223.

- [ ] **Step 1: Create `web/components/ReadingScreen.js`**

```js
import { html } from 'htm/preact';
import { useState, useCallback, useRef, useEffect } from 'preact/hooks';
import { VerseItem } from './VerseItem.js';
import { NavigationModal } from './NavigationModal.js';
import { SidebarNavigation } from './SidebarNavigation.js';

export function ReadingScreen({ state, navigateTo, selectBook, toggleVerse, scrollRef }) {
  const [modalOpen, setModalOpen] = useState(false);
  const [modalTab, setModalTab] = useState(0);
  const mainRef = useRef(null);

  // Wire scrollRef so useReading can scroll to top on navigation
  useEffect(() => {
    scrollRef.current = mainRef.current;
  }, [scrollRef]);

  const { books, currentBook, currentChapter, verses, expandedVerseId,
          expandedFootnotes, previousChapter, nextChapter, pendingBook } = state;

  function openModal(tab) {
    setModalTab(tab);
    setModalOpen(true);
  }

  const handleBookSelected = useCallback((book) => {
    selectBook(book);
  }, [selectBook]);

  const handleChapterSelected = useCallback((chapter) => {
    setModalOpen(false);
    const targetBook = state.pendingBook || state.currentBook;
    if (targetBook) navigateTo(targetBook.id, chapter);
  }, [state.pendingBook, state.currentBook, navigateTo]);

  const handleSidebarChapterSelected = useCallback((chapter) => {
    const targetBook = state.pendingBook || state.currentBook;
    if (targetBook) navigateTo(targetBook.id, chapter);
  }, [state.pendingBook, state.currentBook, navigateTo]);

  return html`
    <div class="app-layout">
      <!-- Desktop Sidebar (both grids visible simultaneously, no tabs) -->
      <div class="sidebar">
        <${SidebarNavigation}
          books=${books}
          currentBook=${currentBook}
          pendingBook=${pendingBook}
          currentChapter=${currentChapter}
          onBookSelected=${handleBookSelected}
          onChapterSelected=${handleSidebarChapterSelected}
        />
      </div>

      <!-- Main Content -->
      <div class="main-content" ref=${mainRef}>
        <div class="content-inner">
          <!-- Chapter nav links -->
          <div class="chapter-nav">
            ${previousChapter ? html`
              <a onClick=${() => navigateTo(previousChapter.book.id, previousChapter.chapter)}>
                ← ${previousChapter.book.name} ${previousChapter.chapter}
              </a>
            ` : html`<span></span>`}
            ${nextChapter ? html`
              <a onClick=${() => navigateTo(nextChapter.book.id, nextChapter.chapter)}>
                ${nextChapter.book.name} ${nextChapter.chapter} →
              </a>
            ` : html`<span></span>`}
          </div>

          <!-- Mobile trigger bar -->
          <div class="trigger-bar">
            <button class="book-name" onClick=${() => openModal(0)}>
              ${currentBook?.name || ''}
            </button>
            <button class="chapter-pill" onClick=${() => openModal(1)}>
              Ch. ${currentChapter}
            </button>
            <span class="dropdown-caret">▾</span>
          </div>

          <!-- Desktop title (book + chapter, since trigger bar is hidden) -->
          <h1 class="desktop-title">
            ${currentBook?.name || ''} ${currentChapter}
          </h1>

          <!-- Verse list -->
          <div class="verse-list">
            ${verses.map(verse => html`
              <${VerseItem}
                key=${verse.id}
                verse=${verse}
                isExpanded=${expandedVerseId === verse.id}
                footnotes=${expandedVerseId === verse.id ? expandedFootnotes : []}
                onClick=${() => toggleVerse(verse)}
              />
            `)}

            <!-- Next chapter button at bottom -->
            ${nextChapter && html`
              <button
                class="next-chapter-btn"
                onClick=${() => navigateTo(nextChapter.book.id, nextChapter.chapter)}
              >
                Next: ${nextChapter.book.name} ${nextChapter.chapter} →
              </button>
            `}
          </div>
        </div>
      </div>

      <!-- Mobile Navigation Modal -->
      <${NavigationModal}
        open=${modalOpen}
        books=${books}
        currentBook=${currentBook}
        pendingBook=${pendingBook}
        currentChapter=${currentChapter}
        initialTab=${modalTab}
        onBookSelected=${handleBookSelected}
        onChapterSelected=${handleChapterSelected}
        onClose=${() => setModalOpen(false)}
      />
    </div>
  `;
}
```

- [ ] **Step 2: Commit**

```bash
git add web/components/ReadingScreen.js
git commit -m "feat(web): add ReadingScreen main layout component"
```

---

### Task 9: App Root (app.js)

**Files:**
- Create: `web/app.js`

Bootstraps the entire application: initializes the database with progress tracking, transitions from loading screen to app, registers the service worker, and handles update notifications.

- [ ] **Step 1: Create `web/app.js`**

```js
import { html, render } from 'htm/preact';
import { useState, useEffect } from 'preact/hooks';
import { initDb } from './db.js';
import { useReading } from './hooks/useReading.js';
import { ReadingScreen } from './components/ReadingScreen.js';

function App() {
  const reading = useReading();

  return html`
    <${ReadingScreen}
      state=${reading.state}
      navigateTo=${reading.navigateTo}
      selectBook=${reading.selectBook}
      toggleVerse=${reading.toggleVerse}
      scrollRef=${reading.scrollRef}
    />
  `;
}

// --- Bootstrap ---
const progressBar = document.getElementById('progress-bar');
const loadingStatus = document.getElementById('loading-status');
const loadingError = document.getElementById('loading-error');
const retryBtn = document.getElementById('retry-btn');
const loadingEl = document.getElementById('loading');
const appEl = document.getElementById('app');

async function boot() {
  // Reset error state
  loadingError.style.display = 'none';
  retryBtn.style.display = 'none';
  loadingStatus.textContent = 'Loading Bible data...';
  progressBar.style.width = '0%';

  try {
    await initDb((loaded, total) => {
      if (total > 0) {
        const pct = Math.round((loaded / total) * 100);
        progressBar.style.width = pct + '%';
        if (pct < 100) {
          loadingStatus.textContent = `Loading Bible data... ${pct}%`;
        } else {
          loadingStatus.textContent = 'Preparing...';
        }
      } else {
        // No Content-Length header — show indeterminate progress
        const mb = (loaded / 1_000_000).toFixed(1);
        loadingStatus.textContent = `Loading Bible data... ${mb} MB`;
        progressBar.style.width = '100%';
        progressBar.style.opacity = '0.5';
      }
    });

    // Hide loading, show app
    loadingEl.style.display = 'none';
    render(html`<${App} />`, appEl);
  } catch (err) {
    loadingStatus.textContent = 'Failed to load';
    loadingError.textContent = err.message;
    loadingError.style.display = 'block';
    retryBtn.style.display = 'inline-block';
  }
}

retryBtn.addEventListener('click', boot);

// Register service worker + listen for update notifications
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {
    // Service worker not supported or failed — app works fine without it
  });

  // Listen for update messages from SW (stale-while-revalidate detected new content)
  navigator.serviceWorker.addEventListener('message', (e) => {
    if (e.data?.type === 'UPDATE_AVAILABLE') {
      const banner = document.createElement('div');
      banner.className = 'update-banner visible';
      banner.textContent = 'Update available \u2014 tap to reload';
      banner.addEventListener('click', () => location.reload());
      document.body.appendChild(banner);
    }
  });
}

boot();
```

- [ ] **Step 2: Verify full app in browser**

```bash
cd web && python3 -m http.server 8000
```

Open `http://localhost:8000`. Expected:
1. Loading screen appears with "RCV Bible" title
2. Progress bar fills as `bible.db` downloads
3. App renders showing Genesis 1 (or last-read position)
4. Tap a verse with a gold dot → footnotes expand with gold border
5. Tap book name (mobile) or sidebar books (desktop) → navigation works
6. Tap chapter pill → chapters grid appears
7. Select a book → switches to chapters tab (two-step selection)
8. Navigate to another chapter → previous verse list scrolls to top
9. Prev/next chapter links work, including cross-book transitions
10. Reload page → restores last-read position

- [ ] **Step 3: Test error handling**

Rename `bible.db` temporarily:
```bash
mv web/bible.db web/bible.db.bak
```

Reload page. Expected: loading screen shows error message + "Retry" button. Restore file:
```bash
mv web/bible.db.bak web/bible.db
```

Click "Retry". Expected: app loads normally.

- [ ] **Step 4: Test responsive layout**

In browser DevTools, toggle device toolbar:
- **Mobile (375px)**: trigger bar visible, no sidebar, tap book name to open modal
- **Desktop (1200px)**: sidebar visible, trigger bar hidden, desktop title shown

- [ ] **Step 5: Test dark mode**

In browser DevTools → Rendering → `prefers-color-scheme: dark`. Expected: dark brown background (#1A1715), light beige text, gold accents unchanged.

- [ ] **Step 6: Commit**

```bash
git add web/app.js
git commit -m "feat(web): add app root with loading, error handling, and SW registration"
```

---

### Task 10: Service Worker (sw.js)

**Files:**
- Create: `web/sw.js`

Implements the two-tier caching strategy from the spec:
- **Cache-first** for immutable assets (bible.db, CDN libs)
- **Stale-while-revalidate** for app code (html, css, js)

Validates responses before caching (no partial bible.db). Posts message to client on update.

- [ ] **Step 1: Create `web/sw.js`**

```js
const CACHE_NAME = 'rcv-bible-v1';

// Immutable assets — cache-first, never re-fetch
const IMMUTABLE_PATTERNS = [
  'bible.db',
  'esm.sh/',
  'cdn.jsdelivr.net/',
];

// App shell — stale-while-revalidate
const APP_SHELL = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './db.js',
  './hooks/useReading.js',
  './components/ReadingScreen.js',
  './components/VerseItem.js',
  './components/FootnoteSection.js',
  './components/NavigationModal.js',
  './components/SidebarNavigation.js',
];

self.addEventListener('install', (event) => {
  // Pre-cache app shell on install
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  // Clean old caches
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

function isImmutable(url) {
  return IMMUTABLE_PATTERNS.some(p => url.includes(p));
}

self.addEventListener('fetch', (event) => {
  const { request } = event;

  // Only handle GET requests
  if (request.method !== 'GET') return;

  if (isImmutable(request.url)) {
    // Cache-first for immutable assets
    event.respondWith(
      caches.match(request).then(cached => {
        if (cached) return cached;
        return fetch(request).then(response => {
          // Don't cache bad responses or partial bible.db
          if (!response.ok) return response;
          if (request.url.includes('bible.db')) {
            const contentLength = response.headers.get('Content-Length');
            if (contentLength && parseInt(contentLength, 10) < 1_000_000) {
              return response; // Don't cache tiny/corrupt bible.db
            }
          }
          const clone = response.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(request, clone));
          return response;
        });
      })
    );
  } else {
    // Stale-while-revalidate for app code
    event.respondWith(
      caches.match(request).then(cached => {
        const fetchPromise = fetch(request).then(response => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then(cache => cache.put(request, clone));

            // Notify clients if app code was updated
            if (cached) {
              const cachedEtag = cached.headers.get('ETag');
              const newEtag = response.headers.get('ETag');
              if (cachedEtag && newEtag && cachedEtag !== newEtag) {
                self.clients.matchAll().then(clients => {
                  clients.forEach(c => c.postMessage({ type: 'UPDATE_AVAILABLE' }));
                });
              }
            }
          }
          return response;
        }).catch(() => cached); // Offline fallback

        return cached || fetchPromise;
      })
    );
  }
});
```

- [ ] **Step 2: Verify offline capability**

1. Open `http://localhost:8000` — let app fully load
2. Open DevTools → Application → Service Workers → confirm `sw.js` is active
3. DevTools → Network → check "Offline"
4. Reload page → app should load from cache, fully functional
5. Navigate between chapters, expand footnotes — all should work offline
6. Uncheck "Offline" when done

- [ ] **Step 3: Commit**

```bash
git add web/sw.js
git commit -m "feat(web): add service worker with cache-first + stale-while-revalidate"
```

---

### Task 11: GitHub Actions Deploy Workflow

**Files:**
- Create: `.github/workflows/deploy.yml`

Copies `bible.db` from `app/src/main/assets/` into `web/` at deploy time, then deploys the `web/` directory to GitHub Pages. This avoids committing the database twice.

- [ ] **Step 1: Create `.github/workflows/deploy.yml`**

```yaml
name: Deploy to GitHub Pages

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - uses: actions/checkout@v4

      - name: Copy bible.db to web directory
        run: cp app/src/main/assets/bible.db web/bible.db

      - name: Setup Pages
        uses: actions/configure-pages@v4

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: web

      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

- [ ] **Step 2: Verify workflow syntax**

```bash
cat .github/workflows/deploy.yml | python3 -c "import yaml, sys; yaml.safe_load(sys.stdin); print('Valid YAML')"
```

Expected: `Valid YAML`

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/deploy.yml
git commit -m "ci: add GitHub Actions workflow for Pages deployment"
```

---

## Final Verification Checklist

After all tasks are complete, run through this end-to-end check:

- [ ] `cd web && python3 -m http.server 8000` — open `http://localhost:8000`
- [ ] Loading screen shows progress bar, transitions to app
- [ ] Genesis 1 loads by default on first visit
- [ ] Verse with gold dot expands on click — gold border + footnotes visible
- [ ] Verse without gold dot is not clickable
- [ ] Tapping expanded verse collapses it
- [ ] Tapping different verse closes previous and opens new
- [ ] Footnote keywords bold gold, content muted
- [ ] Previous/next chapter links at top work
- [ ] Next chapter button at bottom works
- [ ] Cross-book navigation works (e.g., Malachi → Matthew)
- [ ] Gen 1 has no previous link, Rev 22 has no next link
- [ ] Mobile: tap book name → modal opens on Books tab
- [ ] Mobile: tap chapter pill → modal opens on Chapters tab
- [ ] Mobile: select book → auto-switch to Chapters tab, no chapter highlighted
- [ ] Mobile: select chapter → modal closes, navigates
- [ ] Desktop: sidebar always visible with book/chapter grids
- [ ] Desktop: clicking sidebar book + chapter navigates correctly
- [ ] Dark mode matches spec colors (DevTools → Rendering → prefers-color-scheme: dark)
- [ ] Reload page → restores last-read position
- [ ] Offline mode works (DevTools → Network → Offline → reload)
- [ ] `http://localhost:8000/test.html` — all smoke tests pass
- [ ] Error handling: rename bible.db → error + retry button shown
