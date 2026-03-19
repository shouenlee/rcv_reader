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
