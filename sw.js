/* フォトベンチ Service Worker
   キャッシュ更新時は CACHE_VERSION を上げること */
'use strict';
const CACHE_VERSION = 'photobench-v1';
const ASSETS = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon-192.png',
  './icon-512.png',
  './icon-maskable-512.png',
  './apple-touch-icon.png'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE_VERSION)
      .then(c => c.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(
        keys.filter(k => k !== CACHE_VERSION).map(k => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

/* stale-while-revalidate:
   キャッシュを即返しつつ、裏でネットワークから更新する */
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  const url = new URL(e.request.url);
  if (url.origin !== location.origin) return;

  e.respondWith(
    caches.open(CACHE_VERSION).then(async cache => {
      const cached = await cache.match(e.request, {ignoreSearch: true});
      const fetched = fetch(e.request)
        .then(res => {
          if (res && res.ok) cache.put(e.request, res.clone());
          return res;
        })
        .catch(() => cached);
      return cached || fetched;
    })
  );
});
