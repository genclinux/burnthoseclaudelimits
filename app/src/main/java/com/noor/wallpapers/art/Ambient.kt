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
    fun overlay(ctx: RenderContext, timeSeconds: Double): List<Item> {
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
        return b.items
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
        val travel = ctx.safeW * 0.55
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
