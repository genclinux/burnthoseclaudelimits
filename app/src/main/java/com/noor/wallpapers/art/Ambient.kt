package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The moving layer of the live wallpaper, drawn over a cached still of the design:
 * twinkling stars and a slow band of light that drifts across the screen.
 * Kept to a handful of primitives so it costs little per frame.
 */
object Ambient {
    /** A touch on the live wallpaper at ([x], [y]), [time] seconds into the animation. */
    data class Tap(val x: Double, val y: Double, val time: Double)

    /**
     * The moving layer at [timeSeconds]. Each [taps] entry sends a shooting star
     * from where she touched; on her [birthday] rose petals drift down.
     */
    fun overlay(ctx: RenderContext, timeSeconds: Double, taps: List<Tap> = emptyList(), birthday: Boolean = false): List<Item> {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val rnd = ctx.random(97)

        // Light sweep: one pass every 24 s, then rests off-screen.
        val period = 24.0
        val phase = (timeSeconds % period) / period
        val x = lerp(-0.6, 1.6, phase) * ctx.w
        val band = ctx.w * 0.5
        b.fill(
            Common.fullRect(ctx),
            LinearFill(
                (x - band).toFloat(), 0f, (x + band).toFloat(), (ctx.h * 0.35).toFloat(),
                intArrayOf(0, Colors.withAlpha(pal.glow, 0.07f), 0),
            ),
        )

        // Twinkles.
        repeat(26) {
            val sx = rnd.nextDouble() * ctx.w
            val sy = ctx.h * 0.62 * rnd.nextDouble().pow(1.3)
            val speed = 0.4 + rnd.nextDouble() * 0.9
            val offset = rnd.nextDouble() * 2 * PI
            val a = sin(timeSeconds * speed + offset).coerceAtLeast(0.0).pow(3.0).toFloat()
            if (a > 0.02f) {
                val r = ctx.u * (8 + rnd.nextDouble() * 10)
                Common.glow(b, sx, sy, r * 2.5, pal.glow, 0.4f * a)
                b.fill(Common.sparkle(sx, sy, r * (0.6 + 0.4 * a)), Colors.lighten(pal.glow, 0.7f), a)
            }
        }
        shootingStar(b, ctx, timeSeconds)
        for (tap in taps) tapStar(b, ctx, tap, timeSeconds)
        if (birthday) petals(b, ctx, timeSeconds)
        return b.items
    }

    const val TAP_DURATION = 1.3

    /** Easter egg: touch the wallpaper and a star shoots from your fingertip. */
    private fun tapStar(b: SceneBuilder, ctx: RenderContext, tap: Tap, t: Double) {
        val local = t - tap.time
        if (local < 0 || local > TAP_DURATION) return
        val p = local / TAP_DURATION
        val rnd = Random((tap.time * 1000).toLong())
        val dir = Vec(0.78, -0.62).let { if (rnd.nextBoolean()) Vec(-it.x, it.y) else it }
        streak(b, ctx, Vec(tap.x, tap.y), dir, p, ctx.safeW * 0.45)
    }

    /** Her birthday: petals falling slowly, turning as they go. */
    private fun petals(b: SceneBuilder, ctx: RenderContext, t: Double) {
        val rnd = ctx.random(211)
        val span = ctx.h + 120 * ctx.u
        repeat(16) {
            val x0 = rnd.nextDouble() * ctx.w
            val fall = span / (22 + rnd.nextDouble() * 16)
            val offset = rnd.nextDouble() * span
            val sway = (30 + rnd.nextDouble() * 50) * ctx.u
            val phase = rnd.nextDouble() * 2 * PI
            val spin = 0.3 + rnd.nextDouble() * 0.6
            val size = (14 + rnd.nextDouble() * 14) * ctx.u
            val y = (t * fall + offset) % span - 60 * ctx.u
            val x = x0 + sin(t * 0.6 + phase) * sway
            Petals.draw(b, ctx.palette, x, y, size, t * spin + phase, 0.8f)
        }
    }

    /** Easter egg for Hanife Betül: a shooting star every 37 seconds. Make a wish. */
    private fun shootingStar(b: SceneBuilder, ctx: RenderContext, t: Double) {
        val period = 37.0
        val duration = 1.4
        val cycle = (t / period).toInt()
        val local = t - cycle * period
        if (local > duration) return
        val p = local / duration
        val rnd = Random(ctx.seed * 31 + cycle)
        val start = Vec(ctx.x(0.15 + rnd.nextDouble() * 0.6), ctx.y(0.04 + rnd.nextDouble() * 0.2))
        val dir = Vec(0.82, 0.57).let { if (rnd.nextBoolean()) Vec(-it.x, it.y) else it }
        streak(b, ctx, start, dir, p, ctx.safeW * 0.55)
    }

    /** A shooting star [p] (0..1) of the way along its path from [start] in direction [dir]. */
    private fun streak(b: SceneBuilder, ctx: RenderContext, start: Vec, dir: Vec, p: Double, travel: Double) {
        val head = start + dir * (travel * p)
        val tail = head - dir * (travel * 0.35 * (1 - p * 0.5))
        val fade = sin(PI * p).toFloat()
        val glow = Colors.lighten(ctx.palette.glow, 0.6f)
        b.items += StrokeItem(
            Path().moveTo(tail.x, tail.y).lineTo(head.x, head.y),
            LinearFill(tail.x.toFloat(), tail.y.toFloat(), head.x.toFloat(), head.y.toFloat(), intArrayOf(0, glow)),
            (3.5 * ctx.u).toFloat(), fade,
        )
        Common.glow(b, head.x, head.y, 18 * ctx.u, ctx.palette.glow, 0.7f * fade)
    }
}
