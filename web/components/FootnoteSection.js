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
