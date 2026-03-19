import { html, render } from 'htm/preact';
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
