package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Everything a design needs to draw one frame. */
class RenderContext(
    val width: Int,
    val height: Int,
    val palette: Palette,
    val seed: Int,
) {
    val w = width.toDouble()
    val h = height.toDouble()

    /** One "design pixel": layouts are authored for the Find X9 Pro's 1272 px wide panel. */
    val u = w / REFERENCE_WIDTH

    fun random(salt: Int = 0) = Random(seed * 7919 + salt)

    companion object {
        /** OPPO Find X9 Pro: 6.78" LTPO OLED, 1272 x 2772. */
        const val REFERENCE_WIDTH = 1272
        const val REFERENCE_HEIGHT = 2772
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

    fun ring(cx: Double, cy: Double, outer: Double, inner: Double): Path =
        Path().circle(cx, cy, outer).circle(cx, cy, inner).also { it.evenOdd = true }

    /** Regular polygon outline points. */
    fun ngon(cx: Double, cy: Double, r: Double, n: Int, rotation: Double = 0.0) =
        (0 until n).map { Vec(cx, cy) + Vec(r * cos(rotation + 2 * PI * it / n), r * sin(rotation + 2 * PI * it / n)) }
}
