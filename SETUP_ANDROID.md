# フォトベンチ Android ネイティブ版(APK)ビルド手順

ネイティブ版では以下が可能になります(PWA版との違い):

- 起動すると端末内の写真が自動でサムネイル一覧表示される(ファイル選択画面は不要)
- タップしてすぐ編集、保存は元の写真と同じフォルダへ(不可の場合は Pictures/PhotoBench へ)

対応OS: Android 10 以上

## 1. 必要なソフト(PCに1回だけインストール)

1. **Node.js LTS** — https://nodejs.org/ja からインストーラをダウンロードして実行
2. **Android Studio** — https://developer.android.com/studio からインストール
   - 初回起動時のセットアップウィザードは全て標準設定でOK(Android SDKが自動で入ります)

## 2. プロジェクトの準備(1回だけ)

コマンドプロンプトで:

```
cd C:\Users\yamashita_y00031\Documents\github\easy_image_editor
npm install
npm run sync-www
npx cap add android
npx cap sync android
```

## 3. APKのビルド

```
npx cap open android
```

Android Studio が開いたら:

1. 初回はGradleの同期が終わるまで数分待つ(画面下のプログレスバー)
2. メニュー **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
3. 完了通知の **locate** をクリック → `app-debug.apk` ができている
   (場所: `android\app\build\outputs\apk\debug\app-debug.apk`)

## 4. スマホへのインストール

1. `app-debug.apk` をスマホへ送る(Googleドライブ・USBケーブル・Bluetoothなど)
2. スマホでAPKファイルを開く
3. 「提供元不明のアプリ」の警告が出たら、インストールを許可する
4. 初回起動時に「写真へのアクセス」を **許可** する

## 5. アプリを更新するとき

`index.html` を変更したら:

```
npm run sync-www
npx cap sync android
npx cap open android
```

で再ビルド → APKを再インストール。

## トラブルシューティング

- **写真一覧が空 / 「アクセスを許可してください」と出る** → 設定 → アプリ → フォトベンチ → 権限 → 写真と動画 を許可
- **Gradleエラーが出る** → Android Studio のメニュー File → Sync Project with Gradle Files を実行。それでもだめならAndroid Studioを最新版に更新
- **JDKバージョンのエラー** → Android Studio 内蔵のJDKを使うため、通常は追加設定不要。File → Settings → Build Tools → Gradle → Gradle JDK が「Embedded JDK」になっているか確認
