package com.noor.wallpapers.art

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Hand-made surfaces. These are what stop a flat vector image looking
 * computer-made: the tooth of paper, fibres, uneven ink and gold.
 */
object Textures {
    /** Tile size for repeating textures. */
    const val TILE = 256

    private val cache = HashMap<String, IntArray>()

    private fun cached(key: String, make: () -> IntArray): IntArray = synchronized(cache) { cache.getOrPut(key, make) }

    /**
     * Paper grain for MULTIPLY blending: near-white with soft mottling and fine
     * fibres. Tileable.
     */
    fun paperGrain(seed: Int = 7): IntArray = cached("paper$seed") {
        val n = TILE
        val mottle = Noise(seed, 4)
        val tooth = Noise(seed + 1, 64)
        val out = IntArray(n * n)
        val fibres = FloatArray(n * n)
        val rnd = Random(seed)
        // Fibres: short, faint curved strokes.
        repeat(220) {
            var x = rnd.nextDouble() * n; var y = rnd.nextDouble() * n
            var a = rnd.nextDouble() * Math.PI * 2
            val len = 6 + rnd.nextInt(22)
            val strength = 0.04f + rnd.nextFloat() * 0.07f
            repeat(len) {
                val ix = Math.floorMod(x.toInt(), n); val iy = Math.floorMod(y.toInt(), n)
                fibres[iy * n + ix] += strength
                a += (rnd.nextDouble() - 0.5) * 0.5
                x += Math.cos(a); y += Math.sin(a)
            }
        }
        for (y in 0 until n) for (x in 0 until n) {
            val m = mottle.fbm(x * 4.0 / n, y * 4.0 / n, 3)
            val t = tooth.at(x * 64.0 / n, y * 64.0 / n)
            val v = 1.0 - 0.06 * m - 0.07 * t - fibres[y * n + x]
            val c = (v.coerceIn(0.0, 1.0) * 255).roundToInt()
            out[y * n + x] = Colors.argb(255, c, c, (c * 0.985).roundToInt())
        }
        out
    }

    /** Paper grain over the whole canvas, multiplied in. [strength] 0..1. */
    fun grainOverlay(b: SceneBuilder, ctx: RenderContext, strength: Float = 0.6f, clip: Path? = null) {
        val px = paperGrain()
        val item = RasterItem(
            TILE, TILE, px, 0f, 0f, ctx.width.toFloat(), ctx.height.toFloat(),
            alpha = strength, blend = Blend.MULTIPLY, tiled = true, tileScale = (2.0 * ctx.u).toFloat().coerceAtLeast(0.5f),
        )
        if (clip == null) b.raster(item) else b.items += GroupItem(listOf(item), clip)
    }

    /**
     * Gold leaf: warm metal with burnish streaks and flecks, for filling gilded
     * shapes (clip a group to the shape and lay this over it). Tileable.
     */
    fun goldLeaf(base: Int, seed: Int = 3): IntArray = cached("gold$base$seed") {
        val n = TILE
        val streak = Noise(seed, 8)
        val fleck = Noise(seed + 5, 128)
        val out = IntArray(n * n)
        for (y in 0 until n) for (x in 0 until n) {
            val s = streak.fbm(x * 8.0 / n, y * 2.0 / n, 3)
            val f = fleck.at(x * 128.0 / n, y * 128.0 / n)
            var c = Colors.mix(Colors.darken(base, 0.25f), Colors.lighten(base, 0.35f), s.toFloat())
            if (f > 0.93) c = Colors.lighten(c, 0.5f) else if (f < 0.05) c = Colors.darken(c, 0.35f)
            out[y * n + x] = c
        }
        out
    }

    /** Lays gold leaf over everything already inside [clip]. */
    fun gild(b: SceneBuilder, ctx: RenderContext, clip: Path, base: Int, alpha: Float = 1f) {
        b.items += GroupItem(
            listOf(
                RasterItem(
                    TILE, TILE, goldLeaf(base), 0f, 0f, ctx.width.toFloat(), ctx.height.toFloat(),
                    alpha = alpha, tiled = true, tileScale = (1.5 * ctx.u).toFloat().coerceAtLeast(0.5f),
                ),
            ),
            clip,
        )
    }
}

/**
 * Hand-drawn strokes: lines that wobble slightly and swell and thin like a
 * brush or reed pen, instead of perfectly even vector strokes.
 */
object Brush {
    /** Flattens a path's curves into polylines (one per subpath). */
    fun flatten(p: Path, step: Double = 4.0): List<List<Vec>> {
        val out = ArrayList<List<Vec>>()
        var cur = ArrayList<Vec>()
        var start = Vec.ZERO
        var last = Vec.ZERO
        fun seg(n: Int, f: (Double) -> Vec) { for (i in 1..n) cur += f(i / n.toDouble()) }
        for (op in p.ops) when (op) {
            is Path.MoveTo -> {
                if (cur.size > 1) out += cur
                cur = ArrayList(); last = Vec(op.x.toDouble(), op.y.toDouble()); start = last; cur += last
            }
            is Path.LineTo -> {
                val to = Vec(op.x.toDouble(), op.y.toDouble())
                val n = ((to - last).length / step).toInt().coerceAtLeast(1)
                val from = last
                seg(n) { from + (to - from) * it }
                last = to
            }
            is Path.QuadTo -> {
                val p0 = last; val p1 = Vec(op.x1.toDouble(), op.y1.toDouble()); val p2 = Vec(op.x2.toDouble(), op.y2.toDouble())
                val n = (((p1 - p0).length + (p2 - p1).length) / step).toInt().coerceAtLeast(2)
                seg(n) { t -> p0 * ((1 - t) * (1 - t)) + p1 * (2 * (1 - t) * t) + p2 * (t * t) }
                last = p2
            }
            is Path.CubicTo -> {
                val p0 = last; val p1 = Vec(op.x1.toDouble(), op.y1.toDouble())
                val p2 = Vec(op.x2.toDouble(), op.y2.toDouble()); val p3 = Vec(op.x3.toDouble(), op.y3.toDouble())
                val n = (((p1 - p0).length + (p2 - p1).length + (p3 - p2).length) / step).toInt().coerceAtLeast(2)
                seg(n) { t ->
                    val u = 1 - t
                    p0 * (u * u * u) + p1 * (3 * u * u * t) + p2 * (3 * u * t * t) + p3 * (t * t * t)
                }
                last = p3
            }
            Path.Close -> {
                if (cur.isNotEmpty() && (cur.last() - start).length > 1e-6) cur += start
                if (cur.size > 1) out += cur
                cur = ArrayList(); last = start
            }
        }
        if (cur.size > 1) out += cur
        return out
    }

    /** Displaces every point a little along smooth noise: the tremor of a hand. */
    fun wobble(lines: List<List<Vec>>, amount: Double, scale: Double, seed: Int): List<List<Vec>> {
        val nx = Noise(seed); val ny = Noise(seed + 99)
        return lines.map { line ->
            line.map { p ->
                Vec(
                    p.x + (nx.at(p.x / scale, p.y / scale) - 0.5) * 2 * amount,
                    p.y + (ny.at(p.x / scale, p.y / scale) - 0.5) * 2 * amount,
                )
            }
        }
    }

    /** A polyline as a closed path (for filling hand-wobbled shapes). */
    fun toPath(lines: List<List<Vec>>, closed: Boolean): Path {
        val p = Path()
        for (l in lines) p.polygon(l, closed)
        return p
    }

    /**
     * A reed-pen / brush stroke: the polyline drawn as a filled outline whose
     * width swells and thins with smooth "pressure" noise and tapers at the ends.
     */
    fun inkStroke(line: List<Vec>, width: Double, seed: Int, taper: Boolean = true, pressure: Double = 0.35): Path {
        if (line.size < 2) return Path()
        val n = Noise(seed)
        val left = ArrayList<Vec>(line.size); val right = ArrayList<Vec>(line.size)
        var dist = 0.0
        for (i in line.indices) {
            if (i > 0) dist += (line[i] - line[i - 1]).length
            val a = line[maxOf(0, i - 1)]; val b = line[minOf(line.size - 1, i + 1)]
            val d = (b - a).normalized()
            val normal = Vec(-d.y, d.x)
            val t = i / (line.size - 1).toDouble()
            val ends = if (taper) minOf(1.0, minOf(t, 1 - t) * 6 + 0.25) else 1.0
            val w = width * ends * (1 - pressure + pressure * 2 * n.at(dist / (width * 6), 0.5)) / 2
            left += line[i] + normal * w
            right += line[i] - normal * w
        }
        return Path().polygon(left + right.reversed())
    }

    /** Hand-inks every subpath of [p]. */
    fun ink(p: Path, width: Double, seed: Int, wobble: Double = width * 0.35, closedShapes: Boolean = true): Path {
        val lines = wobble(flatten(p, step = maxOf(1.5, width)), wobble, width * 14, seed)
        val out = Path()
        lines.forEachIndexed { i, l -> out.append(inkStroke(l, width, seed + i * 7, taper = !closedShapes)) }
        return out
    }
}

/** Small colour variation, as in hand-mixed paint or glaze. */
fun Int.jitter(rnd: Random, amount: Float): Int {
    val t = (rnd.nextFloat() - 0.5f) * 2 * amount
    return if (t >= 0) Colors.lighten(this, t) else Colors.darken(this, abs(t))
}
