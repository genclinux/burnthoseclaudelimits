package com.noor.wallpapers.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.noor.wallpapers.MainActivity
import com.noor.wallpapers.R
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.RenderContext
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.wallpaper.AndroidRenderer
import com.noor.wallpapers.wallpaper.Selection
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Home-screen widget: the next prayer with a live countdown and today's six
 * times, over a design from the gallery (Betül'ün Gecesi unless she picks
 * another from a design's "apply" sheet).
 */
class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext
        Background.run(this) { updateAll(app) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        val app = context.applicationContext
        Background.run(this) { update(app, manager, id) }
    }

    companion object {
        private const val DEFAULT_ART = "hb-stars"
        private val CELLS = listOf(R.id.widget_cell0, R.id.widget_cell1, R.id.widget_cell2, R.id.widget_cell3, R.id.widget_cell4, R.id.widget_cell5)
        private val NAMES = listOf(R.id.widget_name0, R.id.widget_name1, R.id.widget_name2, R.id.widget_name3, R.id.widget_name4, R.id.widget_name5)
        private val TIMES = listOf(R.id.widget_time0, R.id.widget_time1, R.id.widget_time2, R.id.widget_time3, R.id.widget_time4, R.id.widget_time5)

        private const val WHITE = 0xF2FFFFFF.toInt()
        private const val DIM = 0x8CFFFFFF.toInt()
        private const val GOLD = 0xFFF2D27A.toInt()

        @Volatile private var artCache: Pair<String, Bitmap>? = null

        /** Redraws every placed widget. Call from a background thread: it renders artwork. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidget::class.java))
            for (id in ids) update(context, manager, id)
        }

        fun selection(context: Context): Selection =
            Selection.decode(AppSettings(context).widgetSelection) ?: Selection.of(Catalog.byId(DEFAULT_ART) ?: Catalog.entries.first())

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val density = context.resources.displayMetrics.density
            val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).coerceAtLeast(110)
            val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 120).coerceAtLeast(50)
            val art = art(context, (wDp * density).roundToInt(), (hDp * density).roundToInt(), 20 * density)

            val large = build(context, R.layout.widget_prayer, art, withTimes = true)
            val small = build(context, R.layout.widget_prayer_small, art, withTimes = false)
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(mapOf(SizeF(110f, 50f) to small, SizeF(200f, 105f) to large))
            } else {
                if (hDp < 100) small else large
            }
            manager.updateAppWidget(id, views)
        }

        private fun build(context: Context, layout: Int, art: Bitmap?, withTimes: Boolean): RemoteViews {
            val v = RemoteViews(context.packageName, layout)
            if (art != null) v.setImageViewBitmap(R.id.widget_art, art)
            v.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, 1,
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_PRAYER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            val schedule = PrayerRepository.schedule(context)
            val now = Instant.now()
            val next = schedule?.next(now)
            val today = schedule?.day(schedule.today(now))
            if (schedule == null || next == null || today == null) {
                v.setTextViewText(R.id.widget_place, "noor by HBS")
                v.setTextViewText(R.id.widget_next, "Konumunu seçmek için dokun")
                v.setViewVisibility(R.id.widget_countdown_box, View.GONE)
                if (withTimes) v.setViewVisibility(R.id.widget_times, View.GONE)
                return v
            }
            v.setTextViewText(R.id.widget_place, schedule.location.label)
            val hijri = today.hijri ?: PrayerRepository.hijri(context).of(today.date)
            v.setTextViewText(R.id.widget_hijri, hijri.toString())
            v.setTextViewText(R.id.widget_next, "${next.prayer.title}  ${TurkishText.hhmm(next.time)}")
            v.setViewVisibility(R.id.widget_countdown_box, View.VISIBLE)
            val remaining = Duration.between(now, next.instant).toMillis()
            v.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + remaining, null, true)
            v.setChronometerCountDown(R.id.widget_countdown, true)
            if (withTimes) {
                v.setViewVisibility(R.id.widget_times, View.VISIBLE)
                val current = schedule.current(now)?.prayer
                Prayer.entries.forEachIndexed { i, p ->
                    val lit = p == current
                    val past = today.instant(p).isBefore(now) && !lit
                    v.setTextViewText(NAMES[i], p.title)
                    v.setTextViewText(TIMES[i], TurkishText.hhmm(today.time(p)))
                    val color = when {
                        lit -> GOLD
                        past -> DIM
                        else -> WHITE
                    }
                    v.setTextColor(NAMES[i], color)
                    v.setTextColor(TIMES[i], color)
                    v.setInt(CELLS[i], "setBackgroundResource", if (lit) R.drawable.widget_cell_current else 0)
                }
            }
            return v
        }

        /** The chosen design at the widget's size, darkened for legibility and with rounded corners. */
        private fun art(context: Context, w0: Int, h0: Int, radius: Float): Bitmap? {
            // Keep the bitmap modest: RemoteViews carry it across processes.
            val scale = minOf(1f, 900f / w0, 600f / h0)
            val w = (w0 * scale).roundToInt().coerceAtLeast(64)
            val h = (h0 * scale).roundToInt().coerceAtLeast(32)
            val sel = selection(context)
            val key = "${sel.encode()}@${w}x$h"
            artCache?.let { (k, b) -> if (k == key && !b.isRecycled) return b }
            return try {
                val ctx = RenderContext.tablet(w, h, minOf(w, h).toDouble(), sel.palette, sel.seed, sel.options)
                val raw = AndroidRenderer(context).renderBitmap(sel.entry.render(ctx))
                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
                val r = radius * scale
                c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(raw, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) })
                c.drawRoundRect(
                    rect, r, r,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0x73000000, 0x99000000.toInt(), 0xC7000000.toInt()), null, Shader.TileMode.CLAMP)
                    },
                )
                raw.recycle()
                artCache = key to out
                out
            } catch (_: Exception) {
                null
            }
        }
    }
}
