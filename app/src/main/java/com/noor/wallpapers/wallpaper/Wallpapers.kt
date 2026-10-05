package com.noor.wallpapers.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.DesignOptions
import com.noor.wallpapers.art.Entry
import com.noor.wallpapers.art.Palette
import com.noor.wallpapers.art.RenderContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A catalog entry rendered in a particular palette and seed, with her own options. */
data class Selection(
    val entryId: String,
    val paletteId: String,
    val seed: Int,
    val options: DesignOptions = DesignOptions.DEFAULT,
) {
    val entry: Entry get() = Catalog.byId(entryId) ?: Catalog.entries.first()
    val palette: Palette get() = Palette.byId(paletteId)

    fun encode() = "$entryId|$paletteId|$seed" + if (options.isDefault) "" else "|" + options.encode()

    companion object {
        fun of(entry: Entry) = Selection(entry.id, entry.defaultPalette.id, entry.defaultSeed)

        fun decode(s: String?): Selection? {
            val parts = s?.split('|') ?: return null
            if (parts.size !in 3..4) return null
            val seed = parts[2].toIntOrNull() ?: return null
            if (Catalog.byId(parts[0]) == null) return null
            return Selection(parts[0], parts[1], seed, DesignOptions.decode(parts.getOrNull(3)))
        }
    }
}

enum class Target(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

object Wallpapers {
    /**
     * Full physical panel size in portrait. On the OPPO Find X9 Pro this is 1272 x 2772
     * at the "High" resolution setting, or 1080-wide if ColorOS is set to save power.
     */
    fun screenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            // displayMetrics excludes the system bars; the wallpaper must cover the whole panel.
            val m = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
        val width = minOf(w, h).takeIf { it > 0 } ?: RenderContext.REFERENCE_WIDTH
        val height = maxOf(w, h).takeIf { it > 0 } ?: RenderContext.REFERENCE_HEIGHT
        return width to height
    }

    /** Tablets (smallest width 600dp+) rotate, so their wallpapers must work both ways up. */
    fun isTablet(context: Context) = context.resources.configuration.smallestScreenWidthDp >= 600

    /** Size of the current window (rotates with the device). Needs an Activity context for accuracy. */
    fun windowSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.currentWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
    }

    /**
     * Context for drawing what a [width] x [height] view shows. On a tablet the
     * focal content goes in the centred square, exactly as in the real wallpaper.
     */
    fun viewportContext(context: Context, sel: Selection, width: Int, height: Int): RenderContext =
        if (isTablet(context)) RenderContext.tablet(width, height, minOf(width, height).toDouble(), sel.palette, sel.seed, sel.options)
        else RenderContext(width, height, sel.palette, sel.seed, options = sel.options)

    /**
     * Context for the bitmap handed to WallpaperManager. Phones get the exact
     * panel size. Tablets get a square as wide as the panel's long side: the
     * system centre-crops it to the screen in either orientation, and the
     * focal square (the panel's short side) is visible both ways.
     */
    fun wallpaperContext(context: Context, sel: Selection): RenderContext {
        val (short, long) = screenSize(context)
        return if (isTablet(context)) RenderContext.tablet(long, long, short.toDouble(), sel.palette, sel.seed, sel.options)
        else RenderContext(short, long, sel.palette, sel.seed, options = sel.options)
    }

    /** Renders on the calling thread. */
    fun renderNow(context: Context, sel: Selection, ctx: RenderContext): Bitmap =
        AndroidRenderer(context).renderBitmap(sel.entry.render(ctx))

    suspend fun renderViewport(context: Context, sel: Selection, width: Int, height: Int): Bitmap =
        withContext(Dispatchers.Default) { renderNow(context, sel, viewportContext(context, sel, width, height)) }

    suspend fun renderWallpaper(context: Context, sel: Selection): Bitmap =
        withContext(Dispatchers.Default) { renderNow(context, sel, wallpaperContext(context, sel)) }

    suspend fun apply(context: Context, bitmap: Bitmap, target: Target) = withContext(Dispatchers.IO) {
        val wm = WallpaperManager.getInstance(context)
        // Phones: exact panel size, so ColorOS shows it 1:1. Tablets: a square the system centre-crops.
        wm.setBitmap(bitmap, null, true, target.flags)
    }

    suspend fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/noor by HBS")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create gallery entry")
        try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: error("Could not open gallery entry")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        uri
    }

    /** Stores [sel] for the live wallpaper and opens the system live-wallpaper preview. */
    fun liveWallpaperIntent(context: Context, sel: Selection): Intent {
        Prefs(context).liveSelection = sel
        return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, NoorLiveWallpaperService::class.java),
        )
    }
}

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("noor", Context.MODE_PRIVATE)

    var favorites: Set<String>
        get() = sp.getStringSet(KEY_FAVORITES, emptySet())!!.toSet()
        set(value) = sp.edit().putStringSet(KEY_FAVORITES, value).apply()

    var liveSelection: Selection
        get() = Selection.decode(sp.getString(KEY_LIVE, null)) ?: Selection.of(Catalog.entries.first())
        set(value) = sp.edit().putString(KEY_LIVE, value.encode()).apply()

    /** Whether the first-launch welcome for Hanife Betül has been shown. */
    var welcomed: Boolean
        get() = sp.getBoolean("welcomed", false)
        set(value) = sp.edit().putBoolean("welcomed", value).apply()

    /**
     * Ids of the [com.noor.wallpapers.WhatsNew] features she has been shown.
     * 1.2.4–1.2.6 had a single rotation popup; having seen it counts as seeing "rotation".
     */
    var seenFeatures: Set<String>
        get() {
            val seen = sp.getStringSet("seen_features", emptySet())!!.toSet()
            return if (sp.getBoolean("rotation_intro_seen", false)) seen + "rotation" else seen
        }
        set(value) = sp.edit().putStringSet("seen_features", value).apply()

    /** Last palette/seed the user picked per entry, so the gallery remembers customisations. */
    fun customised(entryId: String): Selection? = Selection.decode(sp.getString("sel_$entryId", null))

    fun saveCustomised(sel: Selection) = sp.edit().putString("sel_${sel.entryId}", sel.encode()).apply()

    fun registerLiveListener(l: android.content.SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregisterLiveListener(l: android.content.SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_LIVE = "live_selection"
    }
}
