// ルートのWebアプリ一式を www/ にコピーする(Capacitorのビルド元)
import { mkdirSync, copyFileSync } from 'node:fs';

const FILES = [
  'index.html',
  'manifest.webmanifest',
  'sw.js',
  'icon-192.png',
  'icon-512.png',
  'icon-maskable-512.png',
  'apple-touch-icon.png'
];

mkdirSync('www', { recursive: true });
for (const f of FILES) copyFileSync(f, 'www/' + f);
console.log('www/ を更新しました (' + FILES.length + ' files)');
