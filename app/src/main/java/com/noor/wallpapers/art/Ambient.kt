package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

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
        return b.items
    }
}
