package com.noor.wallpapers.art

import kotlin.math.roundToInt

object Colors {
    fun hex(s: String): Int {
        val v = s.removePrefix("#").toLong(16)
        return if (s.removePrefix("#").length == 6) (0xFF000000 or v).toInt() else v.toInt()
    }

    fun alpha(c: Int) = (c ushr 24) and 0xFF
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int) =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun withAlpha(c: Int, a: Float) = (c and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).roundToInt() shl 24)

    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return argb(ch(alpha(a), alpha(b)), ch(red(a), red(b)), ch(green(a), green(b)), ch(blue(a), blue(b)))
    }

    fun darken(c: Int, t: Float) = mix(c, withAlpha(0xFF000000.toInt(), alpha(c) / 255f), t)
    fun lighten(c: Int, t: Float) = mix(c, withAlpha(0xFFFFFFFF.toInt(), alpha(c) / 255f), t)

    /** Perceived luminance in 0..1. */
    fun luminance(c: Int) = (0.2126 * red(c) + 0.7152 * green(c) + 0.0722 * blue(c)) / 255.0

    /** Hue 0..360, saturation and value 0..1. */
    fun hsv(h: Double, s: Double, v: Double): Int {
        val hh = ((h % 360) + 360) % 360 / 60
        val c = v * s
        val x = c * (1 - kotlin.math.abs(hh % 2 - 1))
        val (r, g, b) = when (hh.toInt()) {
            0 -> Triple(c, x, 0.0)
            1 -> Triple(x, c, 0.0)
            2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c)
            4 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val m = v - c
        return argb(255, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    const val TRANSPARENT = 0
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
}
