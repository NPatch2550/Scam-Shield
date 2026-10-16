const CACHE_NAME = 'scamshield-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Network first, fallback to cache for offline capabilities
  event.respondWith(
    fetch(event.request).catch(() => caches.match(event.request))
  );
});
