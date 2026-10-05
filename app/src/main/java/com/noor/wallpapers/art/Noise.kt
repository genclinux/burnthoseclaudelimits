package com.noor.wallpapers.art

import kotlin.math.floor

/**
 * Smooth deterministic value noise. With [period] > 0 the noise wraps every
 * `period` lattice cells, so textures built from it tile without seams.
 */
class Noise(private val seed: Int, private val period: Int = 0) {
    private fun hash(x: Int, y: Int): Double {
        val px = if (period > 0) Math.floorMod(x, period) else x
        val py = if (period > 0) Math.floorMod(y, period) else y
        var h = px * 374761393 + py * 668265263 + seed * 1274126177
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7fffffff) / 2147483647.0
    }

    private fun fade(t: Double) = t * t * t * (t * (t * 6 - 15) + 10)

    /** Value noise in 0..1. */
    fun at(x: Double, y: Double): Double {
        val x0 = floor(x).toInt(); val y0 = floor(y).toInt()
        val fx = fade(x - x0); val fy = fade(y - y0)
        val a = hash(x0, y0); val b = hash(x0 + 1, y0)
        val c = hash(x0, y0 + 1); val d = hash(x0 + 1, y0 + 1)
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fy)
    }

    /** Fractal sum of [octaves] layers, in 0..1. With a period, each octave doubles it to stay tileable. */
    fun fbm(x: Double, y: Double, octaves: Int = 4): Double {
        var sum = 0.0; var amp = 0.5; var norm = 0.0; var f = 1.0
        for (o in 0 until octaves) {
            val n = if (period > 0) Noise(seed + o * 101, period * f.toInt()).at(x * f, y * f) else at(x * f + o * 17.3, y * f - o * 9.1)
            sum += amp * n; norm += amp; amp *= 0.5; f *= 2
        }
        return sum / norm
    }
}
