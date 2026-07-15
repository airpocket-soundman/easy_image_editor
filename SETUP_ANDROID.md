# フォトベンチ Android ネイティブ版(APK)

ネイティブ版では以下が可能になります(PWA版との違い):

- 起動すると端末内の写真が自動でサムネイル一覧表示される(ファイル選択画面は不要)
- タップしてすぐ編集、保存は元の写真と同じフォルダへ(不可の場合は Pictures/PhotoBench へ)

対応OS: Android 10 以上

## 方法A: GitHub Actions で自動ビルド(推奨・PCへのインストール不要)

mainブランチにpushするたびに、GitHubが自動でAPKをビルドします。

1. pushする(初回はこのワークフロー追加のpush自体でビルドが走ります)
2. リポジトリの **Actions** タブでビルド完了(緑のチェック)を待つ(5〜10分)
3. スマホのブラウザで
   `https://github.com/airpocket-soundman/easy_image_editor/releases`
   を開き、**PhotoBench.apk** をダウンロード
4. ダウンロードしたAPKを開いてインストール(「提供元不明のアプリ」の警告は許可)
5. 初回起動時に「写真へのアクセス」を**許可**する

以後、コードを変更してpushするたびにReleasesの `latest` が自動更新されます。
同じ鍵(photobench.keystore)で署名されるため、上書きインストールで更新できます。

> 注: photobench.keystore は個人利用向けの簡易署名鍵です(パスワードもリポジトリに公開されています)。Playストアで配布する場合は非公開の鍵を別途作成してください。

## 方法B: PCのAndroid Studioでビルド

### 1. 必要なソフト(1回だけ)

1. **Node.js LTS** — https://nodejs.org/ja
2. **Android Studio** — https://developer.android.com/studio(セットアップは標準設定でOK)

### 2. プロジェクト準備(1回だけ)

```
cd C:\Users\yamashita_y00031\Documents\github\easy_image_editor
npm install
npm run sync-www
npx cap add android
npx cap sync android
```

### 3. ビルド

```
npx cap open android
```

Android Studio で **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
→ `android\app\build\outputs\apk\debug\app-debug.apk` をスマホへ送ってインストール。

### 4. 更新時

```
npm run sync-www
npx cap sync android
```

で再ビルド。

## トラブルシューティング

- **写真一覧が空 / 許可を求められる** → 設定 → アプリ → フォトベンチ → 権限 → 写真と動画を許可
- **minSdkVersionエラー(方法B)** → `android\variables.gradle` の `minSdkVersion` を `29` に変更
- **Gradleエラー** → File → Sync Project with Gradle Files。だめならAndroid Studioを更新
