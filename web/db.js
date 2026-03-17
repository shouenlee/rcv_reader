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
