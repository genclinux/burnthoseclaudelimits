package com.noor.wallpapers.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.LruCache
import com.noor.wallpapers.wallpaper.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * "Gerçek Camiler": the search results for each place are kept on the phone
 * for a month, thumbnails in the cache, and full photos once downloaded, so
 * the rotation and live wallpaper work offline after the first time.
 */
object Photos {
    private const val MAX_AGE_MS = 30L * 24 * 3600 * 1000

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }
    private fun resultFile(context: Context, place: Place) = File(dir(context), "search-${place.id}.json")

    @Volatile private var memo: Pair<Long, List<Photo>>? = null

    /** Why the last search failed, shown when there is nothing to show. */
    @Volatile var lastError: String? = null
        private set

    private val thumbs = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount
    }

    /** What is on the phone already, without the network. */
    fun cached(context: Context): List<Photo> {
        val files = Places.ALL.map { resultFile(context, it) }
        val stamp = files.sumOf { if (it.exists()) it.lastModified() xor it.length() else 0L }
        memo?.let { (s, list) -> if (s == stamp) return list }
        val found = Places.ALL.associate { p ->
            val f = resultFile(context, p)
            p.id to (if (f.exists()) runCatching { Commons.parse(f.readText(), p) }.getOrDefault(emptyList()) else emptyList())
        }
        val list = Commons.choose(Places.ALL, found)
        memo = stamp to list
        return list
    }

    fun byId(context: Context, id: String): Photo? =
        if (!Photo.isPhotoId(id)) null else cached(context).firstOrNull { it.id == id }

    /**
     * Searches Commons for every place whose results are missing or a month
     * old, a few at a time. A place that fails keeps its old results.
     */
    suspend fun refresh(context: Context, force: Boolean = false, onProgress: (List<Photo>) -> Unit = {}): List<Photo> = withContext(Dispatchers.IO) {
        val gate = Semaphore(4)
        coroutineScope {
            Places.ALL.map { place ->
                async {
                    val f = resultFile(context, place)
                    val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE_MS
                    if (fresh && !force) return@async
                    gate.withPermit {
                        runCatching {
                            val text = String(get(Commons.searchUrl(place)), Charsets.UTF_8)
                            Commons.parse(text, place) // only keep a response that parses
                            val tmp = File(f.path + ".tmp")
                            tmp.writeText(text)
                            tmp.renameTo(f)
                            // Show each place as it arrives (Pendik's are asked for first) instead of after all of them.
                            onProgress(cached(context))
                        }.onFailure { lastError = it.message ?: it.javaClass.simpleName }
                    }
                }
            }.awaitAll()
        }
        cached(context)
    }

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        // Wikimedia asks every client to say who it is.
        conn.setRequestProperty("User-Agent", Commons.USER_AGENT)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads [url] into [file] once. Blocking. */
    private fun download(url: String, file: File): File {
        if (file.exists() && file.length() > 0) return file
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeBytes(get(url))
        if (!tmp.renameTo(file)) throw IOException("Could not save ${file.name}")
        return file
    }

    /** A small picture for the grid. */
    suspend fun thumbnail(context: Context, photo: Photo): Bitmap = withContext(Dispatchers.IO) {
        thumbs.get(photo.pageId)?.let { return@withContext it }
        val file = download(photo.urlForWidth(500), File(File(context.cacheDir, "photo-thumbs"), "${photo.pageId}.jpg"))
        val bmp = BitmapFactory.decodeFile(file.path) ?: throw IOException("Bad image")
        thumbs.put(photo.pageId, bmp)
        bmp
    }

    fun cachedThumbnail(photo: Photo): Bitmap? = thumbs.get(photo.pageId)

    /**
     * The photo centre-cropped to exactly [w] x [h], downloading it the first
     * time. Blocking: call off the main thread.
     */
    fun cropped(context: Context, photo: Photo, w: Int, h: Int): Bitmap {
        val want = photo.widthToCover(w, h)
        val url = photo.urlForWidth(want)
        val bucket = url.substringAfterLast('/').substringBefore("px-").toIntOrNull() ?: 0
        val file = download(url, File(File(dir(context), "full"), "${photo.pageId}-$bucket.jpg"))

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        // Decode at the largest power-of-two reduction that still covers the screen.
        val coverWidth = bounds.outWidth * maxOf(w / bounds.outWidth.toDouble(), h / bounds.outHeight.toDouble())
        var sample = 1
        while (bounds.outWidth / (sample * 2.0) >= coverWidth) sample *= 2
        val src = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Bad image")

        val scale = maxOf(w / src.width.toFloat(), h / src.height.toFloat())
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val m = Matrix().apply {
            postScale(scale, scale)
            postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f)
        }
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        src.recycle()
        return out
    }

    /** The bitmap handed to WallpaperManager: the panel on a phone, a square on a tablet (as for designs). */
    fun wallpaperBitmap(context: Context, photo: Photo): Bitmap {
        val (short, long) = Wallpapers.screenSize(context)
        return if (Wallpapers.isTablet(context)) cropped(context, photo, long, long) else cropped(context, photo, short, long)
    }

    suspend fun wallpaper(context: Context, photo: Photo): Bitmap = withContext(Dispatchers.IO) { wallpaperBitmap(context, photo) }

    suspend fun viewport(context: Context, photo: Photo, w: Int, h: Int): Bitmap = withContext(Dispatchers.IO) { cropped(context, photo, w, h) }
}
