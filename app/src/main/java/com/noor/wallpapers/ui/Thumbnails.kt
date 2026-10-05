package com.noor.wallpapers.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

/** Renders gallery thumbnails a few at a time and keeps them in memory. */
object Thumbnails {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(3)

    private val cache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** A third of the screen width keeps thumbnails sharp in a two-column grid. */
    fun size(context: Context): Pair<Int, Int> {
        val (w, h) = Wallpapers.screenSize(context)
        return (w / 3) to (h / 3)
    }

    fun cached(sel: Selection, w: Int, h: Int): Bitmap? = cache.get(key(sel, w, h))

    suspend fun load(context: Context, sel: Selection, w: Int, h: Int): Bitmap {
        cache.get(key(sel, w, h))?.let { return it }
        val bmp = withContext(dispatcher) { Wallpapers.renderNow(context, sel, w, h) }
        cache.put(key(sel, w, h), bmp)
        return bmp
    }

    private fun key(sel: Selection, w: Int, h: Int) = "${sel.encode()}@${w}x$h"
}

@Composable
fun rememberThumbnail(sel: Selection): State<ImageBitmap?> {
    val context = LocalContext.current
    val (w, h) = Thumbnails.size(context)
    return produceState(Thumbnails.cached(sel, w, h)?.asImageBitmap(), sel) {
        value = Thumbnails.load(context, sel, w, h).asImageBitmap()
    }
}
