package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Ebru, Turkish paper marbling, simulated the way a marbler works the tray.
 *
 * Paint is dropped on size water and spreads, pushing everything already there
 * outwards without changing its area. Then a stylus (biz) or comb (tarak) is
 * drawn through it. Each move is a mapping of the surface; following
 * "Mathematical Marbling" (Jaffer, Lu et al.), every one has an exact inverse,
 * so each pixel is traced backwards through the moves to the drop of paint
 * that ended up there. Nothing is drawn by hand-placed shapes, which is why
 * the result has the irregular, organic edges of the real craft.
 */
object Ebru {
    sealed interface Op

    /** A drop of paint [color] at (x, y) with radius r. */
    class Drop(val x: Double, val y: Double, val r: Double, val color: Int) : Op

    /** A stylus line through (x, y) in direction (dx, dy): paint shifts by [z] along it, falling off over [falloff]. */
    class Tine(val x: Double, val y: Double, val dx: Double, val dy: Double, val z: Double, val falloff: Double) : Op

    /** A wavy comb pass: across direction (dx, dy), paint shifts sideways by amp*sin(freq*s + phase). */
    class Wave(val dx: Double, val dy: Double, val amp: Double, val freq: Double, val phase: Double) : Op

    /** A swirl about (x, y): rotation by [angle] at the centre, fading over [radius]. */
    class Swirl(val x: Double, val y: Double, val angle: Double, val radius: Double) : Op

    /**
     * Renders [ops] (applied in order) on a [w] x [h] pixel tray whose
     * coordinates span [scale] canvas pixels per tray pixel. [base] is the
     * colour of the size water where no paint landed.
     */
    fun render(w: Int, h: Int, scale: Double, ops: List<Op>, base: Int, seed: Int, ox: Double = 0.0, oy: Double = 0.0): IntArray {
        // Flatten the moves into parallel arrays: the inner loop runs (pixels x moves)
        // times, so it avoids object dispatch and runs rows on every core.
        val n = ops.size
        val type = IntArray(n)
        val a = DoubleArray(n); val bb = DoubleArray(n); val c = DoubleArray(n)
        val d = DoubleArray(n); val e = DoubleArray(n); val f = DoubleArray(n)
        val col = IntArray(n)
        for ((k, op) in ops.withIndex()) when (op) {
            is Drop -> { type[k] = 0; a[k] = op.x; bb[k] = op.y; c[k] = op.r * op.r; col[k] = op.color }
            is Tine -> { type[k] = 1; a[k] = op.x; bb[k] = op.y; c[k] = op.dx; d[k] = op.dy; e[k] = op.z; f[k] = op.falloff }
            is Wave -> { type[k] = 2; c[k] = op.dx; d[k] = op.dy; e[k] = op.amp; f[k] = op.freq; a[k] = op.phase }
            is Swirl -> { type[k] = 3; a[k] = op.x; bb[k] = op.y; e[k] = op.angle; f[k] = op.radius }
        }
        val out = IntArray(w * h)
        val grain = Noise(seed + 77)
        java.util.stream.IntStream.range(0, h).parallel().forEach { py ->
            for (px in 0 until w) {
                var x = ox + (px + 0.5) * scale
                var y = oy + (py + 0.5) * scale
                var color = base
                var shade = 1.0
                var i = n - 1
                while (i >= 0) {
                    when (type[i]) {
                        0 -> {
                            val ddx = x - a[i]; val ddy = y - bb[i]
                            val d2 = ddx * ddx + ddy * ddy
                            val r2 = c[i]
                            if (d2 < r2) {
                                color = col[i]
                                // Pigment pools slightly at the rim of each cell, as real paint does.
                                val rho2 = d2 / r2
                                shade = 1.0 - 0.10 * rho2 * rho2
                                break
                            }
                            val k = sqrt(1 - r2 / d2)
                            x = a[i] + ddx * k; y = bb[i] + ddy * k
                        }
                        1 -> {
                            val dist = abs((x - a[i]) * -d[i] + (y - bb[i]) * c[i])
                            val m = e[i] * f[i] / (dist + f[i])
                            x -= m * c[i]; y -= m * d[i]
                        }
                        2 -> {
                            val m = e[i] * sin(f[i] * (x * c[i] + y * d[i]) + a[i])
                            x += m * d[i]; y -= m * c[i]
                        }
                        else -> {
                            val ddx = x - a[i]; val ddy = y - bb[i]
                            val ang = -e[i] * exp(-hypot(ddx, ddy) / f[i])
                            val cs = cos(ang); val sn = sin(ang)
                            x = a[i] + ddx * cs - ddy * sn; y = bb[i] + ddx * sn + ddy * cs
                        }
                    }
                    i--
                }
                // Faint mottling: paint never lies perfectly flat.
                val g = 0.94 + 0.06 * grain.at((ox + px * scale) / 9.0, (oy + py * scale) / 9.0)
                out[py * w + px] = scaleColor(color, shade * g)
            }
        }
        return out
    }

    private fun scaleColor(c: Int, k: Double): Int = Colors.argb(
        255,
        (Colors.red(c) * k).roundToInt(), (Colors.green(c) * k).roundToInt(), (Colors.blue(c) * k).roundToInt(),
    )

    // ---------------------------------------------------------------- styles

    enum class Style(val label: String) {
        BATTAL("Battal"),
        GELGIT("Gelgit"),
        SAL("Şal"),
        TARAKLI("Taraklı"),
        BULBUL_YUVASI("Bülbül Yuvası"),
    }

    /**
     * Hand-mixed pigment sets, one per palette, in the spirit of traditional
     * ebru colours (indigo, ochre, madder red, green earth, ivory), and the
     * colour of the size water they float on.
     */
    fun pigments(p: Palette): Pair<List<Int>, Int> {
        fun c(vararg hex: String) = hex.map { Colors.hex(it) }
        return when (p.id) {
            "betul" -> c("#C97B84", "#E8B4A0", "#7E9B83", "#5A2F4A", "#F4E8E0") to Colors.hex("#F1E6DA")
            "emerald" -> c("#1F5E4A", "#C9A04A", "#8E2B33", "#2E3A6B", "#EDE3CC") to Colors.hex("#EFE7D3")
            "lapis" -> c("#1B2F5E", "#3E66A8", "#C9A04A", "#8FA9C9", "#ECE4CF") to Colors.hex("#E9E4D6")
            "iznik" -> c("#1F6F8B", "#2FA3A0", "#B83A2E", "#27427A", "#F1ECE0") to Colors.hex("#F3EEE2")
            "sand" -> c("#A0522D", "#C8913A", "#6B7B4B", "#5A3B28", "#EADBBF") to Colors.hex("#F0E4CC")
            "isfahan" -> c("#B0466E", "#2A7F86", "#E9B872", "#5A2F4A", "#EFDCD5") to Colors.hex("#F2E8E4")
            "alhambra" -> c("#B5462A", "#2A6F5F", "#E0B85C", "#5E2215", "#EADCC0") to Colors.hex("#F0E5CF")
            "onyx" -> c("#E8E2D0", "#8C8778", "#C9A04A", "#3A3A3A", "#B9B2A0") to Colors.hex("#141414")
            "amethyst" -> c("#5B2A9A", "#B89BE6", "#E6C76E", "#2E1456", "#EDE4F5") to Colors.hex("#EFE9F2")
            else -> listOf(p.accentA, p.accentB, p.accentC, p.line, Colors.lighten(p.bgTop, 0.4f)) to Colors.lighten(p.bgTop, 0.6f)
        }
    }

    /**
     * The sequence of moves a marbler would make for [style] on a w x h tray (canvas px).
     * Drops are sprinkled in rounds of each colour; later rounds push earlier ones
     * into the veined, stone-like cells of battal ebru.
     */
    fun moves(style: Style, w: Double, h: Double, colors: List<Int>, seed: Int, unit: Double = minOf(w, h)): List<Op> {
        val rnd = Random(seed)
        val ops = ArrayList<Op>()
        // Drop counts scale with tray area so density is the same on any canvas.
        val density = (w * h) / (unit * unit)
        // Battal: rounds of sprinkled drops, bigger first, smaller later.
        val rounds = 5
        for (round in 0 until rounds) {
            val color = colors[round % colors.size]
            val count = ((26 + round * 14) * density).toInt()
            val baseR = unit * (0.11 - round * 0.015)
            repeat(count) {
                val r = baseR * (0.55 + rnd.nextDouble() * 0.7)
                ops += Drop(rnd.nextDouble() * w, rnd.nextDouble() * h, r, color.jitter(rnd, 0.06f))
            }
        }
        if (style == Style.BATTAL) return ops

        // Gelgit: the stylus is drawn back and forth across the tray.
        val gel = unit / 9
        var y = rnd.nextDouble() * gel
        var dir = 1.0
        while (y < h + gel) {
            ops += Tine(0.0, y, 1.0, 0.0, dir * unit * 0.22, unit * 0.025)
            y += gel; dir = -dir
        }
        if (style == Style.GELGIT) return ops

        // Şal: then crosswise, and a gentle wave.
        val sal = unit / 7
        var x = rnd.nextDouble() * sal
        dir = 1.0
        while (x < w + sal) {
            ops += Tine(x, 0.0, 0.0, 1.0, dir * unit * 0.16, unit * 0.02)
            x += sal; dir = -dir
        }
        ops += Wave(0.0, 1.0, unit * 0.025, 2 * PI / (unit * 0.35), rnd.nextDouble() * 6)
        if (style == Style.SAL) return ops

        // Taraklı: a fine comb pulled through in one stroke.
        val tooth = unit / 28
        x = 0.0
        while (x < w + tooth) {
            ops += Tine(x, 0.0, 0.0, 1.0, unit * 0.09, unit * 0.006)
            x += tooth
        }
        if (style == Style.TARAKLI) return ops

        // Bülbül yuvası: little spirals worked into the combed ground.
        val step = unit / 3.2
        var sy = step * 0.5
        var row = 0
        while (sy < h + step) {
            var sx = step * (0.5 + 0.5 * (row % 2))
            while (sx < w + step) {
                ops += Swirl(sx, sy, (if (rnd.nextBoolean()) 1 else -1) * (5.5 + rnd.nextDouble() * 2), step * 0.18)
                sx += step
            }
            sy += step * 0.87; row++
        }
        return ops
    }

    /**
     * A marbled sheet covering [left, top, right, bottom], computed at reduced
     * resolution (marbling is soft, so bilinear upscaling is invisible) to stay
     * fast on the device.
     */
    fun sheet(
        style: Style, colors: List<Int>, base: Int, seed: Int,
        left: Double, top: Double, right: Double, bottom: Double, maxPixels: Int = 420_000,
    ): RasterItem {
        val w = right - left; val h = bottom - top
        // Marbling is soft: a quarter of the canvas pixels (capped) upscales invisibly,
        // and keeps thumbnails quick enough that a palette change shows at once.
        val budget = minOf(maxPixels.toDouble(), w * h / 4)
        val scale = maxOf(1.0, sqrt(w * h / budget))
        val tw = (w / scale).toInt().coerceAtLeast(1); val th = (h / scale).toInt().coerceAtLeast(1)
        // The marbler works a square tray as big as the long side, centred on the canvas,
        // so a rotated tablet (or the in-app preview) shows the same sheet, just cropped.
        val tray = maxOf(w, h)
        // Paint scale follows the visible short side, not the tray.
        val ops = moves(style, tray, tray, colors, seed, unit = minOf(w, h))
        val px = render(tw, th, scale, ops, base, seed, ox = (w - tray) / 2, oy = (h - tray) / 2)
        return RasterItem(tw, th, px, left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }
}

/** Full-bleed ebru wallpapers. */
object EbruArt {
    class Params(val style: Ebru.Style, val pigments: List<Int>? = null, val base: Int? = null)

    fun scene(ctx: RenderContext, p: Params): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val (pigments, water) = Ebru.pigments(pal)
        val colors = p.pigments ?: pigments
        val base = p.base ?: water
        b.raster(Ebru.sheet(p.style, colors, base, ctx.seed, 0.0, 0.0, ctx.w, ctx.h))
        // Marbled paper is printed onto paper: let its tooth show through.
        Textures.grainOverlay(b, ctx, 0.55f)
        Common.vignette(b, ctx, 0.25f)
        return b.build()
    }
}
