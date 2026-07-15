/* フォトベンチ Service Worker
   キャッシュ更新時は CACHE_VERSION を上げること */
'use strict';
const CACHE_VERSION = 'photobench-v10';
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

/* 共有ターゲット:ギャラリー等から共有された画像を受け取り、
   一時キャッシュに置いてからアプリへリダイレクトする */
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);

  if (e.request.method === 'POST' && url.pathname.endsWith('/share-target')) {
    e.respondWith((async () => {
      try {
        const data = await e.request.formData();
        const files = data.getAll('images')
          .filter(f => f && f.type && f.type.startsWith('image/'));
        const cache = await caches.open('shared-images');
        await Promise.all(files.map((f, i) =>
          cache.put('./shared-' + i, new Response(f, {
            headers: {
              'Content-Type': f.type,
              'X-Name': encodeURIComponent(f.name || ('shared-' + i + '.jpg'))
            }
          }))
        ));
        return Response.redirect('./?shared=' + files.length, 303);
      } catch (_) {
        return Response.redirect('./', 303);
      }
    })());
    return;
  }

  /* stale-while-revalidate:
     キャッシュを即返しつつ、裏でネットワークから更新する */
  if (e.request.method !== 'GET') return;
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
