package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Ebced (abjad): the old way of reading Arabic letters as numbers, used by
 * Ottoman poets to hide dates in verses. Her names: حنيفة 153, بتول 438,
 * together 591. And الله, لاله (lâle, tulip) and هلال (hilâl, crescent) are
 * all 66, which is why Ottoman art pairs the tulip and the crescent with the Name.
 */
object Ebced {
    private val VALUES: Map<Char, Int> = buildMap {
        "اأإآٱء".forEach { put(it, 1) }
        put('ى', 10)
        put('ب', 2); put('پ', 2); put('ج', 3); put('چ', 3); put('د', 4)
        put('ه', 5); put('ة', 5); put('و', 6); put('ؤ', 6); put('ز', 7); put('ژ', 7)
        put('ح', 8); put('ط', 9); put('ي', 10); put('ئ', 10); put('ی', 10)
        put('ك', 20); put('ک', 20); put('گ', 20); put('ل', 30); put('م', 40); put('ن', 50)
        put('س', 60); put('ع', 70); put('ف', 80); put('ص', 90); put('ق', 100)
        put('ر', 200); put('ش', 300); put('ت', 400); put('ث', 500); put('خ', 600)
        put('ذ', 700); put('ض', 800); put('ظ', 900); put('غ', 1000)
    }

    /** Sum of the letters' values; vowel marks and spaces count nothing. */
    fun value(arabic: String): Int = arabic.sumOf { VALUES[it] ?: 0 }

    const val HANIFE = "حنيفة"
    const val BETUL = "بتول"
}

/** Soft rose petals, for her birthday. */
object Petals {
    fun petal(x: Double, y: Double, size: Double, angle: Double): Path {
        val s = size
        val p = Path().moveTo(0, -s)
            .cubicTo(0.62 * s, -0.62 * s, 0.58 * s, 0.48 * s, 0, s)
            .cubicTo(-0.58 * s, 0.48 * s, -0.62 * s, -0.62 * s, 0, -s)
            .close()
        val c = kotlin.math.cos(angle); val sn = kotlin.math.sin(angle)
        return p.transformed { px, py -> (x + px * c - py * sn).toFloat() to (y + px * sn + py * c).toFloat() }
    }

    fun color(pal: Palette) = Colors.mix(pal.accentC, Colors.hex("#EBA3AE"), 0.55f)

    /** Draws one petal with a lighter heart, the way a real petal catches light. */
    fun draw(b: SceneBuilder, pal: Palette, x: Double, y: Double, size: Double, angle: Double, alpha: Float) {
        val col = color(pal)
        b.fill(petal(x, y, size, angle), col, 0.85f * alpha)
        b.fill(petal(x + size * 0.08, y - size * 0.1, size * 0.55, angle), Colors.lighten(col, 0.35f), 0.5f * alpha)
    }

    /** Petals scattered over the canvas, kept out of a central band [clearTop, clearBottom]. */
    fun scatter(b: SceneBuilder, ctx: RenderContext, count: Int, clearTop: Double, clearBottom: Double, salt: Int = 71) {
        val rnd = ctx.random(salt)
        var placed = 0
        var tries = 0
        while (placed < count && tries < count * 20) {
            tries++
            val x = rnd.nextDouble() * ctx.w
            val y = rnd.nextDouble() * ctx.h
            if (y in clearTop..clearBottom && x in ctx.x(0.1)..ctx.x(0.9)) continue
            draw(b, ctx.palette, x, y, (16 + rnd.nextDouble() * 22) * ctx.u, rnd.nextDouble() * 2 * PI, 0.6f + rnd.nextFloat() * 0.4f)
            placed++
        }
    }
}

/**
 * Lâle · Hilâl · Allah. An İznik-style tulip under a crescent, inside a gilded
 * arch, with الله above: three words written with the same letters, each worth
 * 66 in ebced. Hidden until she counts her own name's number on the tesbih.
 */
object TulipArt {
    fun scene(ctx: RenderContext): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        Common.background(b, ctx)
        val backdrop = GeometricArt.Params(Tiling.OCTAGON_SQUARE, PatternStyle.LINEWORK, edge = 84.0)
        b.group(alpha = 0.2f) { GeometricArt.drawPattern(this, ctx, backdrop, GeometricArt.tiles(ctx, backdrop)) }
        Common.vignette(b, ctx, 0.5f)

        // A tablet's square safe area is short: tighter spacing and slightly smaller pieces.
        val square = ctx.safeH < ctx.safeW * 1.5
        val k = if (square) 0.74 else 1.0
        fun y(phone: Double, tablet: Double) = ctx.y(if (square) tablet else phone)
        val cx = ctx.cx

        // The arch panel.
        val archBottom = y(0.84, 0.845)
        val archH = archBottom - y(0.155, 0.075)
        val archW = minOf(ctx.safeW * 0.80, archH * 0.62)
        val arch = Shapes.pointedArch(cx, archBottom, archW, archH)
        Common.glow(b, cx, (archBottom - archH / 2), archW * 0.9, pal.glow, 0.14f)
        b.fill(
            arch,
            LinearFill(
                0f, (archBottom - archH).toFloat(), 0f, archBottom.toFloat(),
                intArrayOf(Colors.darken(pal.bgBottom, 0.25f), Colors.mix(pal.bgBottom, pal.bgTop, 0.4f), Colors.darken(pal.bgBottom, 0.35f)),
            ),
            0.92f,
        )
        b.stroke(arch, pal.line, (7 * u).toFloat())
        b.stroke(Shapes.pointedArch(cx, archBottom - 18 * u, archW - 36 * u, archH - 30 * u), pal.line, (2 * u).toFloat(), 0.7f)
        // Small stars along the arch's inner rim.
        for (i in 0 until 9) {
            val f = (i + 0.5) / 9
            val yy = archBottom - archH * (0.12 + 0.5 * f)
            for (side in listOf(-1.0, 1.0)) {
                b.fill(Common.rubElHizb(cx + side * (archW / 2 - 40 * u), yy, 9 * u), pal.line, 0.55f)
            }
        }

        // الله at the top of the arch.
        val nameY = y(0.26, 0.205)
        b.text(
            TextItem(
                "ٱللَّٰه", FontId.NASKH_BOLD, (300 * u * k).toFloat(), cx.toFloat(), nameY.toFloat(),
                LinearFill(0f, (nameY - 130 * u * k).toFloat(), 0f, (nameY + 130 * u * k).toFloat(), intArrayOf(Colors.lighten(pal.line, 0.35f), pal.line)),
                maxWidth = (archW * 0.42).toFloat(), maxHeight = (250 * u * k).toFloat(), inkCentered = true,
                glowColor = Colors.withAlpha(pal.glow, 0.5f), glowRadius = (16 * u).toFloat(),
            ),
        )

        // The crescent, horns up, cradling the space above the bloom.
        val moonY = y(0.375, 0.34)
        Common.glow(b, cx, moonY, 190 * u * k, pal.glow, 0.25f)
        b.fill(Common.crescent(cx, moonY, 92 * u * k, rotation = -PI / 2, thickness = 0.42), Colors.lighten(pal.glow, 0.2f))

        tulip(b, ctx, cx, y(0.56, 0.595), y(0.81, 0.82), 330 * u * k)

        // The three words, and why they belong together.
        val arabicY = y(0.875, 0.872)
        b.text(
            TextItem(
                "لاله  ·  هلال  ·  الله", FontId.NASKH_BOLD, (78 * u).toFloat(), cx.toFloat(), arabicY.toFloat(),
                SolidFill(pal.line), maxWidth = (ctx.safeW * 0.8).toFloat(), maxHeight = (90 * u).toFloat(), inkCentered = true,
            ),
        )
        if (ctx.options.captions) {
            b.text(
                TextItem(
                    "Lâle · Hilâl · Allah", FontId.LATIN, (36 * u).toFloat(), cx.toFloat(), y(0.912, 0.91).toFloat(),
                    SolidFill(pal.line), maxWidth = (ctx.safeW * 0.84).toFloat(), alpha = 0.95f, letterSpacing = 0.06f,
                ),
            )
            b.text(
                TextItem(
                    "Üçü de aynı harflerle yazılır; ebced değerleri 66", FontId.LATIN, (25 * u).toFloat(), cx.toFloat(), y(0.938, 0.937).toFloat(),
                    SolidFill(Colors.lighten(pal.line, 0.5f)), maxWidth = (ctx.safeW * 0.84).toFloat(), alpha = 0.7f,
                ),
            )
        }
        Textures.grainOverlay(b, ctx, 0.4f)
        Common.topShade(b, ctx)
        return b.build()
    }

    /** An Ottoman tulip: almond petals ending in needle points, a curved stem and two saz leaves. */
    fun tulip(b: SceneBuilder, ctx: RenderContext, cx: Double, bloomBase: Double, ground: Double, h: Double) {
        val pal = ctx.palette
        val u = ctx.u
        val w = h * 0.62
        val leaf = Colors.mix(pal.accentB, Colors.hex("#4F7F5A"), 0.55f)
        val outline = Colors.darken(pal.line, 0.15f)

        // Stem with a gentle S-curve.
        val stem = Path().moveTo(cx, bloomBase).cubicTo(cx + 18 * u, bloomBase + (ground - bloomBase) * 0.35, cx - 22 * u, bloomBase + (ground - bloomBase) * 0.7, cx, ground)
        b.stroke(stem, leaf, (11 * u).toFloat())
        b.stroke(stem, Colors.lighten(leaf, 0.3f), (3 * u).toFloat(), 0.6f)

        // Saz leaves: long, curved, pointed; one each side.
        for (side in listOf(-1.0, 1.0)) {
            val baseY = ground - 30 * u
            val tipX = cx + side * w * 1.05
            val tipY = bloomBase + (ground - bloomBase) * 0.12
            val l = Path().moveTo(cx, baseY)
                .cubicTo(cx + side * w * 0.35, baseY - 60 * u, cx + side * w * 1.35, tipY + 300 * u, tipX, tipY)
                .cubicTo(cx + side * w * 0.45, tipY + 110 * u, cx + side * w * 0.02, baseY - 230 * u, cx, baseY)
                .close()
            b.fill(l, leaf, 0.95f)
            b.stroke(l, outline, (2.5 * u).toFloat(), 0.8f)
            val vein = Path().moveTo(cx, baseY).cubicTo(cx + side * w * 0.3, baseY - 130 * u, cx + side * w * 0.95, tipY + 220 * u, tipX, tipY)
            b.stroke(vein, Colors.lighten(leaf, 0.35f), (1.6 * u).toFloat(), 0.7f)
        }

        val top = bloomBase - h
        val fill = LinearFill(
            0f, top.toFloat(), 0f, bloomBase.toFloat(),
            intArrayOf(Colors.lighten(pal.accentA, 0.3f), pal.accentA, Colors.darken(pal.accentA, 0.25f)),
        )
        // Side petals first, flaring outward to needle tips.
        for (side in listOf(-1.0, 1.0)) {
            val p = Path().moveTo(cx + side * 0.05 * w, bloomBase)
                .cubicTo(cx + side * 0.70 * w, bloomBase - 0.02 * h, cx + side * 0.80 * w, bloomBase - 0.55 * h, cx + side * 0.74 * w, top + 0.16 * h)
                .cubicTo(cx + side * 0.50 * w, bloomBase - 0.58 * h, cx + side * 0.26 * w, bloomBase - 0.42 * h, cx + side * 0.02 * w, bloomBase - 0.30 * h)
                .close()
            b.fill(p, fill)
            b.stroke(p, outline, (3 * u).toFloat())
        }
        // The tall middle petal.
        val mid = Path().moveTo(cx - 0.26 * w, bloomBase - 0.04 * h)
            .cubicTo(cx - 0.40 * w, bloomBase - 0.48 * h, cx - 0.12 * w, bloomBase - 0.84 * h, cx, top)
            .cubicTo(cx + 0.12 * w, bloomBase - 0.84 * h, cx + 0.40 * w, bloomBase - 0.48 * h, cx + 0.26 * w, bloomBase - 0.04 * h)
            .quadTo(cx, bloomBase + 0.07 * h, cx - 0.26 * w, bloomBase - 0.04 * h)
            .close()
        b.fill(mid, fill)
        b.stroke(mid, outline, (3 * u).toFloat())
        // Veins and a gilded heart.
        for (k in listOf(-0.14, 0.0, 0.14)) {
            val v = Path().moveTo(cx + k * w, bloomBase - 0.08 * h).quadTo(cx + k * w * 1.4, bloomBase - 0.5 * h, cx + k * w * 0.5, bloomBase - 0.8 * h)
            b.stroke(v, Colors.lighten(pal.accentC, 0.2f), (1.8 * u).toFloat(), 0.55f)
        }
        b.fill(Common.rubElHizb(cx, bloomBase - 0.28 * h, 14 * u), pal.line, 0.9f)
        // A small calyx where the bloom meets the stem.
        b.fill(Path().moveTo(cx - 26 * u, bloomBase - 4 * u).quadTo(cx, bloomBase + 30 * u, cx + 26 * u, bloomBase - 4 * u).close(), leaf)
    }
}

/**
 * Kendi Sözün: her own words, written on a levha. Up to five lines, set in
 * Amiri (the same Naskh used for the Arabic) so Turkish reads like calligraphy.
 */
object CustomTextArt {
    const val DEFAULT_TEXT = "Yolun nur olsun"

    fun scene(ctx: RenderContext): Scene {
        val text = ctx.options.text?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_TEXT
        val lines = wrap(text)
        val longest = lines.maxOf { it.length }
        val wide = lines.size <= 2
        return LevhaArt.panel(ctx, Ebru.Style.BATTAL, wide) { s ->
            val u = ctx.u
            val n = lines.size
            val lineH = s.innerH * 0.74 / n
            // Amiri's Latin letters average about half an em wide.
            val size = minOf(lineH * 0.72, s.innerW * 0.84 / (longest * 0.5), 150 * u)
            val blockTop = s.innerT + s.innerH / 2 - lineH * n / 2
            lines.forEachIndexed { i, line ->
                val y = blockTop + lineH * (i + 0.5)
                text(
                    TextItem(
                        line, FontId.NASKH_BOLD, size.toFloat(), ctx.cx.toFloat(), y.toFloat(),
                        LinearFill(
                            0f, (y - size / 2).toFloat(), 0f, (y + size / 2).toFloat(),
                            intArrayOf(Colors.lighten(s.ink, if (s.night) 0.25f else 0.08f), s.ink, Colors.darken(s.ink, 0.1f)),
                        ),
                        maxWidth = (s.innerW * 0.88).toFloat(),
                    ),
                )
            }
            // A small star above and below the words.
            fill(Common.rubElHizb(ctx.cx, blockTop - 6 * u, 13 * u), s.gold, 0.9f)
            fill(Common.rubElHizb(ctx.cx, blockTop + lineH * n + 6 * u, 13 * u), s.gold, 0.9f)
        }
    }

    /** Breaks [text] into balanced lines: her own line breaks are kept, long lines split by word. */
    fun wrap(text: String, maxLines: Int = 5): List<String> {
        val out = ArrayList<String>()
        for (para in text.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
            val words = para.split(Regex("\\s+"))
            // Aim for lines of about 20 characters, evenly filled, with a little slack so words aren't stranded.
            val lineCount = ceil(para.length / 20.0).coerceIn(1.0, maxLines.toDouble())
            val target = ceil(para.length / lineCount).toInt() + 4
            var line = StringBuilder()
            for (w in words) {
                if (line.isNotEmpty() && line.length + 1 + w.length > target) {
                    out += line.toString(); line = StringBuilder()
                }
                if (line.isNotEmpty()) line.append(' ')
                line.append(w)
            }
            if (line.isNotEmpty()) out += line.toString()
        }
        if (out.isEmpty()) out += DEFAULT_TEXT
        if (out.size > maxLines) {
            val head = out.take(maxLines - 1)
            return head + out.drop(maxLines - 1).joinToString(" ")
        }
        return out
    }
}

/** Her birthday: her name in the medallion, wishes underneath, and rose petals. */
object BirthdayArt {
    val PHRASE = Phrase(
        "dogum-gunu", Phrases.HANIFE_BETUL.arabic, Phrases.HANIFE_BETUL.bare,
        "İyi ki doğdun, Hanife Betül", "Nice nur dolu, huzurlu ve mutlu yıllara",
    )

    fun scene(ctx: RenderContext): Scene {
        val base = CalligraphyArt.scene(ctx, CalligraphyArt.Params(PHRASE, CalligraphyArt.Script.NASKH, Tiling.DODECAGON_HEXAGON_SQUARE))
        val b = SceneBuilder(ctx.width, ctx.height)
        val r = minOf(ctx.safeW * 0.40, ctx.safeH * 0.27)
        // Petals everywhere but over the medallion and the wishes below it.
        Petals.scatter(b, ctx, 46, ctx.cy - r * 1.2, ctx.cy + r + 330 * ctx.u)
        // A ring of tiny stars around the medallion.
        val rnd = Random(ctx.seed + 5)
        repeat(24) {
            val a = 2 * PI * it / 24
            val p = Vec(ctx.cx, ctx.cy) + Vec.polar(r * (1.32 + rnd.nextDouble() * 0.06), a)
            b.fill(Common.sparkle(p.x, p.y, (7 + rnd.nextDouble() * 6) * ctx.u), Colors.lighten(ctx.palette.glow, 0.5f), 0.85f)
        }
        return Scene(base.width, base.height, base.items + b.items)
    }
}
