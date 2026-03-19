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

    setState(s => {
      if (s.expandedVerseId === verse.id) {
        return { ...s, expandedVerseId: null, expandedFootnotes: [] };
      }
      // getFootnotesForVerse is synchronous (sql.js in-memory), safe inside updater.
      // Uses verse_number, NOT verse.id — IDs diverge from verse_numbers after Gen 1.
      const footnotes = getFootnotesForVerse(verse.book_id, verse.chapter, verse.verse_number);
      return { ...s, expandedVerseId: verse.id, expandedFootnotes: footnotes };
    });
  }, []);

  return { state, navigateTo, selectBook, toggleVerse, scrollRef };
}
