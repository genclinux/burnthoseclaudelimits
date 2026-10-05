package com.noor.wallpapers.preview

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
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.MultipleGradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.TexturePaint
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File

/** Desktop mirror of the app's AndroidRenderer, for previews only. */
class Java2DRenderer(fontDir: File) {
    private val fonts: Map<FontId, Font> = mapOf(
        FontId.NASKH to "Amiri-Regular.ttf",
        FontId.NASKH_BOLD to "Amiri-Bold.ttf",
        FontId.RUQAA to "ArefRuqaa-Bold.ttf",
        FontId.KUFI to "ReemKufi.ttf",
    ).mapValues { Font.createFont(Font.TRUETYPE_FONT, File(fontDir, it.value)) } +
        (FontId.LATIN to Font(Font.SANS_SERIF, Font.PLAIN, 12))

    fun render(scene: Scene): BufferedImage {
        val img = BufferedImage(scene.width, scene.height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = Color.BLACK
        g.fillRect(0, 0, scene.width, scene.height)
        scene.items.forEach { draw(g, it, 1f) }
        g.dispose()
        return img
    }

    private fun draw(g: Graphics2D, item: Item, alpha: Float) {
        when (item) {
            is FillItem -> {
                g.composite = composite(item.blend, item.alpha * alpha)
                g.paint = paint(item.fill)
                g.fill(shape(item.path))
            }
            is StrokeItem -> {
                g.composite = composite(item.blend, item.alpha * alpha)
                g.paint = paint(item.fill)
                g.stroke = if (item.round) {
                    BasicStroke(item.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                } else {
                    BasicStroke(item.width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
                }
                g.draw(shape(item.path))
            }
            is TextItem -> drawText(g, item, alpha)
            is RasterItem -> {
                val img = BufferedImage(item.width, item.height, BufferedImage.TYPE_INT_ARGB)
                img.setRGB(0, 0, item.width, item.height, item.pixels, 0, item.width)
                g.composite = composite(item.blend, item.alpha * alpha)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                val r = java.awt.geom.Rectangle2D.Float(item.left, item.top, item.right - item.left, item.bottom - item.top)
                if (item.tiled) {
                    g.paint = TexturePaint(img, java.awt.geom.Rectangle2D.Float(0f, 0f, item.width * item.tileScale, item.height * item.tileScale))
                    g.fill(r)
                } else {
                    g.drawImage(img, item.left.toInt(), item.top.toInt(), (item.right - item.left).toInt(), (item.bottom - item.top).toInt(), null)
                }
            }
            is GroupItem -> {
                val oldClip = g.clip
                item.clip?.let { g.clip(shape(it)) }
                item.items.forEach { draw(g, it, alpha * item.alpha) }
                g.clip = oldClip
            }
        }
    }

    private fun drawText(g: Graphics2D, t: TextItem, alpha: Float) {
        var font = fonts.getValue(t.font).deriveFont(t.size)
        if (t.letterSpacing != 0f) font = font.deriveFont(mapOf(TextAttribute.TRACKING to t.letterSpacing))
        var fm = g.getFontMetrics(font)
        val width = fm.stringWidth(t.text)
        if (width > t.maxWidth) {
            font = font.deriveFont(t.size * t.maxWidth / width)
            fm = g.getFontMetrics(font)
        }
        var x = t.cx - fm.stringWidth(t.text) / 2f
        var y = t.cy + (fm.ascent - fm.descent) / 2f
        if (t.inkCentered) {
            // Same rule as the app: fit and centre the pixels actually drawn.
            font = fonts.getValue(t.font).deriveFont(t.size)
            if (t.letterSpacing != 0f) font = font.deriveFont(mapOf(TextAttribute.TRACKING to t.letterSpacing))
            val ink = inkBounds(t.text, font)
            val k = minOf(1f, t.maxWidth / ink[2], t.maxHeight / ink[3])
            font = font.deriveFont(font.size2D * k)
            fm = g.getFontMetrics(font)
            x = t.cx - (ink[0] + ink[2] / 2) * k
            y = t.cy - (ink[1] + ink[3] / 2) * k
        }
        g.font = font
        if (t.glowRadius > 0f && t.glowColor != 0) {
            // Java2D has no blur; fake the glow with faint offset copies.
            g.color = color(Colors.withAlpha(t.glowColor, 0.08f))
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * t.alpha)
            val r = t.glowRadius
            for (dx in listOf(-r, -r / 2, 0f, r / 2, r)) for (dy in listOf(-r, -r / 2, 0f, r / 2, r)) {
                g.drawString(t.text, x + dx, y + dy)
            }
        }
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (alpha * t.alpha).coerceIn(0f, 1f))
        g.paint = paint(t.fill)
        g.drawString(t.text, x, y)
    }

    /** Ink box [left, top, width, height] relative to (text origin, baseline), from pixels. */
    private fun inkBounds(text: String, font: Font): FloatArray {
        val size = font.size2D
        val probe = BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY).createGraphics()
        val advance = probe.getFontMetrics(font).stringWidth(text)
        probe.dispose()
        val w = (advance * 1.5f + size * 3).toInt()
        val h = (size * 5).toInt()
        val ox = size * 1.5f
        val oy = size * 3f
        val img = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.font = font
        g.drawString(text, ox, oy)
        g.dispose()
        val raster = img.raster
        var minX = Int.MAX_VALUE; var maxX = -1; var minY = Int.MAX_VALUE; var maxY = -1
        for (yy in 0 until h) for (xx in 0 until w) {
            if (raster.getSample(xx, yy, 0) > 8) {
                if (xx < minX) minX = xx
                if (xx > maxX) maxX = xx
                if (yy < minY) minY = yy
                if (yy > maxY) maxY = yy
            }
        }
        if (maxX < 0) return floatArrayOf(0f, -size, advance.toFloat(), size)
        return floatArrayOf(minX - ox, minY - oy, (maxX + 1 - minX).toFloat(), (maxY + 1 - minY).toFloat())
    }

    private fun composite(blend: Blend, a: Float): java.awt.Composite =
        if (blend == Blend.NORMAL) AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a.coerceIn(0f, 1f))
        else BlendComposite(blend, a.coerceIn(0f, 1f))

    private fun color(c: Int) = Color(Colors.red(c), Colors.green(c), Colors.blue(c), Colors.alpha(c))

    private fun paint(f: Fill): java.awt.Paint = when (f) {
        is SolidFill -> color(f.color)
        is LinearFill -> {
            val stops = fixStops(f.stops ?: evenStops(f.colors.size))
            if (f.x0 == f.x1 && f.y0 == f.y1) color(f.colors.last())
            else LinearGradientPaint(
                Point2D.Float(f.x0, f.y0), Point2D.Float(f.x1, f.y1), stops,
                f.colors.map(::color).toTypedArray(), MultipleGradientPaint.CycleMethod.NO_CYCLE,
            )
        }
        is RadialFill -> RadialGradientPaint(
            Point2D.Float(f.cx, f.cy), f.radius.coerceAtLeast(0.01f),
            fixStops(f.stops ?: evenStops(f.colors.size)), f.colors.map(::color).toTypedArray(),
        )
    }

    private fun evenStops(n: Int) = FloatArray(n) { it / (n - 1).toFloat() }

    /** Java2D requires strictly increasing stops. */
    private fun fixStops(s: FloatArray): FloatArray {
        val out = s.copyOf()
        for (i in 1 until out.size) if (out[i] <= out[i - 1]) out[i] = out[i - 1] + 1e-4f
        return out
    }

    private fun shape(p: Path): Path2D {
        val gp = GeneralPath(if (p.evenOdd) Path2D.WIND_EVEN_ODD else Path2D.WIND_NON_ZERO)
        for (op in p.ops) when (op) {
            is Path.MoveTo -> gp.moveTo(op.x, op.y)
            is Path.LineTo -> gp.lineTo(op.x, op.y)
            is Path.QuadTo -> gp.quadTo(op.x1, op.y1, op.x2, op.y2)
            is Path.CubicTo -> gp.curveTo(op.x1, op.y1, op.x2, op.y2, op.x3, op.y3)
            Path.Close -> gp.closePath()
        }
        return gp
    }
}
