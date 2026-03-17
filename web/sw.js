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
