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
