package com.noor.wallpapers

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.RenderContext
import com.noor.wallpapers.wallpaper.AndroidRenderer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Renders every wallpaper with the real Android text stack (HarfBuzz/Minikin and
 * the bundled Arabic fonts) at the Galaxy Tab S9 FE's resolution, both ways up,
 * plus the phone. CI pulls the PNGs off the emulator so they can be inspected:
 * the desktop preview tool can't be trusted for Arabic glyph extents.
 */
@RunWith(AndroidJUnit4::class)
class DevicePreviewTest {
    @Test
    fun renderAllOnDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val out = File(context.getExternalFilesDir(null), "device-previews").apply { deleteRecursively(); mkdirs() }
        val renderer = AndroidRenderer(context)
        val scale = 0.5
        val devices = listOf(
            Triple("tab_portrait", 1440, 2304),
            Triple("tab_landscape", 2304, 1440),
            Triple("phone", RenderContext.REFERENCE_WIDTH, RenderContext.REFERENCE_HEIGHT),
        )
        for ((name, w0, h0) in devices) {
            val w = (w0 * scale).toInt()
            val h = (h0 * scale).toInt()
            val dir = File(out, name).apply { mkdirs() }
            for (e in Catalog.entries) {
                val ctx = if (name == "phone") RenderContext(w, h, e.defaultPalette, e.defaultSeed)
                else RenderContext.tablet(w, h, minOf(w, h).toDouble(), e.defaultPalette, e.defaultSeed)
                val bmp = renderer.renderBitmap(e.render(ctx))
                File(dir, "${e.id}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bmp.recycle()
            }
        }
        assertTrue(File(out, "tab_portrait").list()!!.isNotEmpty())
    }
}
