package com.noor.wallpapers.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.noor.wallpapers.art.Blend
import com.noor.wallpapers.art.Colors
import com.noor.wallpapers.art.Fill
import com.noor.wallpapers.art.FillItem
import com.noor.wallpapers.art.FontId
import com.noor.wallpapers.art.GroupItem
import com.noor.wallpapers.art.Item
import com.noor.wallpapers.art.LinearFill
import com.noor.wallpapers.art.Path
import com.noor.wallpapers.art.RadialFill
import com.noor.wallpapers.art.RasterItem
import com.noor.wallpapers.art.Scene
import com.noor.wallpapers.art.SolidFill
import com.noor.wallpapers.art.StrokeItem
import com.noor.wallpapers.art.TextItem
import android.graphics.Path as AndroidPath

/** Draws platform-neutral [Scene]s onto an Android [Canvas]. */
class AndroidRenderer(context: Context) {
    private val typefaces: Map<FontId, Typeface> = Fonts.get(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    fun renderBitmap(scene: Scene): Bitmap {
        val bmp = Bitmap.createBitmap(scene.width, scene.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)
        draw(canvas, scene.items)
        return bmp
    }

    fun draw(canvas: Canvas, items: List<Item>, alpha: Float = 1f) {
        for (item in items) draw(canvas, item, alpha)
    }

    private fun draw(canvas: Canvas, item: Item, alpha: Float) {
        when (item) {
            is FillItem -> {
                reset(item.fill, item.alpha * alpha)
                paint.blendMode = item.blend.toAndroid()
                paint.style = Paint.Style.FILL
                canvas.drawPath(item.path.toAndroid(), paint)
            }
            is StrokeItem -> {
                reset(item.fill, item.alpha * alpha)
                paint.blendMode = item.blend.toAndroid()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = item.width
                paint.strokeCap = if (item.round) Paint.Cap.ROUND else Paint.Cap.BUTT
                paint.strokeJoin = if (item.round) Paint.Join.ROUND else Paint.Join.MITER
                canvas.drawPath(item.path.toAndroid(), paint)
            }
            is TextItem -> drawText(canvas, item, alpha)
            is RasterItem -> drawRaster(canvas, item, alpha)
            is GroupItem -> {
                val save = canvas.save()
                item.clip?.let { canvas.clipPath(it.toAndroid()) }
                draw(canvas, item.items, alpha * item.alpha)
                canvas.restoreToCount(save)
            }
        }
    }

    private fun drawRaster(canvas: Canvas, r: RasterItem, alpha: Float) {
        val bmp = Bitmap.createBitmap(r.pixels, r.width, r.height, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        p.alpha = (255 * (alpha * r.alpha).coerceIn(0f, 1f)).toInt()
        p.blendMode = r.blend.toAndroid()
        val dst = RectF(r.left, r.top, r.right, r.bottom)
        if (r.tiled) {
            p.shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
                setLocalMatrix(Matrix().apply { setScale(r.tileScale, r.tileScale) })
            }
            canvas.drawRect(dst, p)
        } else {
            canvas.drawBitmap(bmp, null, dst, p)
        }
        // A hardware canvas draws later, so the bitmap must outlive this call there.
        if (!canvas.isHardwareAccelerated) bmp.recycle()
    }

    private fun Blend.toAndroid(): BlendMode = when (this) {
        Blend.NORMAL -> BlendMode.SRC_OVER
        Blend.MULTIPLY -> BlendMode.MULTIPLY
        Blend.SCREEN -> BlendMode.SCREEN
    }

    private fun drawText(canvas: Canvas, t: TextItem, alpha: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.typeface = typefaces[t.font] ?: Typeface.DEFAULT
        paint.textSize = t.size
        paint.letterSpacing = t.letterSpacing
        paint.textAlign = Paint.Align.CENTER
        var x = t.cx
        val baseline: Float
        if (t.inkCentered) {
            // Fit and centre the ink actually drawn, measured from pixels (see InkMeter).
            val ink = InkMeter.measure(t.text, paint)
            val k = minOf(1f, t.maxWidth / ink.width(), t.maxHeight / ink.height())
            paint.textSize = t.size * k
            x = t.cx - (ink.left + ink.right) * k / 2f
            baseline = t.cy - (ink.top + ink.bottom) * k / 2f
        } else {
            val width = paint.measureText(t.text)
            if (width > t.maxWidth) paint.textSize = t.size * t.maxWidth / width
            val fm = paint.fontMetrics
            baseline = t.cy - (fm.ascent + fm.descent) / 2f
        }

        if (t.glowRadius > 0f && t.glowColor != 0) {
            val glow = Paint(paint)
            glow.color = t.glowColor
            glow.alpha = (Colors.alpha(t.glowColor) * alpha * t.alpha).toInt().coerceIn(0, 255)
            glow.maskFilter = BlurMaskFilter(t.glowRadius, BlurMaskFilter.Blur.NORMAL)
            canvas.drawText(t.text, x, baseline, glow)
        }
        applyFill(t.fill, t.alpha * alpha)
        canvas.drawText(t.text, x, baseline, paint)
    }

    private fun reset(fill: Fill, alpha: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.isDither = true
        applyFill(fill, alpha)
    }

    private fun applyFill(fill: Fill, alpha: Float) {
        val a = alpha.coerceIn(0f, 1f)
        when (fill) {
            is SolidFill -> {
                paint.shader = null
                paint.color = fill.color
                paint.alpha = (Colors.alpha(fill.color) * a).toInt()
            }
            is LinearFill -> {
                paint.shader = LinearGradient(fill.x0, fill.y0, fill.x1, fill.y1, fill.colors, fill.stops, Shader.TileMode.CLAMP)
                paint.color = Color.BLACK
                paint.alpha = (255 * a).toInt()
            }
            is RadialFill -> {
                paint.shader = RadialGradient(
                    fill.cx, fill.cy, fill.radius.coerceAtLeast(0.01f), fill.colors, fill.stops, Shader.TileMode.CLAMP,
                )
                paint.color = Color.BLACK
                paint.alpha = (255 * a).toInt()
            }
        }
    }

    private fun Path.toAndroid(): AndroidPath {
        val p = AndroidPath()
        p.fillType = if (evenOdd) AndroidPath.FillType.EVEN_ODD else AndroidPath.FillType.WINDING
        for (op in ops) when (op) {
            is Path.MoveTo -> p.moveTo(op.x, op.y)
            is Path.LineTo -> p.lineTo(op.x, op.y)
            is Path.QuadTo -> p.quadTo(op.x1, op.y1, op.x2, op.y2)
            is Path.CubicTo -> p.cubicTo(op.x1, op.y1, op.x2, op.y2, op.x3, op.y3)
            Path.Close -> p.close()
        }
        return p
    }
}

/** Bundled OFL Arabic fonts, loaded once per process. */
object Fonts {
    @Volatile private var cache: Map<FontId, Typeface>? = null

    fun get(context: Context): Map<FontId, Typeface> = cache ?: synchronized(this) {
        cache ?: load(context.applicationContext).also { cache = it }
    }

    private fun load(context: Context): Map<FontId, Typeface> {
        val assets = context.assets
        fun asset(name: String) = Typeface.createFromAsset(assets, "fonts/$name")
        val kufi = Typeface.Builder(assets, "fonts/ReemKufi.ttf").setFontVariationSettings("'wght' 700").build()
            ?: asset("ReemKufi.ttf")
        return mapOf(
            FontId.NASKH to asset("Amiri-Regular.ttf"),
            FontId.NASKH_BOLD to asset("Amiri-Bold.ttf"),
            FontId.RUQAA to asset("ArefRuqaa-Bold.ttf"),
            FontId.KUFI to kufi,
            FontId.LATIN to Typeface.create("sans-serif", Typeface.NORMAL),
        )
    }
}
