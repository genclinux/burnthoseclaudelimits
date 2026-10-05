package com.noor.wallpapers.wallpaper

import android.app.WallpaperManager
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.noor.wallpapers.art.Ambient
import com.noor.wallpapers.art.Item
import com.noor.wallpapers.art.RenderContext
import com.noor.wallpapers.prayer.OverlayInfo
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.PrayerOverlay
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.PrayerRepository
import com.noor.wallpapers.ui.HanifeBetul
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay

/**
 * Live wallpaper: the chosen design rendered once at full resolution, with
 * twinkling stars and a drifting band of light animated on top, and, if she
 * likes, the prayer times in a gilded panel. It can follow the day (dawn at
 * İmsak, dusk at Akşam, stars at Yatsı), a touch sends a shooting star, and
 * on her birthday rose petals fall. Animation runs only while visible, at 20 fps.
 */
class NoorLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = NoorEngine()

    private inner class NoorEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val thread = HandlerThread("noor-live").apply { start() }
        private val handler = Handler(thread.looper)
        private val prefs = Prefs(this@NoorLiveWallpaperService)
        private val settings = AppSettings(this@NoorLiveWallpaperService)
        private val renderer = AndroidRenderer(this@NoorLiveWallpaperService)

        @Volatile private var width = 0
        @Volatile private var height = 0
        private var still: Bitmap? = null
        private var ctx: RenderContext? = null
        @Volatile private var visible = false
        private val start = SystemClock.uptimeMillis()

        /** The period the current still was drawn for, when following the day. */
        private var period: Prayer? = null
        private var periodCheckedAt = 0L

        /** The prayer panel, rebuilt once a minute (or when settings change). */
        private var overlay: List<Item> = emptyList()
        private var overlayMinute = -1L

        private val taps = ArrayList<Ambient.Tap>()
        private var birthday = false

        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, FRAME_MS)
            }
        }

        private fun seconds() = (SystemClock.uptimeMillis() - start) / 1000.0

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerLiveListener(this)
        }

        override fun onDestroy() {
            prefs.unregisterLiveListener(this)
            handler.removeCallbacksAndMessages(null)
            handler.post { still?.recycle(); still = null }
            thread.quitSafely()
            super.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            handler.post { rebuild() }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            // Reschedule on the render thread so two frame loops can never overlap.
            handler.post {
                handler.removeCallbacks(frame)
                if (this.visible) {
                    // The day may have moved on while hidden.
                    periodCheckedAt = 0L
                    overlayMinute = -1L
                    handler.post(frame)
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        /** Launchers send a tap on empty space: easter egg, a star shoots from her finger. */
        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) {
                handler.post {
                    val t = seconds()
                    taps.removeAll { t - it.time > Ambient.TAP_DURATION }
                    if (taps.size < 4) taps += Ambient.Tap(x.toDouble(), y.toDouble(), t)
                }
                HanifeBetul.find(applicationContext, HanifeBetul.Surprise.TAP_STAR)
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            when {
                key == Prefs.KEY_LIVE || key == AppSettings.KEY_LIVE_FOLLOWS -> handler.post { periodCheckedAt = 0L; rebuild(); drawFrame() }
                key != null && key in AppSettings.LIVE_KEYS -> handler.post { overlayMinute = -1L; birthday = isBirthday(); drawFrame() }
            }
        }

        private fun isBirthday() = settings.birthday == MonthDay.from(LocalDate.now())

        /** The design to show now: hers, or the one for this part of the day. */
        private fun selection(): Selection {
            if (!settings.liveFollowsPrayer) return prefs.liveSelection
            val ev = PrayerRepository.schedule(this@NoorLiveWallpaperService)?.current(Instant.now())
            period = ev?.prayer
            val kandil = ev != null && PrayerRepository.isKandilEvening(this@NoorLiveWallpaperService, ev.day.date)
            return TimeOfDay.selection(ev?.prayer, kandil)
        }

        /** Runs on the render thread. */
        private fun rebuild() {
            if (width <= 0 || height <= 0) return
            val sel = selection()
            // The surface follows rotation, so on a tablet this re-lays out for each orientation.
            val c = Wallpapers.viewportContext(this@NoorLiveWallpaperService, sel, width, height)
            val bmp = renderer.renderBitmap(sel.entry.render(c))
            still?.recycle()
            still = bmp
            ctx = c
            birthday = isBirthday()
            periodCheckedAt = SystemClock.uptimeMillis()
            overlayMinute = -1L
        }

        /** Once a minute: has the prayer period changed (new design) and what should the panel say? */
        private fun refreshMinute() {
            val now = SystemClock.uptimeMillis()
            if (settings.liveFollowsPrayer && now - periodCheckedAt > 60_000L) {
                periodCheckedAt = now
                val p = PrayerRepository.schedule(this@NoorLiveWallpaperService)?.current(Instant.now())?.prayer
                if (p != period) rebuild()
            }
            // After a rebuild the context (and its palette) may be new.
            val c = ctx ?: return
            val minute = System.currentTimeMillis() / 60_000L
            if (minute == overlayMinute) return
            overlayMinute = minute
            val position = settings.overlayPosition
            val schedule = if (position != null) PrayerRepository.schedule(this@NoorLiveWallpaperService) else null
            val info = schedule?.let { OverlayInfo.of(it, Instant.now()) }
            overlay = if (position != null && info != null) PrayerOverlay.items(c, info, settings.overlayStyle, position) else emptyList()
        }

        private fun drawFrame() {
            if (still == null) rebuild()
            refreshMinute()
            // Read both after refreshMinute: following the day may have just replaced them.
            val image = still ?: return
            val c = ctx ?: return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockHardwareCanvas() ?: return
                canvas.drawColor(Color.BLACK)
                canvas.drawBitmap(image, 0f, 0f, null)
                val t = seconds()
                renderer.draw(canvas, Ambient.overlay(c, t, taps, birthday))
                renderer.draw(canvas, overlay)
            } catch (_: IllegalStateException) {
                // Surface went away between frames.
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: IllegalStateException) {}
                }
            }
        }
    }

    companion object {
        private const val FRAME_MS = 50L // 20 fps is plenty for slow twinkles.
    }
}
