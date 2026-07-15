package jp.photobench.medialib

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Size
import java.io.ByteArrayOutputStream
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback

@CapacitorPlugin(
    name = "MediaLib",
    permissions = [
        Permission(strings = [Manifest.permission.READ_MEDIA_IMAGES], alias = "photos13"),
        Permission(strings = [Manifest.permission.READ_EXTERNAL_STORAGE], alias = "photos")
    ]
)
class MediaLibPlugin : Plugin() {

    private fun photoAlias(): String =
        if (Build.VERSION.SDK_INT >= 33) "photos13" else "photos"

    /* ---------- 写真一覧(ファイルパス付き・高速) ---------- */
    @PluginMethod
    fun list(call: PluginCall) {
        if (getPermissionState(photoAlias()) != PermissionState.GRANTED) {
            requestPermissionForAlias(photoAlias(), call, "listPermCallback")
            return
        }
        doList(call)
    }

    @PermissionCallback
    private fun listPermCallback(call: PluginCall) {
        if (getPermissionState(photoAlias()) == PermissionState.GRANTED) {
            doList(call)
        } else {
            call.reject("写真へのアクセスが許可されませんでした")
        }
    }

    @Suppress("DEPRECATION")
    private fun doList(call: PluginCall) {
        val limit = call.getInt("limit") ?: 300
        val resolver = context.contentResolver
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATA
        )
        val images = JSArray()
        try {
            val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC"
            )
            cursor?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val iPath = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val iMime = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val iData = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                var n = 0
                while (c.moveToNext() && n < limit) {
                    val mime = c.getString(iMime) ?: ""
                    // 静止画のみ(GIF・動画は除外)
                    if (!mime.startsWith("image/") || mime == "image/gif") continue
                    val o = JSObject()
                    o.put("id", c.getLong(iId).toString())
                    o.put("name", c.getString(iName) ?: "photo.jpg")
                    o.put("mtime", c.getLong(iDate) * 1000)
                    o.put("relPath", c.getString(iPath) ?: "")
                    o.put("mime", mime)
                    o.put("path", c.getString(iData) ?: "")
                    images.put(o)
                    n++
                }
            }
        } catch (e: Exception) {
            call.reject("一覧の取得に失敗しました: " + e.message)
            return
        }
        val ret = JSObject()
        ret.put("images", images)
        call.resolve(ret)
    }

    /* ---------- サムネイル1枚生成(表示範囲のみ順次呼ばれる) ---------- */
    @PluginMethod
    fun thumb(call: PluginCall) {
        val id = call.getString("id")
        if (id == null) {
            call.reject("id が必要です")
            return
        }
        val size = call.getInt("size") ?: 256
        try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong()
            )
            val bmp = context.contentResolver.loadThumbnail(uri, Size(size, size), null)
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos)
            val ret = JSObject()
            ret.put("data", Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP))
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("サムネイル生成エラー: " + e.message)
        }
    }

    /* ---------- 1枚読み込み(フル解像度・フォールバック用) ---------- */
    @PluginMethod
    fun read(call: PluginCall) {
        val id = call.getString("id")
        if (id == null) {
            call.reject("id が必要です")
            return
        }
        try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong()
            )
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                call.reject("読み込めませんでした")
                return
            }
            val ret = JSObject()
            ret.put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("読み込みエラー: " + e.message)
        }
    }

    /* ---------- 保存(元のフォルダへ。不可なら Pictures/PhotoBench へ) ---------- */
    @PluginMethod
    fun save(call: PluginCall) {
        val name = call.getString("name")
        val data = call.getString("data")
        if (name == null || data == null) {
            call.reject("name と data が必要です")
            return
        }
        val mime = call.getString("mime") ?: "image/jpeg"
        val wanted = (call.getString("relPath") ?: "").trim('/')
        val bytes = try {
            Base64.decode(data, Base64.DEFAULT)
        } catch (e: Exception) {
            call.reject("データが不正です")
            return
        }
        val fallback = "Pictures/PhotoBench"
        val first = if (wanted.isBlank()) fallback else wanted
        val used = try {
            insertImage(name, mime, first, bytes)
            first
        } catch (e: Exception) {
            try {
                insertImage(name, mime, fallback, bytes)
                fallback
            } catch (e2: Exception) {
                call.reject("保存エラー: " + e2.message)
                return
            }
        }
        val ret = JSObject()
        ret.put("relPath", used)
        call.resolve(ret)
    }

    private fun insertImage(name: String, mime: String, relPath: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, relPath)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw RuntimeException("URIを作成できませんでした")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw RuntimeException("書き込みに失敗しました")
    }
}
