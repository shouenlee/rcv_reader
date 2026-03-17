import { html } from 'htm/preact';
import { useState } from 'preact/hooks';
import { SectionLabel, BookGrid, ChaptersGrid } from './NavigationModal.js';

/**
 * Desktop sidebar with Books/Chapters tabs — matches Android BottomSheet UX.
 * Selecting a book auto-switches to Chapters tab (two-step selection).
 */
export function SidebarNavigation({
  books, currentBook, pendingBook, currentChapter,
  onBookSelected, onChapterSelected
}) {
  const [tab, setTab] = useState(0);

  const activeBook = pendingBook || currentBook;
  const otBooks = books.filter(b => b.testament === 'OT');
  const ntBooks = books.filter(b => b.testament === 'NT');

  // When a pending book is set, don't highlight any chapter
  const displayChapter = pendingBook ? -1 : currentChapter;

  function handleBookSelect(book) {
    onBookSelected(book);
    setTab(1); // Auto-switch to Chapters tab
  }

  return html`
    <div class="sidebar-tabs">
      <button class=${'sidebar-tab' + (tab === 0 ? ' active' : '')} onClick=${() => setTab(0)}>Books</button>
      <button class=${'sidebar-tab' + (tab === 1 ? ' active' : '')} onClick=${() => setTab(1)}>Chapters</button>
    </div>
    <div class="sidebar-content">
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
  `;
}
