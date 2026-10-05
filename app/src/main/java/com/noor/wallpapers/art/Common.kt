package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Everything a design needs to draw one frame.
 *
 * Backgrounds fill the whole canvas, but focal content (medallions, mosques,
 * lanterns, arches) is laid out inside the *safe area*. On a phone the safe
 * area is the whole screen. On a tablet it is the centred square that stays
 * visible whether the tablet is held upright or sideways, so one wallpaper
 * works in both orientations.
 */
class RenderContext(
    val width: Int,
    val height: Int,
    val palette: Palette,
    val seed: Int,
    safeLeft: Double = 0.0,
    safeTop: Double = 0.0,
    safeWidth: Double = width.toDouble(),
    safeHeight: Double = height.toDouble(),
    /** Her own choices on top of palette and seed: captions, texture, dimming, her own words. */
    val options: DesignOptions = DesignOptions.DEFAULT,
) {
    val w = width.toDouble()
    val h = height.toDouble()

    val safeLeft = safeLeft
    val safeTop = safeTop
    val safeW = safeWidth
    val safeH = safeHeight
    val cx = safeLeft + safeWidth / 2
    val cy = safeTop + safeHeight / 2

    /** A point [f] of the way across / down the safe area. */
    fun x(f: Double) = safeLeft + f * safeW
    fun y(f: Double) = safeTop + f * safeH

    /**
     * One "design pixel". Layouts are authored for the Find X9 Pro's 1272 px
     * wide panel; in a squarer safe area the height limit takes over, so tall
     * things like minarets still fit.
     */
    val u = minOf(safeW / REFERENCE_WIDTH, safeH / 2000.0)

    fun random(salt: Int = 0) = Random(seed * 7919 + salt)

    companion object {
        /** OPPO Find X9 Pro: 6.78" LTPO OLED, 1272 x 2772. */
        const val REFERENCE_WIDTH = 1272
        const val REFERENCE_HEIGHT = 2772

        /**
         * A tablet canvas of [width] x [height] whose focal content sits in a
         * centred square of side [safeSize] (the panel's short side).
         */
        fun tablet(
            width: Int, height: Int, safeSize: Double, palette: Palette, seed: Int,
            options: DesignOptions = DesignOptions.DEFAULT,
        ) = RenderContext(
            width, height, palette, seed,
            (width - safeSize) / 2, (height - safeSize) / 2, safeSize, safeSize, options,
        )
    }
}

/**
 * Choices that apply to any design. [texture] scales the paper grain (0 = smooth
 * vector), [dim] darkens the whole wallpaper (for legibility or a dark OLED
 * screen), [captions] shows the Turkish reading and meaning under calligraphy,
 * and [text] is her own words for the "Kendi Sözün" design.
 */
data class DesignOptions(
    val captions: Boolean = true,
    val texture: Float = 1f,
    val dim: Float = 0f,
    val text: String? = null,
) {
    val isDefault get() = this == DEFAULT

    /** Compact and free of '|' so it fits inside a Selection: "c0;t50;d20;x<url-encoded text>". */
    fun encode(): String = buildList {
        if (!captions) add("c0")
        if (texture != 1f) add("t${(texture * 100).roundToInt()}")
        if (dim != 0f) add("d${(dim * 100).roundToInt()}")
        text?.let { add("x" + java.net.URLEncoder.encode(it, "UTF-8")) }
    }.joinToString(";")

    companion object {
        val DEFAULT = DesignOptions()
        const val MAX_DIM = 0.6f
        const val MAX_TEXT = 120

        fun decode(s: String?): DesignOptions {
            if (s.isNullOrBlank()) return DEFAULT
            var o = DEFAULT
            for (part in s.split(';')) {
                if (part.isEmpty()) continue
                val v = part.substring(1)
                o = when (part[0]) {
                    'c' -> o.copy(captions = v != "0")
                    't' -> v.toIntOrNull()?.let { o.copy(texture = (it / 100f).coerceIn(0f, 1f)) } ?: o
                    'd' -> v.toIntOrNull()?.let { o.copy(dim = (it / 100f).coerceIn(0f, MAX_DIM)) } ?: o
                    'x' -> runCatching { java.net.URLDecoder.decode(v, "UTF-8") }.getOrNull()
                        ?.take(MAX_TEXT)?.let { o.copy(text = it) } ?: o
                    else -> o
                }
            }
            return o
        }
    }
}

object Common {
    fun fullRect(ctx: RenderContext) = Path().rect(0, 0, ctx.w, ctx.h)

    fun verticalGradient(ctx: RenderContext, vararg colors: Int, stops: FloatArray? = null) =
        LinearFill(0f, 0f, 0f, ctx.h.toFloat(), colors, stops)

    fun background(b: SceneBuilder, ctx: RenderContext) {
        val p = ctx.palette
        b.fill(fullRect(ctx), verticalGradient(ctx, p.bgTop, Colors.mix(p.bgTop, p.bgBottom, 0.55f), p.bgBottom))
    }

    /** Darkens the corners so the eye settles in the middle and icons stay legible. */
    fun vignette(b: SceneBuilder, ctx: RenderContext, strength: Float = 0.55f) {
        val r = (sqrt(ctx.w * ctx.w + ctx.h * ctx.h) / 2).toFloat()
        b.fill(
            fullRect(ctx),
            RadialFill(
                ctx.w.toFloat() / 2, ctx.h.toFloat() * 0.48f, r,
                intArrayOf(0, 0, Colors.withAlpha(Colors.BLACK, strength)),
                floatArrayOf(0f, 0.55f, 1f),
            ),
        )
    }

    /** Soft shade behind the status bar and lock-screen clock. */
    fun topShade(b: SceneBuilder, ctx: RenderContext, strength: Float = 0.35f) {
        b.fill(
            Path().rect(0, 0, ctx.w, ctx.h * 0.16),
            LinearFill(
                0f, 0f, 0f, (ctx.h * 0.16).toFloat(),
                intArrayOf(Colors.withAlpha(Colors.BLACK, strength), 0),
            ),
        )
    }

    fun glow(b: SceneBuilder, cx: Double, cy: Double, r: Double, color: Int, alpha: Float) {
        b.fill(
            Path().circle(cx, cy, r),
            RadialFill(
                cx.toFloat(), cy.toFloat(), r.toFloat(),
                intArrayOf(Colors.withAlpha(color, alpha), Colors.withAlpha(color, alpha * 0.35f), 0),
                floatArrayOf(0f, 0.35f, 1f),
            ),
        )
    }

    /**
     * A crescent: the part of a circle of radius [r] at ([cx], [cy]) not covered by
     * a smaller circle offset to one side. [rotation] turns the opening (0 = opening right).
     */
    fun crescent(cx: Double, cy: Double, r: Double, rotation: Double = 0.0, thickness: Double = 0.38): Path {
        val r2 = r * 0.84
        val d = r * thickness
        val ix = (d * d + r * r - r2 * r2) / (2 * d)
        val iy = sqrt((r * r - ix * ix).coerceAtLeast(0.0))
        val outerStart = atan2(iy, ix)
        val innerAngle = atan2(iy, ix - d)
        val pts = ArrayList<Vec>()
        val steps = 48
        for (i in 0..steps) {
            val a = lerp(outerStart, 2 * PI - outerStart, i / steps.toDouble())
            pts += Vec.polar(r, a)
        }
        for (i in 0..steps) {
            val a = lerp(2 * PI - innerAngle, innerAngle, i / steps.toDouble())
            pts += Vec(d, 0.0) + Vec.polar(r2, a)
        }
        val c = Vec(cx, cy)
        return Path().polygon(pts.map { it.rotated(rotation) + c })
    }

    /** An n-pointed star polygon. */
    fun star(cx: Double, cy: Double, outer: Double, inner: Double, points: Int, rotation: Double = -PI / 2): Path {
        val pts = (0 until points * 2).map { i ->
            val r = if (i % 2 == 0) outer else inner
            Vec(cx, cy) + Vec.polar(r, rotation + PI * i / points)
        }
        return Path().polygon(pts)
    }

    /** Four-point sparkle used for bright stars. */
    fun sparkle(cx: Double, cy: Double, r: Double): Path = star(cx, cy, r, r * 0.18, 4)

    /** A starfield in the band [top, bottom), denser towards the top. */
    fun starfield(b: SceneBuilder, ctx: RenderContext, top: Double, bottom: Double, count: Int, salt: Int = 11) {
        val rnd = ctx.random(salt)
        val small = Path()
        val medium = Path()
        repeat(count) {
            val x = rnd.nextDouble() * ctx.w
            val y = top + (bottom - top) * rnd.nextDouble().pow(1.6)
            val s = rnd.nextDouble()
            if (s < 0.8) small.circle(x, y, ctx.u * (1.2 + rnd.nextDouble() * 1.4))
            else medium.circle(x, y, ctx.u * (2.4 + rnd.nextDouble() * 1.6))
        }
        b.fill(small, Colors.lighten(ctx.palette.glow, 0.5f), 0.55f)
        b.fill(medium, Colors.lighten(ctx.palette.glow, 0.6f), 0.85f)
        repeat(count / 30 + 2) {
            val x = rnd.nextDouble() * ctx.w
            val y = top + (bottom - top) * rnd.nextDouble().pow(1.8)
            val r = ctx.u * (10 + rnd.nextDouble() * 12)
            glow(b, x, y, r * 2.2, ctx.palette.glow, 0.35f)
            b.fill(sparkle(x, y, r), Colors.lighten(ctx.palette.glow, 0.7f), 0.95f)
        }
    }

    /** Eight-pointed Rub el Hizb: two overlapping squares. */
    fun rubElHizb(cx: Double, cy: Double, r: Double): Path {
        val p = Path()
        for (rot in listOf(0.0, PI / 4)) {
            p.polygon((0 until 4).map { Vec(cx, cy) + Vec.polar(r, rot + PI / 4 + it * PI / 2) })
        }
        return p
    }

    /**
     * Easter egg: a small constellation spelling "HB" for Hanife Betül, hidden
     * among the stars of every night sky. [size] is the letter height.
     */
    fun initialsConstellation(b: SceneBuilder, ctx: RenderContext, left: Double, top: Double, size: Double, alpha: Float = 1f) {
        fun p(x: Double, y: Double) = Vec(left + x * size, top + y * size)
        val strokes = listOf(
            // H
            listOf(p(0.0, 0.0), p(0.0, 1.0)),
            listOf(p(0.0, 0.5), p(0.5, 0.5)),
            listOf(p(0.5, 0.0), p(0.5, 1.0)),
            // B
            listOf(p(0.85, 0.0), p(0.85, 1.0)),
            listOf(p(0.85, 0.0), p(1.2, 0.08), p(1.25, 0.3), p(0.85, 0.5)),
            listOf(p(0.85, 0.5), p(1.3, 0.6), p(1.3, 0.88), p(0.85, 1.0)),
        )
        val lines = Path()
        for (s in strokes) lines.polygon(s, closed = false)
        b.stroke(lines, ctx.palette.glow, (1.6 * ctx.u).toFloat(), 0.28f * alpha)
        val dots = strokes.flatten().distinct()
        for (d in dots) {
            glow(b, d.x, d.y, 16 * ctx.u, ctx.palette.glow, 0.3f * alpha)
            b.fill(Path().circle(d.x, d.y, 3.4 * ctx.u), Colors.lighten(ctx.palette.glow, 0.6f), 0.95f * alpha)
        }
    }

    /** Easter egg: a quiet "H·B" monogram in a tiny eight-pointed star, on every wallpaper. */
    fun signature(b: SceneBuilder, ctx: RenderContext) {
        val x = ctx.cx
        val y = ctx.y(0.968)
        val r = 15 * ctx.u
        val col = ctx.palette.line
        b.fill(rubElHizb(x, y, r), col, 0.38f)
        b.fill(Path().circle(x, y, r * 0.55), Colors.darken(ctx.palette.bgBottom, 0.2f), 0.55f)
        b.text(
            TextItem(
                "H·B", FontId.LATIN, (9 * ctx.u).toFloat(), x.toFloat(), y.toFloat(),
                SolidFill(col), alpha = 0.6f,
            ),
        )
    }

    fun ring(cx: Double, cy: Double, outer: Double, inner: Double): Path =
        Path().circle(cx, cy, outer).circle(cx, cy, inner).also { it.evenOdd = true }

    /** Regular polygon outline points. */
    fun ngon(cx: Double, cy: Double, r: Double, n: Int, rotation: Double = 0.0) =
        (0 until n).map { Vec(cx, cy) + Vec(r * cos(rotation + 2 * PI * it / n), r * sin(rotation + 2 * PI * it / n)) }
}
