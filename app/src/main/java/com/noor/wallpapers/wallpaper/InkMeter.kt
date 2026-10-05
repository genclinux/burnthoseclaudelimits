package com.noor.wallpapers.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Measures where a line of text actually puts ink, by drawing it off-screen
 * and scanning the pixels.
 *
 * Font metrics and Paint.getTextBounds can't be trusted for Arabic calligraphy:
 * Ruqaa and Naskh swashes and stacked vowel marks reach well past the font's
 * declared ascent/descent. Aref Ruqaa's "رمضان كريم" goes 0.35 em below the
 * baseline against a declared descent of 0.24 em, and on Samsung tablets the
 * reported bounds were smaller still, so the calligraphy ran into captions.
 * Pixels can't be wrong.
 */
object InkMeter {
    /** Probe size in px. Ink scales linearly with text size, so one probe per string is enough. */
    private const val PROBE = 160f

    /** Ink box per (typeface, spacing, text), in ems relative to a centre-aligned anchor on the baseline. */
    private val cache = ConcurrentHashMap<String, RectF>()

    /**
     * Ink bounds for [text] drawn with [paint] (centre-aligned) at its current
     * text size. Coordinates are relative to the anchor point: x from the
     * anchor, y from the baseline (negative is above).
     */
    fun measure(text: String, paint: Paint): RectF {
        val key = "${System.identityHashCode(paint.typeface)}|${paint.letterSpacing}|$text"
        val em = cache.getOrPut(key) { probe(text, paint) }
        val s = paint.textSize
        return RectF(em.left * s, em.top * s, em.right * s, em.bottom * s)
    }

    private fun probe(text: String, src: Paint): RectF {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = src.typeface
            letterSpacing = src.letterSpacing
            textAlign = Paint.Align.CENTER
            textSize = PROBE
            color = Color.BLACK
        }
        // Generous canvas: swashes can overhang the advance, marks can stack high.
        val w = (p.measureText(text) * 1.5f + PROBE * 3).toInt().coerceAtLeast(1)
        val h = (PROBE * 5).toInt()
        val ox = w / 2f
        val oy = PROBE * 3f
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        try {
            Canvas(bmp).drawText(text, ox, oy, p)
            val stride = bmp.rowBytes
            val pixels = ByteArray(stride * h)
            bmp.copyPixelsToBuffer(ByteBuffer.wrap(pixels))
            var minX = Int.MAX_VALUE; var maxX = -1; var minY = Int.MAX_VALUE; var maxY = -1
            for (y in 0 until h) {
                val row = y * stride
                for (x in 0 until w) {
                    if ((pixels[row + x].toInt() and 0xFF) > 8) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            if (maxX < 0) {
                // Nothing drawn (e.g. blank text): fall back to the font metrics.
                val fm = p.fontMetrics
                val half = p.measureText(text) / 2
                return RectF(-half / PROBE, fm.ascent / PROBE, half / PROBE, fm.descent / PROBE)
            }
            return RectF(
                (minX - ox) / PROBE, (minY - oy) / PROBE,
                (maxX + 1 - ox) / PROBE, (maxY + 1 - oy) / PROBE,
            )
        } finally {
            bmp.recycle()
        }
    }
}
