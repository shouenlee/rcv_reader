import { html } from 'htm/preact';
import { FootnoteSection } from './FootnoteSection.js';

const ENTITIES = { '&mdash;': '\u2014', '&ndash;': '\u2013', '&hellip;': '\u2026', '&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&apos;': "'" };
const decodeEntities = (text) => text.replace(/&[a-zA-Z]+;/g, (m) => ENTITIES[m] ?? m);

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
        ${' '}${decodeEntities(verse.text)}
        ${hasFootnotes && html`<span class="verse-dot">${' \u2022'}</span>`}
      </span>
      <div class=${'footnote-section' + (isExpanded ? ' visible' : '')}>
        <${FootnoteSection} footnotes=${footnotes} />
      </div>
    </div>
  `;
}
