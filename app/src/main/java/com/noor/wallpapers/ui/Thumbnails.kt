package com.noor.wallpapers.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
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

    fun cached(sel: Selection, size: IntSize): Bitmap? = cache.get(key(sel, size))

    suspend fun load(context: Context, sel: Selection, size: IntSize): Bitmap {
        cache.get(key(sel, size))?.let { return it }
        val bmp = withContext(dispatcher) {
            Wallpapers.renderNow(context, sel, Wallpapers.viewportContext(context, sel, size.width, size.height))
        }
        cache.put(key(sel, size), bmp)
        return bmp
    }

    private fun key(sel: Selection, size: IntSize) = "${sel.encode()}@${size.width}x${size.height}"
}

/** The window size in pixels; changes when a tablet rotates. */
@Composable
fun rememberViewport(): IntSize {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    return remember(config.orientation, config.screenWidthDp, config.screenHeightDp) {
        val (w, h) = Wallpapers.windowSize(context)
        IntSize(w.coerceAtLeast(1), h.coerceAtLeast(1))
    }
}

/** Thumbnails are a third of the window size: sharp in the grid, cheap to render. */
@Composable
fun rememberThumbnailSize(): IntSize {
    val v = rememberViewport()
    val scale = if (Wallpapers.isTablet(LocalContext.current)) 4 else 3
    return IntSize(v.width / scale, v.height / scale)
}

@Composable
fun rememberThumbnail(sel: Selection): State<ImageBitmap?> {
    val context = LocalContext.current
    val size = rememberThumbnailSize()
    return produceState(Thumbnails.cached(sel, size)?.asImageBitmap(), sel, size) {
        value = Thumbnails.load(context, sel, size).asImageBitmap()
    }
}
