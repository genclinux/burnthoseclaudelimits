package com.noor.wallpapers.wallpaper

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.noor.wallpapers.art.Ambient
import com.noor.wallpapers.art.RenderContext

/**
 * Live wallpaper: the chosen design rendered once at full resolution, with
 * twinkling stars and a drifting band of light animated on top. Animation runs
 * only while visible and at a modest frame rate to be kind to the battery.
 */
class NoorLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = NoorEngine()

    private inner class NoorEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val thread = HandlerThread("noor-live").apply { start() }
        private val handler = Handler(thread.looper)
        private val prefs = Prefs(this@NoorLiveWallpaperService)
        private val renderer = AndroidRenderer(this@NoorLiveWallpaperService)

        @Volatile private var width = 0
        @Volatile private var height = 0
        private var still: Bitmap? = null
        private var ctx: RenderContext? = null
        @Volatile private var visible = false
        private val start = SystemClock.uptimeMillis()

        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, FRAME_MS)
            }
        }

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
                if (this.visible) handler.post(frame)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key == Prefs.KEY_LIVE) handler.post { rebuild(); drawFrame() }
        }

        /** Runs on the render thread. */
        private fun rebuild() {
            if (width <= 0 || height <= 0) return
            val sel = prefs.liveSelection
            // The surface follows rotation, so on a tablet this re-lays out for each orientation.
            val c = Wallpapers.viewportContext(this@NoorLiveWallpaperService, sel, width, height)
            val bmp = renderer.renderBitmap(sel.entry.render(c))
            still?.recycle()
            still = bmp
            ctx = c
        }

        private fun drawFrame() {
            val bmp = still ?: run { rebuild(); still } ?: return
            val c = ctx ?: return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockHardwareCanvas() ?: return
                canvas.drawColor(Color.BLACK)
                canvas.drawBitmap(bmp, 0f, 0f, null)
                val t = (SystemClock.uptimeMillis() - start) / 1000.0
                renderer.draw(canvas, Ambient.overlay(c, t))
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
