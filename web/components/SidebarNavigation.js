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
