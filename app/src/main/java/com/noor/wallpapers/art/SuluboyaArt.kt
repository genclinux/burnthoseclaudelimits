package com.noor.wallpapers.art

import com.noor.wallpapers.art.SuluboyaArt.Mood
import java.util.stream.IntStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Suluboya: mosques painted in watercolour on cold-press paper.
 *
 * The paintings are made the way a painter makes them, not as flat vector
 * shapes. A small simulation of pigment on wet paper ([WetSheet]) lays down
 * translucent washes one after another. Each wash is a pool of water whose
 * edge wanders with the paper and the brush (noise), whose pigment drifts to
 * the drying rim (edge darkening), settles into the tooth of the paper
 * (granulation), blooms where wetter paint runs back into a drying wash, and
 * mixes with its neighbour while wet. Washes glaze over one another by
 * multiplying their optical densities, so overlaps darken like real pigment.
 *
 * On top of that come the things the hand adds: a faint graphite
 * underdrawing, confident dark brush strokes for minarets and balconies,
 * lifted-out lights, a few birds, splatter, and the unpainted ragged margin
 * of the sheet.
 */
object SuluboyaArt {

    enum class Mood { DAWN, SUNSET, NIGHT, MIST, RAIN, WINTER, SPRING, KANDIL }

    /**
     * A painter's box for one palette: [sky] (cerulean / ultramarine), [deep]
     * (indigo / Payne's grey), [glow] (yellow ochre / Naples), [rose]
     * (quinacridone rose), [earth] (burnt sienna), [green] (sap green),
     * [shade] (a mixed neutral for shadows), [ink] (sepia / dark for the
     * brush line) and the [paper] they are painted on. Each is the colour the
     * pigment gives at an ordinary wash strength.
     */
    class Pigments(
        val sky: Int, val deep: Int, val glow: Int, val rose: Int, val earth: Int,
        val green: Int, val shade: Int, val ink: Int, val paper: Int,
    )

    fun pigments(p: Palette): Pigments {
        fun k(vararg h: String) = h.map { Colors.hex(it) }.let {
            Pigments(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8])
        }
        return when (p.id) {
            // Rose gold and teal: cerulean, phthalo turquoise, peach, quinacridone rose.
            "betul" -> k("#86AFBC", "#2B5A68", "#EDB98F", "#D98892", "#B0705A", "#8BAA8A", "#8E7889", "#2E2433", "#FBF6EE")
            // Sap green, quinacridone gold, alizarin.
            "emerald" -> k("#86B2C2", "#1F4A49", "#E6BE62", "#C9575A", "#9A6838", "#4E8A58", "#6F786C", "#1E2A24", "#FAF6EA")
            // French ultramarine, indanthrone, yellow ochre.
            "lapis" -> k("#7590CB", "#22306A", "#E7BF78", "#C98698", "#A26A48", "#7B9876", "#6F6D90", "#1B2140", "#F9F6EF")
            // Cobalt turquoise, Iznik red, ochre.
            "iznik" -> k("#6CB0BE", "#1C5266", "#E6CB93", "#C2564A", "#9C6A4C", "#5C987C", "#5F7A86", "#1A2C36", "#FAF7EF")
            // Raw sienna, burnt sienna, sepia, a little cerulean.
            "sand" -> k("#A6BAC4", "#4B3A31", "#E7B566", "#C88168", "#A85A30", "#8A8D58", "#8C7362", "#3A291F", "#FBF3E3")
            // Opera rose, cobalt teal, Naples yellow, plum.
            "isfahan" -> k("#8AA4C3", "#44284C", "#F0C28C", "#C9547E", "#A6624F", "#5D9387", "#896884", "#2C1A2C", "#FBF4F0")
            // Cadmium orange, burnt sienna, viridian.
            "alhambra" -> k("#93ADBF", "#3B2D4C", "#F0B159", "#D2624A", "#A9472A", "#5C896A", "#8A5D58", "#381A12", "#FBF2E3")
            // Monochrome: Payne's grey and sepia.
            "onyx" -> k("#A3AAB1", "#353A41", "#D8CCAE", "#B7A6A3", "#8A7C6F", "#8C9489", "#7B8087", "#1C1E22", "#F8F6F1")
            // Dioxazine violet, lavender, Naples gold.
            "amethyst" -> k("#A29ACA", "#38286A", "#E8CB84", "#C78AB6", "#9B6F62", "#7D998A", "#7D6E9A", "#22173D", "#FAF6F6")
            // Marmara evening: cerulean sea, Prussian blue, apricot, coral and island violet.
            "marmara" -> k("#7FB2CC", "#1D4560", "#F1C08A", "#E0826A", "#A8644A", "#6E9878", "#7C7894", "#1A2638", "#FBF6EC")
            else -> Pigments(
                Colors.lighten(p.accentA, 0.5f), p.bgTop, Colors.lighten(p.glow, 0.1f), p.accentC, p.accentB,
                Colors.hex("#7B9876"), Colors.mix(p.bgTop, Colors.WHITE, 0.4f), p.bgBottom, Colors.hex("#FAF6EE"),
            )
        }
    }

    // ------------------------------------------------------------ scenes

    fun scene(ctx: RenderContext, mood: Mood): Scene = Painting(ctx, mood).paint()

    private fun entry(id: String, title: String, palette: String, mood: Mood, seed: Int) =
        Entry("suluboya-$id", title, Category.SULUBOYA, Palette.byId(palette), seed) { ctx -> scene(ctx, mood) }

    fun entries(): List<Entry> = listOf(
        entry("safak", "İstanbul'da Şafak", "betul", Mood.DAWN, 3),
        entry("gun-batimi", "Haliç'te Gün Batımı", "alhambra", Mood.SUNSET, 5),
        entry("hilal", "Hilal Gecesi", "lapis", Mood.NIGHT, 7),
        entry("sisli-sabah", "Sisli Sabah", "onyx", Mood.MIST, 2),
        entry("yagmur", "Yağmurlu Akşam", "iznik", Mood.RAIN, 11),
        entry("kis", "Karlı Kış", "amethyst", Mood.WINTER, 4),
        entry("lale", "Lâle Bahçesi", "emerald", Mood.SPRING, 6),
        entry("kandil", "Kandil Gecesi", "isfahan", Mood.KANDIL, 9),
    )
}

// =================================================================== engine

internal fun smooth(e0: Double, e1: Double, x: Double): Double {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0.0, 1.0)
    return t * t * (3 - 2 * t)
}

internal fun Path.moved(dx: Double, dy: Double): Path =
    transformed { x, y -> (x + dx).toFloat() to (y + dy).toFloat() }

internal fun rectP(x0: Double, y0: Double, x1: Double, y1: Double) =
    Path().rect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1))

/** A wetter drop landing in a drying wash: pigment is pushed to a frilly rim. */
internal class Bloom(val x: Double, val y: Double, val r: Double, val strength: Double = 1.0)

/**
 * Noise fields shared by every wash on a sheet of a given size: slow pigment
 * flow, edge raggedness, paper tooth and the painter's unpainted margin.
 * Computed once per canvas size and reused (thumbnails, re-renders).
 */
internal class SheetFields(val w: Int, val h: Int, sc: Double, u: Double) {
    val n = w * h
    val flow = FloatArray(n)
    val flow2 = FloatArray(n)
    val rag = FloatArray(n)
    val tooth = FloatArray(n)
    val margin = FloatArray(n)

    init {
        val nf = Noise(4101); val nf2 = Noise(4202); val nr = Noise(4303)
        val nt = Noise(4404); val nt2 = Noise(4505); val nm = Noise(4606)
        val wD = w * sc / u; val hD = h * sc / u
        fun stretch(v: Double, k: Double) = (0.5 + (v - 0.5) * k).coerceIn(0.0, 1.0).toFloat()
        IntStream.range(0, h).parallel().forEach { y ->
            val dy = (y + 0.5) * sc / u
            for (x in 0 until w) {
                val dx = (x + 0.5) * sc / u
                val i = y * w + x
                flow[i] = stretch(nf.fbm(dx / 380, dy / 380, 3), 2.2)
                flow2[i] = stretch(nf2.fbm(dx / 150 + 13.1, dy / 150, 3), 2.2)
                rag[i] = stretch(nr.fbm(dx / 28, dy / 28, 4), 2.4)
                // Cold-press tooth: rounded bumps a few millimetres across.
                tooth[i] = stretch(0.7 * nt.fbm(dx / 8.0, dy / 8.0, 2) + 0.3 * nt2.at(dx / 3.4, dy / 3.4), 1.9)
                // Distance (design px) inside the ragged edge the painter didn't cross.
                val e = min(min(dx, dy), min(wD - dx, hD - dy))
                val along = nm.fbm(dx / 600, dy / 600, 2)
                val m = 1.5 + 20 * smooth(0.5, 0.8, along) + 2.5 * (rag[i] - 0.5)
                margin[i] = (e - m).toFloat()
            }
        }
    }

    companion object {
        private val cache = LinkedHashMap<String, SheetFields>()
        fun get(w: Int, h: Int, sc: Double, u: Double): SheetFields {
            val key = "$w:$h:${"%.4f".format(sc)}:${"%.4f".format(u)}"
            synchronized(cache) {
                cache[key]?.let { return it }
            }
            val f = SheetFields(w, h, sc, u)
            synchronized(cache) {
                cache[key] = f
                while (cache.size > 2) cache.remove(cache.keys.first())
            }
            return f
        }
    }
}

/**
 * A sheet of paper being painted, held as optical density per RGB channel at
 * reduced resolution (watercolour is soft; it upscales invisibly). Each wash
 * adds density, so layered washes glaze like transparent pigment.
 */
internal class WetSheet(val ctx: RenderContext, val paper: Int, budget: Int = 880_000) {
    val sc = max(1.0, sqrt(ctx.w * ctx.h / budget))
    val w = ceil(ctx.w / sc).toInt()
    val h = ceil(ctx.h / sc).toInt()
    val f = SheetFields.get(w, h, sc, ctx.u)
    val od = FloatArray(w * h * 3)
    private val uc = ctx.u / sc // cells per design px

    private class Mask(val x0: Int, val y0: Int, val w: Int, val h: Int, val m: FloatArray)

    private fun density(c: Int) = doubleArrayOf(
        -ln(max(Colors.red(c), 4) / 255.0), -ln(max(Colors.green(c), 4) / 255.0), -ln(max(Colors.blue(c), 4) / 255.0),
    )

    /**
     * Lays one wash of [color] (bleeding into [color2] where [mix] says, plus a
     * little drift) over [path]. [soft] is how wet the edge is (design px of
     * spread), [hard] how crisp it dries (0.02 crisp .. 0.45 melting), [rag]
     * how far the edge wanders. [edge] darkens the drying rim, [gran] settles
     * pigment into the tooth, [miss] leaves skipped holes, [dry] makes it a
     * dry-brush stroke that only catches the tops of the tooth.
     */
    fun wash(
        path: Path, color: Int, color2: Int = color, density: Double = 0.5,
        soft: Double = 3.0, hard: Double = 0.1, rag: Double = 0.5,
        edge: Double = 0.8, edgeWidth: Double = 5.0, gran: Double = 0.35, flowVar: Double = 0.3,
        miss: Double = 0.0, dry: Double = 0.0, clip: Path? = null,
        mix: ((Double, Double) -> Double)? = null, mixNoise: Double = 0.35,
        grade: ((Double, Double) -> Double)? = null, blooms: List<Bloom> = emptyList(),
        frame: Boolean = true, frameJitter: Double = 0.0,
    ) {
        val mask = shapeMask(path, clip, soft, hard, rag, frame, frameJitter, edgeWidth) ?: return
        val eR = (edgeWidth * uc).roundToInt().coerceAtLeast(1)
        val bl = mask.m.copyOf()
        blur(bl, mask.w, mask.h, eR, 1)
        val a = density(color); val b = density(color2)
        val f = f
        IntStream.range(0, mask.h).parallel().forEach { y ->
            val gy = mask.y0 + y
            val cy = (gy + 0.5) * sc
            for (x in 0 until mask.w) {
                val i = y * mask.w + x
                val mm = mask.m[i].toDouble()
                if (mm < 0.004) continue
                val gx = mask.x0 + x
                val gi = gy * w + gx
                val cx = (gx + 0.5) * sc
                val g = grade?.invoke(cx, cy) ?: 1.0
                if (g <= 0.0) continue
                val fl = f.flow[gi].toDouble(); val fl2 = f.flow2[gi].toDouble()
                val rg = f.rag[gi].toDouble(); val tt = f.tooth[gi].toDouble()
                var d = density * mm * g * (1 - flowVar + 2 * flowVar * fl)
                val rim = mm - bl[i]
                if (rim > 0) d += density * g * edge * rim * 2.2 * (0.45 + 1.1 * fl2)
                // Granulation: pigment settles in the hollows of the tooth, in patches.
                d *= max(0.0, 1 + gran * (0.5 - tt) * 0.8 * (0.4 + 1.2 * fl2))
                if (miss > 0) d *= smooth(miss - 0.06, miss + 0.06, 0.55 * fl2 + 0.45 * rg)
                if (dry > 0) d *= smooth(dry - 0.07, dry + 0.07, 0.75 * tt + 0.25 * rg)
                for (bm in blooms) {
                    val dx = cx - bm.x; val dy = cy - bm.y
                    val rr = sqrt(dx * dx + dy * dy) / (bm.r * (0.7 + 0.6 * rg))
                    if (rr < 1.3) {
                        if (rr < 1) d *= 1 - bm.strength * 0.6 * (1 - rr * rr)
                        val q = (rr - 1) / 0.055
                        d += density * g * bm.strength * 0.9 * exp(-q * q) * mm
                    }
                }
                val t = ((mix?.invoke(cx, cy) ?: 0.0) + (fl - 0.5) * mixNoise * 2).coerceIn(0.0, 1.0)
                val o = gi * 3
                od[o] += (d * (a[0] + (b[0] - a[0]) * t)).toFloat()
                od[o + 1] += (d * (a[1] + (b[1] - a[1]) * t)).toFloat()
                od[o + 2] += (d * (a[2] + (b[2] - a[2]) * t)).toFloat()
            }
        }
    }

    /** Lifts paint back off with a damp brush or tissue: lights, moon, snow, streaks. */
    fun lift(path: Path, amount: Double, soft: Double = 2.0, hard: Double = 0.12, rag: Double = 0.4, clip: Path? = null) {
        val mask = shapeMask(path, clip, soft, hard, rag, false, 0.0, 0.0) ?: return
        IntStream.range(0, mask.h).parallel().forEach { y ->
            val gy = mask.y0 + y
            for (x in 0 until mask.w) {
                val mm = mask.m[y * mask.w + x]
                if (mm < 0.004) continue
                val gi = gy * w + mask.x0 + x
                // Lifting never comes perfectly clean: a ghost stays in the tooth.
                val k = (1 - amount * mm * (0.92 + 0.16 * (f.tooth[gi] - 0.5))).toFloat().coerceIn(0f, 1f)
                val o = gi * 3
                od[o] *= k; od[o + 1] *= k; od[o + 2] *= k
            }
        }
    }

    private fun shapeMask(
        path: Path, clip: Path?, soft: Double, hard: Double, rag: Double,
        frame: Boolean, frameJitter: Double, extra: Double,
    ): Mask? {
        val lines = Brush.flatten(path, max(1.0, sc * 0.8))
        if (lines.isEmpty()) return null
        var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        for (l in lines) for (p in l) {
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
        }
        val pad = ceil((soft * 2.2 + extra * 1.5 + rag * soft) * uc).toInt() + 2
        val x0 = max(0, floor(minX / sc).toInt() - pad)
        val y0 = max(0, floor(minY / sc).toInt() - pad)
        val x1 = min(w, ceil(maxX / sc).toInt() + pad)
        val y1 = min(h, ceil(maxY / sc).toInt() + pad)
        val bw = x1 - x0; val bh = y1 - y0
        if (bw <= 0 || bh <= 0) return null
        val cov = rasterize(lines, path.evenOdd, x0, y0, bw, bh)
        if (clip != null) {
            val c = rasterize(Brush.flatten(clip, max(1.0, sc * 0.8)), clip.evenOdd, x0, y0, bw, bh)
            for (i in cov.indices) cov[i] *= c[i]
        }
        val r = (soft * uc).roundToInt()
        if (r > 0) blur(cov, bw, bh, r, 2)
        val f = f
        // Big wet washes wander in long slow curves; only small strokes get crunchy edges.
        val fine = (8.0 / max(soft, 1.0)).coerceIn(0.12, 1.0)
        IntStream.range(0, bh).parallel().forEach { y ->
            for (x in 0 until bw) {
                val i = y * bw + x
                val gi = (y0 + y) * w + x0 + x
                // Noise moves the edge, never the middle: no holes punched in a wash.
                val c0 = cov[i]
                val zone = min(1.0, 4.0 * c0 * (1 - c0) + 0.12)
                val t = c0 + ((f.rag[gi] - 0.5) * 0.8 * fine + (f.flow2[gi] - 0.5) * (0.5 + 0.5 * (1 - fine))) * rag * zone
                var v = smooth(0.5 - hard, 0.5 + hard, t)
                if (frame && v > 0) v *= smooth(-1.2, 1.2, f.margin[gi] + frameJitter)
                cov[i] = v.toFloat()
            }
        }
        return Mask(x0, y0, bw, bh, cov)
    }

    /** Coverage of closed polylines on a grid of cells (two sub-rows per cell, exact spans). */
    private fun rasterize(lines: List<List<Vec>>, evenOdd: Boolean, x0: Int, y0: Int, bw: Int, bh: Int): FloatArray {
        val sub = 2
        val ns = bh * sub
        val counts = IntArray(ns + 1)
        val local = lines.map { l -> l.map { Vec(it.x / sc - x0, it.y / sc - y0) } }
        // Every subpath is turned the same way round, so overlapping parts unite.
        val dirs = local.map { l ->
            var a = 0.0
            for (i in l.indices) { val p = l[i]; val q = l[(i + 1) % l.size]; a += p.x * q.y - q.x * p.y }
            if (a < 0) -1 else 1
        }
        fun edges(block: (Double, Double, Double, Double, Int) -> Unit) {
            for ((li, l) in local.withIndex()) {
                val s = if (evenOdd) 1 else dirs[li]
                for (i in l.indices) {
                    val p = l[i]; val q = l[(i + 1) % l.size]
                    if (p.y != q.y) block(p.x, p.y, q.x, q.y, if (q.y > p.y) s else -s)
                }
            }
        }
        fun range(ay: Double, by: Double): IntRange {
            val lo = min(ay, by); val hi = max(ay, by)
            val k0 = max(0, ceil(lo * sub - 0.5).toInt())
            val k1 = min(ns - 1, ceil(hi * sub - 0.5).toInt() - 1)
            return k0..k1
        }
        edges { _, ay, _, by, _ -> for (k in range(ay, by)) counts[k + 1]++ }
        for (k in 1..ns) counts[k] += counts[k - 1]
        val total = counts[ns]
        val xs = FloatArray(total); val ds = ByteArray(total)
        val fill = counts.copyOf()
        edges { ax, ay, bx, by, d ->
            for (k in range(ay, by)) {
                val yy = (k + 0.5) / sub
                val j = fill[k]++
                xs[j] = (ax + (yy - ay) * (bx - ax) / (by - ay)).toFloat()
                ds[j] = d.toByte()
            }
        }
        val cov = FloatArray(bw * bh)
        IntStream.range(0, bh).parallel().forEach { row ->
            for (k in row * sub until row * sub + sub) {
                val s = counts[k]; val e = counts[k + 1]
                for (i in s + 1 until e) {
                    val x = xs[i]; val d = ds[i]; var j = i - 1
                    while (j >= s && xs[j] > x) { xs[j + 1] = xs[j]; ds[j + 1] = ds[j]; j-- }
                    xs[j + 1] = x; ds[j + 1] = d
                }
                var wind = 0; var start = 0f
                for (i in s until e) {
                    val before = wind
                    wind = if (evenOdd) wind xor 1 else wind + ds[i]
                    if (before == 0 && wind != 0) start = xs[i]
                    else if (before != 0 && wind == 0) span(cov, row * bw, bw, start, xs[i], 1f / sub)
                }
            }
        }
        for (i in cov.indices) if (cov[i] > 1f) cov[i] = 1f
        return cov
    }

    private fun span(cov: FloatArray, off: Int, bw: Int, a: Float, b: Float, wgt: Float) {
        val xa = a.coerceIn(0f, bw.toFloat()); val xb = b.coerceIn(0f, bw.toFloat())
        if (xb <= xa) return
        val ia = xa.toInt(); val ib = xb.toInt()
        if (ia == ib) { if (ia < bw) cov[off + ia] += (xb - xa) * wgt; return }
        cov[off + ia] += (ia + 1 - xa) * wgt
        for (i in ia + 1 until ib) cov[off + i] += wgt
        if (ib < bw) cov[off + ib] += (xb - ib) * wgt
    }

    /** Separable box blur, [passes] times (two passes look Gaussian). */
    private fun blur(a: FloatArray, bw: Int, bh: Int, r: Int, passes: Int) {
        val norm = 1f / (2 * r + 1)
        repeat(passes) {
            IntStream.range(0, bh).parallel().forEach { y ->
                val off = y * bw
                val src = a.copyOfRange(off, off + bw)
                var acc = 0f
                for (k in 0..min(r, bw - 1)) acc += src[k]
                for (x in 0 until bw) {
                    a[off + x] = acc * norm
                    val add = x + r + 1; val rem = x - r
                    if (add < bw) acc += src[add]
                    if (rem >= 0) acc -= src[rem]
                }
            }
            val chunks = 8
            IntStream.range(0, chunks).parallel().forEach { c ->
                val xa = bw * c / chunks; val xb = bw * (c + 1) / chunks
                val n = xb - xa
                if (n <= 0) return@forEach
                val acc = FloatArray(n)
                val col = Array(n) { FloatArray(bh) }
                for (y in 0 until bh) for (x in 0 until n) col[x][y] = a[y * bw + xa + x]
                for (x in 0 until n) { var s = 0f; for (k in 0..min(r, bh - 1)) s += col[x][k]; acc[x] = s }
                for (y in 0 until bh) {
                    val off = y * bw + xa
                    for (x in 0 until n) {
                        a[off + x] = acc[x] * norm
                        val add = y + r + 1; val rem = y - r
                        if (add < bh) acc[x] += col[x][add]
                        if (rem >= 0) acc[x] -= col[x][rem]
                    }
                }
            }
        }
    }

    /** The finished sheet: paper colour through the pigment, with the paper's tooth in raking light. */
    fun toRaster(): RasterItem {
        val px = IntArray(w * h)
        val pr = Colors.red(paper); val pg = Colors.green(paper); val pb = Colors.blue(paper)
        IntStream.range(0, h).parallel().forEach { y ->
            for (x in 0 until w) {
                val i = y * w + x
                val o = i * 3
                // The tooth in raking light: each bump lit on one side, shadowed on the other.
                val a = f.tooth[max(0, i - w - 1)]; val c = f.tooth[min(w * h - 1, i + w + 1)]
                val tex = 0.985 + 0.075 * (a - c) + 0.025 * (f.tooth[i] - 0.5) + 0.015 * (f.flow[i] - 0.5)
                px[i] = Colors.argb(
                    255,
                    (pr * exp(-od[o].toDouble()) * tex).roundToInt(),
                    (pg * exp(-od[o + 1].toDouble()) * tex).roundToInt(),
                    (pb * exp(-od[o + 2].toDouble()) * tex).roundToInt(),
                )
            }
        }
        return RasterItem(w, h, px, 0f, 0f, (w * sc).toFloat(), (h * sc).toFloat())
    }
}

// =============================================================== the hand

internal const val GRAPHITE = 0xFF55535A.toInt()

/** Hand-drawn marks laid over the washes: graphite, brush line, birds, splatter. */
internal class Hand(val b: SceneBuilder, val ctx: RenderContext, val seed: Int) {
    val u = ctx.u
    private var salt = seed * 31

    /** Faint pencil underdrawing: wobbly, sometimes broken, overshooting the corners. */
    fun pencil(paths: List<Path>, alpha: Float = 0.26f, skip: Double = 0.18) {
        val rnd = Random(seed + 501)
        val out = Path()
        for ((i, p) in paths.withIndex()) {
            val lines = Brush.wobble(Brush.flatten(p, 3.0 * u), 1.5 * u, 70 * u, seed + i * 13)
            for (l in lines) {
                if (l.size < 2) continue
                // The pencil lifts here and there.
                var cur = ArrayList<Vec>()
                for (pt in l) {
                    if (rnd.nextDouble() < skip * 0.04 && cur.size > 3) {
                        out.polygon(cur, closed = false); cur = ArrayList()
                    }
                    cur += pt
                }
                if (cur.size > 1) out.polygon(cur, closed = false)
            }
        }
        b.stroke(out, GRAPHITE, (1.0 * u).toFloat().coerceAtLeast(0.6f), alpha)
    }

    /** Straight construction lines, drawn freehand and running past where they're needed. */
    fun guides(lines: List<Pair<Vec, Vec>>, alpha: Float = 0.2f) {
        val rnd = Random(seed + 77)
        val out = Path()
        for ((a, c) in lines) {
            val d = c - a
            val pts = (0..24).map { a + d * (it / 24.0) }
            val wob = Brush.wobble(listOf(pts), 1.4 * u, 120 * u, seed + rnd.nextInt(1000))
            out.polygon(wob[0], closed = false)
        }
        b.stroke(out, GRAPHITE, (0.9 * u).toFloat().coerceAtLeast(0.5f), alpha)
    }

    /**
     * A confident brush line along [pts]: it swells and thins, wobbles with
     * the hand, and the brush leaves the paper once or twice ([breaks]).
     */
    fun line(pts: List<Vec>, width: Double, color: Int, alpha: Float, breaks: Int = 1, wob: Double = 1.0) {
        salt++
        val rnd = Random(salt)
        val dense = Brush.flatten(Path().polygon(pts, closed = false), max(1.0, width * 0.7))
        if (dense.isEmpty()) return
        val line = Brush.wobble(dense, wob * u, 50 * u, salt)[0]
        if (line.size < 3) return
        val cuts = (0 until breaks).filter { rnd.nextDouble() < 0.7 }.map { 0.2 + rnd.nextDouble() * 0.6 }.sorted()
        var start = 0
        val pieces = ArrayList<List<Vec>>()
        for (c in cuts) {
            val at = (c * line.size).toInt()
            val gap = max(1, (line.size * (0.04 + rnd.nextDouble() * 0.08)).toInt())
            if (at - start > 2) pieces += line.subList(start, at)
            start = min(line.size - 1, at + gap)
        }
        if (line.size - start > 2) pieces += line.subList(start, line.size)
        for ((k, p) in pieces.withIndex()) {
            b.fill(Brush.inkStroke(p, width, salt * 7 + k, taper = true, pressure = 0.5), color, alpha)
        }
    }

    /** A seagull or swift: two quick curved strokes meeting at the body. */
    fun bird(x: Double, y: Double, s: Double, color: Int, alpha: Float, rnd: Random) {
        val tilt = (rnd.nextDouble() - 0.5) * 0.5
        fun r(p: Vec) = (p - Vec(x, y)).rotated(tilt) + Vec(x, y)
        val lift = 0.35 + rnd.nextDouble() * 0.35
        fun curve(a: Vec, c: Vec, e: Vec) = (0..10).map { i ->
            val t = i / 10.0
            r(a * ((1 - t) * (1 - t)) + c * (2 * (1 - t) * t) + e * (t * t))
        }
        val l = curve(Vec(x - s, y - s * 0.18), Vec(x - s * 0.45, y - s * lift * 1.4), Vec(x, y))
        val rr = curve(Vec(x, y), Vec(x + s * 0.4, y - s * lift * 1.5), Vec(x + s * 0.95, y - s * 0.3))
        b.fill(Brush.inkStroke(l, s * 0.16, rnd.nextInt(), pressure = 0.5), color, alpha)
        b.fill(Brush.inkStroke(rr, s * 0.14, rnd.nextInt(), pressure = 0.5), color, alpha)
    }

    /** Flicked paint: a scatter of irregular dots, a few elongated. */
    fun splatter(cx: Double, cy: Double, spread: Double, count: Int, colors: List<Int>, alpha: Float, rnd: Random) {
        val byColor = colors.map { Path() }
        val n = Noise(seed + 909)
        repeat(count) {
            val a = rnd.nextDouble() * 2 * PI
            val d = spread * rnd.nextDouble().pow(1.4)
            val x = cx + cos(a) * d; val y = cy + sin(a) * d * 0.8
            val r = u * (0.7 + 4.5 * rnd.nextDouble().pow(4))
            val stretch = if (rnd.nextDouble() < 0.15) 1.8 + rnd.nextDouble() else 1.0
            val pts = (0 until 12).map { k ->
                val t = k / 12.0 * 2 * PI
                val rr = r * (0.75 + 0.5 * n.at(x / u + cos(t) * 1.7, y / u + sin(t) * 1.7))
                val v = Vec(cos(t) * rr * stretch, sin(t) * rr).rotated(a)
                Vec(x, y) + v
            }
            byColor[rnd.nextInt(colors.size)].polygon(pts)
        }
        for ((i, p) in byColor.withIndex()) b.fill(p, colors[i], alpha)
    }

    /** Opaque white gouache dots: stars, snow, sparkle on water. */
    fun gouache(points: List<Vec>, radius: (Random) -> Double, color: Int, alpha: Float, rnd: Random) {
        val p = Path()
        for (pt in points) {
            val r = radius(rnd)
            val pts = (0 until 9).map { k ->
                val t = k / 9.0 * 2 * PI
                pt + Vec(cos(t), sin(t)) * (r * (0.75 + rnd.nextDouble() * 0.45))
            }
            p.polygon(pts)
        }
        b.fill(p, color, alpha)
    }
}

// ============================================================== the mosque

internal class Minaret(val x: Double, val base: Double, val top: Double, val w: Double, val capBase: Double, val balconies: List<Double>)

internal class Dome(val cx: Double, val base: Double, val r: Double, val h: Double) {
    val path get() = Shapes.hemiDome(cx, base, r, h)
}

/** An Ottoman mosque in the manner of Süleymaniye, seen a little from one side. */
internal class Camii(
    val body: Path, val parts: List<Path>, val domes: List<Dome>, val minarets: List<Minaret>,
    val windows: Path, val arcade: Path, val left: Double, val right: Double, val top: Double,
    val ground: Double, val hallTop: Double, val hallLeft: Double, val hallRight: Double, val s: Double,
)

/**
 * Builds the drawing. [s] is pixels per design unit, [flip] puts the
 * courtyard on the right, [plan] is 2 or 4 minarets.
 */
internal fun camii(cx: Double, ground: Double, s: Double, plan: Int, flip: Boolean, rnd: Random): Camii {
    val dir = if (flip) -1.0 else 1.0
    fun X(o: Double) = cx + dir * o * s
    fun L(v: Double) = v * s
    val body = Path()
    val parts = ArrayList<Path>()
    val domes = ArrayList<Dome>()
    val minarets = ArrayList<Minaret>()
    val windows = Path(); val arcade = Path()
    fun add(p: Path) { body.append(p); parts += p }
    fun dome(x: Double, base: Double, r: Double, hh: Double) {
        val d = Dome(x, base, r, hh); domes += d; add(d.path)
    }
    fun alem(x: Double, y: Double, len: Double) {
        add(rectP(x - L(1.6), y - len, x + L(1.6), y + L(2.0)))
        add(Path().circle(x, y - len * 0.45, L(3.6)))
    }

    val hallH = 128 + rnd.nextDouble() * 14
    val hallTop = ground - L(hallH)
    add(rectP(X(-250.0), hallTop, X(250.0), ground))

    // Main dome high on its drum, between two half domes.
    val drum = 62.0
    val domeBase = hallTop - L(drum)
    add(rectP(X(-128.0), domeBase, X(128.0), hallTop))
    for (o in listOf(-150.0, 150.0)) dome(X(o), hallTop, L(94.0), L(74.0))
    dome(cx, domeBase, L(126.0), L(116.0))
    alem(cx, domeBase - L(116.0), L(44.0))
    // Weight towers at the drum's corners and small domes over the side bays.
    for (o in listOf(-136.0, 136.0)) {
        add(rectP(X(o) - L(10.0), domeBase - L(26.0), X(o) + L(10.0), hallTop))
        dome(X(o), domeBase - L(26.0), L(13.0), L(15.0))
        alem(X(o), domeBase - L(41.0), L(12.0))
    }
    for (o in listOf(-218.0, 218.0)) {
        dome(X(o), hallTop, L(32.0), L(29.0))
        alem(X(o), hallTop - L(29.0), L(14.0))
    }
    // Courtyard and its arcade of little domes.
    val courtH = 78.0
    add(rectP(X(-250.0), ground - L(courtH), X(-528.0), ground))
    for (i in 0 until 8) dome(X(-266.0 - i * 34.5), ground - L(courtH), L(14.5), L(13.5))
    add(rectP(X(-400.0), ground - L(112.0), X(-360.0), ground))

    // Windows on the drum and in two rows on the hall; the arcade's dark arches.
    for (i in -5..5) windows.append(Shapes.pointedArch(X(i * 21.0), hallTop - L(9.0), L(10.0), L(34.0)))
    for (i in -4..3) {
        windows.append(Shapes.pointedArch(X(-210.0 + (i + 4) * 60.0), ground - L(22.0), L(20.0), L(46.0)))
        windows.append(Shapes.pointedArch(X(-210.0 + (i + 4) * 60.0), ground - L(86.0), L(15.0), L(28.0)))
    }
    for (i in 0 until 7) arcade.append(Shapes.pointedArch(X(-272.0 - i * 36.0), ground - L(4.0), L(24.0), L(50.0)))
    arcade.append(Shapes.pointedArch(X(-380.0), ground - L(4.0), L(26.0), L(78.0)))

    // Pencil minarets with their balconies (şerefe) and lead caps.
    val plans = if (plan == 4) listOf(
        doubleArrayOf(-272.0, 735.0, 3.0, 0.0), doubleArrayOf(-218.0, 700.0, 3.0, 8.0),
        doubleArrayOf(-522.0, 565.0, 2.0, 0.0), doubleArrayOf(-474.0, 540.0, 2.0, 6.0),
    ) else listOf(doubleArrayOf(-276.0, 650.0, 2.0, 0.0), doubleArrayOf(270.0, 615.0, 2.0, 4.0))
    for (p in plans) {
        val x = X(p[0]); val base = ground - L(p[3]); val height = L(p[1] + rnd.nextDouble() * 20)
        val w = L(25.0)
        val top = base - height
        val capBase = top + height * 0.16
        add(rectP(x - w / 2, capBase, x + w / 2, base))
        add(rectP(x - w * 0.72, base - L(82.0), x + w * 0.72, base))
        val bals = ArrayList<Double>()
        for (k in 0 until p[2].toInt()) {
            val y = capBase + L(30.0) + k * height * 0.15
            bals += y
            add(rectP(x - w, y, x + w, y + L(8.0)))
            add(Path().polygon(listOf(Vec(x - w, y + L(8.0)), Vec(x + w, y + L(8.0)), Vec(x + w * 0.5, y + L(24.0)), Vec(x - w * 0.5, y + L(24.0)))))
            windows.append(rectP(x - L(2.2), y + L(40.0), x + L(2.2), y + L(54.0)))
        }
        add(Path().polygon(listOf(Vec(x - w * 0.62, capBase), Vec(x, top), Vec(x + w * 0.62, capBase))))
        alem(x, top, L(24.0))
        minarets += Minaret(x, base, top, w, capBase, bals)
    }
    val xs = listOf(X(-560.0), X(260.0))
    return Camii(
        body, parts, domes, minarets, windows, arcade, xs.min(), xs.max(),
        minarets.minOf { it.top } - L(24.0), ground, hallTop, min(X(-250.0), X(250.0)), max(X(-250.0), X(250.0)), s,
    )
}

/** How the mosque is painted in a given light. */
private class Look(
    val stoneA: Int, val stoneB: Int, val stoneD: Double,
    val shadow: Int, val shadowD: Double,
    val dome: Int, val domeD: Double,
    val dark: Int, val darkD: Double,
    val ink: Int, val inkA: Float,
    val lit: Int? = null,
    val fade: ((Double, Double) -> Double)? = null,
    val snow: Boolean = false,
    val soft: Double = 1.0,
)

// ============================================================== paintings

private class Painting(val ctx: RenderContext, val mood: SuluboyaArt.Mood) {
    val p = SuluboyaArt.pigments(ctx.palette)
    val u = ctx.u
    val W = ctx.w
    val H = ctx.h
    val rnd = ctx.random(4242)
    val sheet = WetSheet(ctx, p.paper)
    val b = SceneBuilder(ctx.width, ctx.height)
    val hand = Hand(b, ctx, ctx.seed)
    val pad = 60 * u

    val water = mood in setOf(Mood.DAWN, Mood.SUNSET, Mood.NIGHT, Mood.MIST, Mood.KANDIL)
    val night = mood == Mood.NIGHT || mood == Mood.KANDIL
    val horizon = ctx.y(if (water) 0.665 else 0.70)
    val hill = when (mood) { Mood.DAWN -> 150 * u; Mood.SUNSET -> 80 * u; else -> 0.0 }
    val ground = horizon - hill
    val flip = ctx.seed % 2 == 0 && mood != Mood.DAWN
    val mosqueX = ctx.x(if (flip) 0.40 else 0.60)
    val scale = u * (if (ctx.safeW > ctx.safeH * 0.9) 1.25 else 1.22)
    val mosque = camii(mosqueX, ground, scale, if (mood == Mood.SPRING || mood == Mood.RAIN) 2 else 4, flip, Random(ctx.seed + 9))
    val reflections = ArrayList<Pair<Path, Pair<Int, Double>>>()

    fun paint(): Scene {
        sky()
        distance()
        paintMosque()
        foreground()
        b.items.add(0, sheet.toRaster())
        marks()
        return b.build()
    }

    // ---------------------------------------------------------------- sky

    private fun skyRect(bottom: Double) = rectP(-pad, -pad, W + pad, bottom)

    private fun sky() {
        val top = 0.0
        val hz = horizon
        fun down(y: Double) = ((y - top) / (hz - top)).coerceIn(0.0, 1.0)
        val bloomsAt = { n: Int, y0: Double, y1: Double ->
            (0 until n).map { Bloom(rnd.nextDouble() * W, y0 + rnd.nextDouble() * (y1 - y0), (40 + rnd.nextDouble() * 70) * u, 0.6 + rnd.nextDouble() * 0.4) }
        }
        when (mood) {
            Mood.DAWN -> {
                // Warm under-wash first, rose bleeding into ochre near the water.
                sheet.wash(rectP(-pad, hz - (hz - top) * 0.62, W + pad, hz + 30 * u), p.glow, p.rose, 0.55, soft = 40.0, hard = 0.32, rag = 1.0,
                    edge = 0.3, gran = 0.15, mix = { x, _ -> smooth(0.0, W, x) * 0.9 }, grade = { _, y -> 0.25 + 0.9 * smooth(0.35, 1.0, down(y)) })
                // Then cerulean dropped in from the top while it is still damp.
                sheet.wash(skyRect(hz - (hz - top) * 0.28), p.sky, p.deep, 0.5, soft = 45.0, hard = 0.35, rag = 1.0,
                    edge = 0.45, gran = 0.45, mix = { _, y -> (1 - down(y)) * 0.35 }, grade = { _, y -> 1.1 - 0.8 * down(y) },
                    blooms = bloomsAt(2, hz * 0.45, hz * 0.62))
                // Soft clouds lifted with a tissue.
                cloudLifts(3, top + (hz - top) * 0.3, top + (hz - top) * 0.62, 0.45)
                sheet.wash(cloudBand(hz - (hz - top) * 0.33, 34 * u), p.rose, p.shade, 0.22, soft = 16.0, hard = 0.3, rag = 1.0, edge = 0.8, gran = 0.3)
            }
            Mood.SUNSET -> {
                sheet.wash(skyRect(hz + 30 * u), p.glow, p.rose, 0.9, soft = 30.0, hard = 0.3, rag = 0.8, edge = 0.25, gran = 0.15,
                    mix = { _, y -> 1 - smooth(0.55, 1.0, down(y)) }, grade = { _, y -> 0.6 + 0.6 * down(y) })
                sheet.wash(skyRect(hz - (hz - top) * 0.45), p.shade, p.deep, 0.55, soft = 50.0, hard = 0.38, rag = 1.1, edge = 0.5, gran = 0.5,
                    mix = { _, y -> 1 - down(y) }, grade = { _, y -> 1.15 - 1.1 * down(y) }, blooms = bloomsAt(2, hz * 0.3, hz * 0.55))
                // Long glowing clouds, painted wet-on-dry.
                for (k in 0 until 2) {
                    val y = top + (hz - top) * (0.5 + k * 0.17)
                    sheet.wash(cloudBand(y, (34 + k * 14) * u), p.rose, p.earth, 0.45, soft = 10.0, hard = 0.16, rag = 1.0, edge = 1.0, gran = 0.25)
                }
                // The sun, lifted out of the wet wash.
                val sx = if (flip) ctx.x(0.16) else ctx.x(0.84)
                sheet.lift(Path().circle(sx, hz - 520 * u, 170 * u), 0.45, soft = 50.0, hard = 0.45, rag = 0.8)
                sheet.lift(Path().circle(sx, hz - 520 * u, 58 * u), 0.95, soft = 3.0, hard = 0.15, rag = 0.3)
                sheet.wash(Path().circle(sx, hz - 520 * u, 60 * u), p.glow, p.glow, 0.3, soft = 3.0, edge = 1.4, edgeWidth = 3.0, gran = 0.0)
                sunX = sx
            }
            Mood.NIGHT, Mood.KANDIL -> {
                val low = if (mood == Mood.KANDIL) p.rose else p.glow
                sheet.wash(rectP(-pad, hz - (hz - top) * 0.45, W + pad, hz + 30 * u), low, p.glow, 0.42, soft = 40.0, hard = 0.35, rag = 1.0,
                    edge = 0.2, gran = 0.1, grade = { _, y -> smooth(0.4, 1.0, down(y)) })
                sheet.wash(skyRect(hz + 10 * u), p.deep, p.sky, 1.35, soft = 40.0, hard = 0.38, rag = 1.0, edge = 0.55, gran = 0.6, flowVar = 0.4,
                    mix = { _, y -> smooth(0.35, 1.0, down(y)) * 0.9 }, grade = { _, y -> 1.3 - 0.75 * smooth(0.2, 1.0, down(y)) },
                    blooms = bloomsAt(3, hz * 0.15, hz * 0.7))
                // A second glaze to deepen the zenith, with its own wandering edge.
                sheet.wash(skyRect(top + (hz - top) * 0.42), p.deep, p.shade, 0.55, soft = 35.0, hard = 0.25, rag = 1.2, edge = 0.9, gran = 0.6)
                // Crescent moon lifted back to the paper.
                val mx = if (flip) ctx.x(0.76) else ctx.x(0.24)
                if (mood == Mood.KANDIL) return
                val my = ctx.y(0.17)
                moon = Vec(mx, my)
                sheet.lift(Path().circle(mx, my, 150 * u), 0.35, soft = 40.0, hard = 0.4, rag = 0.8)
                sheet.lift(Common.crescent(mx, my, 62 * u, rotation = -0.6), 0.97, soft = 1.0, hard = 0.1, rag = 0.25)
                sheet.wash(Common.crescent(mx, my, 62 * u, rotation = -0.6), p.glow, p.glow, 0.18, soft = 1.0, edge = 1.4, gran = 0.0, frame = false)
            }
            Mood.MIST -> {
                sheet.wash(skyRect(hz + 60 * u), p.sky, p.shade, 0.32, soft = 60.0, hard = 0.45, rag = 1.2, edge = 0.25, gran = 0.4,
                    mix = { _, y -> down(y) }, grade = { _, y -> 0.85 - 0.75 * down(y) })
                sheet.wash(cloudBand(top + (hz - top) * 0.3, 60 * u), p.glow, p.rose, 0.2, soft = 30.0, hard = 0.4, rag = 1.0, edge = 0.3)
            }
            Mood.RAIN -> {
                sheet.wash(skyRect(hz + 40 * u), p.shade, p.sky, 0.75, soft = 40.0, hard = 0.36, rag = 1.1, edge = 0.5, gran = 0.55,
                    mix = { _, y -> down(y) * 0.8 }, grade = { _, y -> 1.1 - 0.5 * down(y) }, blooms = bloomsAt(1, hz * 0.2, hz * 0.8))
                // Heavy cloud dropped in wet, with a warm glow below from the city lights.
                for (k in 0 until 3) {
                    val cy = top + (hz - top) * (0.1 + k * 0.22)
                    sheet.wash(cloudBand(cy, (130 - k * 25) * u), p.deep, p.shade, 0.5 - k * 0.08,
                        soft = 45.0, hard = 0.36, rag = 1.2, edge = 0.6, gran = 0.4)
                }
                sheet.wash(rectP(-pad, hz - 120 * u, W + pad, hz + 10 * u), p.glow, p.rose, 0.35, soft = 30.0, hard = 0.4, rag = 1.0, edge = 0.3)
            }
            Mood.WINTER -> {
                sheet.wash(skyRect(hz + 30 * u), p.shade, p.rose, 0.5, soft = 50.0, hard = 0.38, rag = 1.0, edge = 0.45, gran = 0.55,
                    mix = { _, y -> smooth(0.5, 1.0, down(y)) * 0.7 }, grade = { _, y -> 1.0 - 0.55 * down(y) }, blooms = bloomsAt(2, hz * 0.2, hz * 0.6))
                sheet.wash(cloudBand(top + (hz - top) * 0.22, 150 * u), p.deep, p.shade, 0.22, soft = 60.0, hard = 0.42, rag = 1.2, edge = 0.4, gran = 0.5)
            }
            Mood.SPRING -> {
                sheet.wash(skyRect(hz + 20 * u), p.sky, p.sky, 0.6, soft = 40.0, hard = 0.3, rag = 1.0, edge = 0.6, gran = 0.45,
                    grade = { _, y -> 1.05 - 0.75 * down(y) }, blooms = bloomsAt(2, hz * 0.3, hz * 0.6))
                cloudLifts(4, top + (hz - top) * 0.15, top + (hz - top) * 0.55, 0.8)
                sheet.wash(rectP(-pad, hz - 520 * u, W + pad, hz + 10 * u), p.glow, p.glow, 0.22, soft = 30.0, hard = 0.4, rag = 1.0, edge = 0.2)
            }
        }
    }

    var sunX = 0.0
    var moon: Vec? = null

    /** A long, irregular horizontal band of cloud centred on [y]. */
    private fun cloudBand(y: Double, thick: Double): Path {
        val n = Noise(ctx.seed + (y / u).toInt())
        val top = ArrayList<Vec>(); val bot = ArrayList<Vec>()
        var x = -pad
        val left = -pad + rnd.nextDouble() * W * 0.3
        val right = W + pad - rnd.nextDouble() * W * 0.3
        x = left
        while (x <= right) {
            val t = (x - left) / (right - left)
            val env = sin(t * PI).pow(0.6)
            val a = thick * env * (0.4 + 1.2 * n.fbm(x / (160 * u), 0.3, 3))
            val c = y + thick * 0.8 * (n.at(x / (400 * u), 7.7) - 0.5)
            top += Vec(x, c - a * 0.8)
            bot += Vec(x, c + a * 0.45)
            x += 12 * u
        }
        return Path().polygon(top + bot.reversed())
    }

    /** A heap of cloud: overlapping rounded masses, wider than tall. */
    private fun cloudMass(cx: Double, cy: Double, r: Double): Path {
        val p = Path()
        val k = 5 + rnd.nextInt(4)
        repeat(k) { j ->
            val rr = r * (0.35 + rnd.nextDouble() * 0.4)
            p.circle(cx + (j - k / 2.0) * r * 0.45 + (rnd.nextDouble() - 0.5) * r * 0.3, cy + (rnd.nextDouble() - 0.5) * r * 0.35, rr)
        }
        return p.transformed { x, y -> x to (cy + (y - cy) * 0.6).toFloat() }
    }

    private fun cloudLifts(n: Int, y0: Double, y1: Double, amount: Double) {
        repeat(n) {
            val cx = rnd.nextDouble() * W
            val cy = y0 + rnd.nextDouble() * (y1 - y0)
            val p = Path()
            val k = 3 + rnd.nextInt(3)
            repeat(k) { j ->
                val r = (35 + rnd.nextDouble() * 45) * u
                p.circle(cx + (j - k / 2.0) * r * 1.1, cy + (rnd.nextDouble() - 0.5) * r * 0.6, r)
            }
            sheet.lift(p, amount * (0.6 + rnd.nextDouble() * 0.4), soft = 22.0, hard = 0.35, rag = 1.0)
        }
    }

    // ------------------------------------------------------- distance

    /** The far shore: a hill of houses and a smaller mosque, paler with distance. */
    private fun distance() {
        val farCol = when (mood) {
            Mood.SUNSET -> p.shade
            Mood.NIGHT, Mood.KANDIL -> p.deep
            Mood.MIST -> p.sky
            else -> Colors.mix(p.shade, p.sky, 0.5f)
        }
        val farD = when (mood) { Mood.MIST -> 0.14; Mood.NIGHT, Mood.KANDIL -> 0.75; Mood.SUNSET -> 0.5; Mood.WINTER -> 0.3; else -> 0.34 }
        // Far hill line with a scatter of roofs and, further off, another mosque.
        val n = Noise(ctx.seed + 333)
        val farBase = horizon
        val pts = ArrayList<Vec>()
        var x = -pad
        while (x <= W + pad) {
            val hgt = (34 + 50 * n.fbm(x / (300 * u), 0.5, 3)) * u
            pts += Vec(x, farBase - hgt)
            x += 8 * u
        }
        val far = Path().polygon(pts + listOf(Vec(W + pad, farBase + 6 * u), Vec(-pad, farBase + 6 * u)))
        val farX = if (flip) ctx.x(0.88) else ctx.x(0.12)
        val small = NightArt.mosque(ctx, NightArt.Architecture.OTTOMAN, 0.0, 0.0, Random(ctx.seed + 4)).first
        val k = 0.36
        val fy = farBase - 60 * u
        far.append(small.transformed { px, py -> (farX + px * k).toFloat() to (fy + py * k).toFloat() })
        val houses = Path()
        val hr = Random(ctx.seed + 77)
        x = -pad
        while (x < W + pad) {
            val ww = (12 + hr.nextDouble() * 26) * u
            val hh = (10 + hr.nextDouble() * 22) * u
            val base = farBase - (34 + 50 * n.fbm(x / (300 * u), 0.5, 3)) * u + 10 * u
            houses.rect(x, base - hh, x + ww, base + 4 * u)
            if (hr.nextDouble() < 0.5) houses.polygon(listOf(Vec(x - 2 * u, base - hh), Vec(x + ww / 2, base - hh - 8 * u), Vec(x + ww + 2 * u, base - hh)))
            x += ww + hr.nextDouble() * 18 * u
        }
        far.append(houses)
        val fade: ((Double, Double) -> Double)? = if (mood == Mood.MIST) { _, y -> 1 - smooth(farBase - 60 * u, farBase, y) * 0.8 } else null
        sheet.wash(far, farCol, p.shade, farD, soft = 3.0, hard = 0.14, rag = 0.9, edge = 0.9, gran = 0.4, miss = 0.06, grade = fade)
        reflections += far to (farCol to farD * 0.5)

        // The near hill the mosque stands on, with houses stepping down to the water.
        if (hill > 0) {
            val hn = Noise(ctx.seed + 444)
            val hp = ArrayList<Vec>()
            x = -pad
            while (x <= W + pad) {
                val t = (x - mosqueX) / (720 * u)
                val y = horizon - hill * exp(-t * t * 1.4) * (0.9 + 0.2 * hn.at(x / (90 * u), 1.0)) - 6 * u
                hp += Vec(x, min(y, horizon - 4 * u))
                x += 8 * u
            }
            val hillP = Path().polygon(hp + listOf(Vec(W + pad, horizon + 4 * u), Vec(-pad, horizon + 4 * u)))
            val houses2 = Path()
            x = -pad
            while (x < W + pad) {
                val t = (x - mosqueX) / (720 * u)
                val y = horizon - hill * exp(-t * t * 1.4)
                val ww = (16 + hr.nextDouble() * 22) * u
                val hh = (14 + hr.nextDouble() * 18) * u
                if (x < mosque.left - 10 * u || x > mosque.right + 10 * u) {
                    houses2.rect(x, y - hh, x + ww, y + 8 * u)
                    houses2.polygon(listOf(Vec(x - 3 * u, y - hh), Vec(x + ww / 2, y - hh - 9 * u), Vec(x + ww + 3 * u, y - hh)))
                }
                x += ww + hr.nextDouble() * 10 * u
            }
            hillP.append(houses2)
            val (hc, hd) = if (mood == Mood.SUNSET) p.deep to 0.7 else Colors.mix(p.green, p.shade, 0.5f) to 0.5
            sheet.wash(hillP, hc, p.earth, hd, soft = 2.5, hard = 0.12, rag = 0.9, edge = 1.0, gran = 0.45, miss = 0.08,
                mix = { _, y -> smooth(horizon - hill, horizon, y) * 0.6 })
            // Windows of the houses as tiny dabs.
            val dabs = Path()
            x = -pad
            while (x < W + pad) {
                val t = (x - mosqueX) / (720 * u)
                val y = horizon - hill * exp(-t * t * 1.4) + hr.nextDouble() * 18 * u
                if (x < mosque.left || x > mosque.right) dabs.rect(x, y - 4 * u, x + 4 * u, y + 2 * u)
                x += (10 + hr.nextDouble() * 22) * u
            }
            sheet.wash(dabs, p.ink, p.deep, 0.6, soft = 0.5, hard = 0.15, rag = 0.3, edge = 0.5, gran = 0.2, miss = 0.3)
            reflections += hillP to (hc to hd * 0.6)
            hand.pencil(listOf(hillP), 0.12f)
        }
    }

    // ---------------------------------------------------------- mosque

    private fun look(): Look = when (mood) {
        Mood.DAWN -> Look(Colors.mix(p.glow, p.paper, 0.35f), p.rose, 0.24, p.shade, 0.42, Colors.mix(p.sky, p.shade, 0.5f), 0.45, p.shade, 0.9, p.ink, 0.78f)
        Mood.SUNSET -> Look(p.earth, p.shade, 0.75, p.deep, 0.75, p.deep, 0.55, p.ink, 0.9, p.ink, 0.85f, lit = p.glow)
        Mood.NIGHT -> Look(p.shade, p.deep, 0.9, p.deep, 0.75, p.deep, 0.5, p.ink, 0.9, p.ink, 0.85f, lit = p.glow)
        Mood.KANDIL -> Look(p.shade, p.deep, 0.85, p.deep, 0.7, p.deep, 0.5, p.ink, 0.9, p.ink, 0.85f, lit = p.glow)
        Mood.MIST -> Look(p.shade, p.sky, 0.3, p.shade, 0.35, p.shade, 0.25, p.shade, 0.5, p.ink, 0.5f,
            fade = { _, y -> 1 - 0.92 * smooth(mosque.top + (ground - mosque.top) * 0.35, ground - 20 * u, y) }, soft = 2.0)
        Mood.RAIN -> Look(p.shade, p.sky, 0.55, p.deep, 0.55, p.deep, 0.4, p.ink, 0.8, p.ink, 0.75f, lit = p.glow)
        Mood.WINTER -> Look(Colors.mix(p.glow, p.paper, 0.4f), p.shade, 0.3, p.shade, 0.6, p.shade, 0.45, p.deep, 0.8, p.ink, 0.8f, snow = true)
        Mood.SPRING -> Look(Colors.mix(p.glow, p.paper, 0.2f), p.rose, 0.32, p.shade, 0.5, Colors.mix(p.sky, p.shade, 0.5f), 0.45, p.shade, 0.85, p.ink, 0.8f)
    }

    private fun paintMosque() {
        val c = mosque
        val lk = look()
        val s = c.s
        val fade = lk.fade
        // Underdrawing first, as the painter did.
        hand.pencil(c.parts, if (mood == Mood.MIST) 0.3f else 0.24f)
        hand.guides(listOf(
            Vec(c.left - 80 * u, c.ground + 2 * u) to Vec(c.right + 90 * u, c.ground - 1 * u),
            Vec(mosqueX, c.hallTop + 40 * u) to Vec(mosqueX + 2 * u, c.hallTop - 330 * s),
            Vec(c.hallLeft - 30 * u, c.hallTop) to Vec(c.hallRight + 50 * u, c.hallTop + 2 * u),
        ))

        // 1. Local colour of the stone, laid a little off the drawing.
        val off = Vec(3.0 * u, -2.0 * u)
        sheet.wash(c.body.moved(off.x, off.y), lk.stoneA, lk.stoneB, lk.stoneD, soft = 1.6 * lk.soft, hard = 0.1, rag = 0.7, edge = 1.0,
            edgeWidth = 4.0, gran = 0.3, miss = 0.1, mix = { x, _ -> smooth(c.left, c.right, x) }, grade = fade)

        // 2. Shadows: the far side of every dome and minaret, the foot of the walls.
        val shade = Path()
        val lightFromLeft = !(mood == Mood.SUNSET && sunX > mosqueX)
        val sd = if (lightFromLeft) 1.0 else -1.0
        for (m in c.minarets) {
            shade.append(rectP(m.x + sd * m.w * 0.08, m.capBase, m.x + sd * m.w, m.base))
            shade.append(Path().polygon(listOf(Vec(m.x, m.top), Vec(m.x + sd * m.w * 0.62, m.capBase), Vec(m.x, m.capBase))))
        }
        shade.append(rectP(c.left - 10 * u, c.ground - (c.ground - c.hallTop) * 0.3, c.right + 10 * u, c.ground + 2 * u))
        shade.append(if (sd > 0) rectP(c.hallRight - 70 * s, c.hallTop, c.hallRight, c.ground) else rectP(c.hallLeft, c.hallTop, c.hallLeft + 70 * s, c.ground))
        sheet.wash(shade.moved(1.5 * u, 0.0), lk.shadow, lk.shadow, lk.shadowD, soft = 1.6 * lk.soft, hard = 0.1, rag = 0.8, edge = 1.1,
            edgeWidth = 3.5, gran = 0.45, miss = 0.08, clip = c.body, grade = fade)

        // The turned-away side of each big dome, kept inside that dome.
        for (d in c.domes) if (d.r > 60 * s) {
            sheet.wash(Path().circle(d.cx + sd * d.r * 0.7, d.base - d.h * 0.2, d.r * 0.9), lk.shadow, lk.shadow, lk.shadowD * 0.8,
                soft = 2.0 * lk.soft, hard = 0.12, rag = 0.8, edge = 1.0, edgeWidth = 3.5, gran = 0.4, clip = d.path, grade = fade)
        }

        // 3. The lead domes, each its own small wash, lighter where the light falls.
        for ((i, d) in c.domes.withIndex()) {
            val gx = d.cx - sd * d.r * 0.45; val gy = d.base - d.h * 0.75
            sheet.wash(d.path.moved((i % 3 - 1) * 1.2 * u, 0.0), lk.dome, p.sky, lk.domeD, soft = 1.2 * lk.soft, hard = 0.1, rag = 0.6,
                edge = 1.2, edgeWidth = 3.0, gran = 0.5, mix = { _, _ -> 0.3 },
                grade = { x, y ->
                    val dd = Vec(x - gx, y - gy).length / (d.r * 1.3)
                    (0.35 + 0.9 * smooth(0.1, 1.0, dd)) * (fade?.invoke(x, y) ?: 1.0)
                })
        }

        // 4. Darks: windows and the arcade, not every one filled.
        if (lk.lit == null) {
            sheet.wash(c.windows, lk.dark, p.deep, lk.darkD, soft = 0.6, hard = 0.12, rag = 0.5, edge = 0.8, edgeWidth = 2.0, gran = 0.3, miss = 0.18, grade = fade)
        }
        sheet.wash(c.arcade, lk.dark, p.deep, lk.darkD * 0.85, soft = 0.8, hard = 0.12, rag = 0.6, edge = 0.8, edgeWidth = 2.0, gran = 0.3, miss = 0.12, grade = fade)
        // Dry-brush at the foot of the walls.
        sheet.wash(rectP(c.left - 30 * u, c.ground - 30 * u, c.right + 40 * u, c.ground + 4 * u), lk.dark, p.earth, lk.darkD * 0.7,
            soft = 3.0, hard = 0.14, rag = 1.0, edge = 0.6, gran = 0.6, dry = 0.42, grade = fade)

        // 5. Snow left as paper on the domes and ledges.
        if (lk.snow) {
            for (d in c.domes) {
                sheet.lift(Shapes.hemiDome(d.cx, d.base - d.h * 0.3, d.r * 1.05, d.h * 0.72), 0.92, soft = 1.0, hard = 0.1, rag = 0.8, clip = d.path)
            }
            for (m in c.minarets) {
                for (y in m.balconies) sheet.lift(rectP(m.x - m.w, y - 2 * u, m.x + m.w, y + 3 * u), 0.9, soft = 0.6, rag = 0.5)
                sheet.lift(Path().polygon(listOf(Vec(m.x - m.w * 0.62, m.capBase - 6 * u), Vec(m.x, m.top), Vec(m.x - m.w * 0.05, m.capBase - 4 * u))), 0.85, soft = 0.6)
            }
            sheet.lift(rectP(c.hallLeft, c.hallTop - 2 * u, c.hallRight, c.hallTop + 5 * u), 0.9, soft = 0.8, rag = 0.6)
        }

        // 6. Lit windows: lifted back to the paper, a touch of warm glaze.
        lk.lit?.let { lit ->
            sheet.lift(c.windows, 0.93, soft = 0.4, hard = 0.12, rag = 0.3)
            sheet.wash(c.windows, lit, p.rose, 0.32, soft = 0.8, hard = 0.2, edge = 1.0, edgeWidth = 1.5, gran = 0.0, frame = false)
        }

        reflections += c.body to (lk.shadow to (lk.stoneD + lk.shadowD) * 0.45)
    }

    // ------------------------------------------------------ foreground

    private fun foreground() {
        if (water) waterPlane() else when (mood) {
            Mood.RAIN -> street()
            Mood.WINTER -> snowField()
            Mood.SPRING -> garden()
            else -> {}
        }
    }

    private fun waterPlane() {
        val top = horizon
        val streak = Noise(ctx.seed + 55)
        val (wc, wc2, wd) = when (mood) {
            Mood.DAWN -> Triple(p.sky, p.rose, 0.45)
            Mood.SUNSET -> Triple(p.glow, p.deep, 0.75)
            Mood.NIGHT -> Triple(p.deep, p.sky, 1.0)
            Mood.KANDIL -> Triple(p.deep, p.rose, 1.0)
            else -> Triple(p.sky, p.shade, 0.2)
        }
        sheet.wash(rectP(-pad, top - 2 * u, W + pad, H + pad), wc, wc2, wd, soft = 8.0, hard = 0.2, rag = 0.6, edge = 0.6, gran = 0.5,
            mix = { _, y -> if (mood == Mood.SUNSET) smooth(top, H, y) else smooth(top, top + (H - top) * 0.5, y) * 0.5 },
            grade = { x, y -> (0.55 + 0.6 * smooth(top, H, y)) * (0.7 + 0.6 * ripple(streak, x, y)) })
        // Reflections: everything above, upside down, broken by the ripples.
        val rip = Noise(ctx.seed + 66)
        for ((path, cd) in reflections) {
            val lines = Brush.flatten(path, 3 * u)
            val mirrored = Path()
            for (l in lines) mirrored.polygon(l.map { v ->
                val y = 2 * top - v.y
                Vec(v.x + (rip.at(v.x / (200 * u), y / (10 * u)) - 0.5) * 16 * u, y)
            })
            val depth = 0.8 * (ground - mosque.top)
            sheet.wash(mirrored, cd.first, p.deep, cd.second * (if (mood == Mood.MIST) 0.5 else 1.0), soft = 3.0, hard = 0.2, rag = 1.0, edge = 0.5, gran = 0.4,
                clip = rectP(-pad, top, W + pad, H + pad),
                grade = { x, y ->
                    val s = ripple(streak, x * 3 + 917 * u, y * 0.6)
                    (1 - smooth(top, top + depth, y) * 0.75) * smooth(0.3, 0.5, s)
                })
        }
        // Light on the water: lifted streaks and the moon or sun path.
        val lr = Random(ctx.seed + 88)
        val lifts = Path()
        repeat(14) {
            val y = top + (H - top) * lr.nextDouble().pow(1.4)
            val x = lr.nextDouble() * W
            val len = (60 + lr.nextDouble() * 220) * u
            lifts.rect(x - len / 2, y, x + len / 2, y + (2 + lr.nextDouble() * 3) * u)
        }
        sheet.lift(lifts, 0.55, soft = 1.0, hard = 0.15, rag = 0.8)
        val pathX = moon?.x ?: if (mood == Mood.SUNSET) sunX else -1.0
        if (pathX > 0) {
            val gl = Path()
            repeat(13) {
                val t = (it + lr.nextDouble() * 0.6) / 13.0
                val y = top + 8 * u + (H - top) * 0.55 * t.pow(1.3)
                val half = (14 + 60 * t + lr.nextDouble() * 50) * u
                val x = pathX + (lr.nextDouble() - 0.5) * 40 * u
                gl.rect(x - half, y, x + half, y + (4 + 6 * t + lr.nextDouble() * 4) * u)
            }
            sheet.lift(gl, 0.75, soft = 0.8, hard = 0.15, rag = 0.7)
        }
        if (mood == Mood.KANDIL) {
            // Halos around every lit balcony, lifted out and glazed warm.
            for (m in mosque.minarets) for (y in m.balconies) {
                sheet.lift(Path().circle(m.x, y, 46 * u), 0.6, soft = 16.0, hard = 0.4, rag = 0.6)
                sheet.wash(Path().circle(m.x, y, 34 * u), p.glow, p.rose, 0.4, soft = 12.0, hard = 0.4, rag = 0.6, edge = 0.4, gran = 0.0, frame = false)
            }
        }
        // Kandil: the lights of the minarets fall on the water.
        if (mood == Mood.KANDIL) {
            val gl = Path()
            for (m in mosque.minarets) {
                repeat(10) {
                    val y = top + 8 * u + it * (14 + lr.nextDouble() * 10) * u
                    val half = (6 + lr.nextDouble() * 18) * u
                    gl.rect(m.x - half, y, m.x + half, y + 3 * u)
                }
            }
            sheet.lift(gl, 0.8, soft = 0.6, hard = 0.15, rag = 0.6)
            sheet.wash(gl, p.glow, p.glow, 0.4, soft = 1.0, edge = 0.5, gran = 0.0)
        }
        // The near water darker, a last wash pulled across the bottom.
        sheet.wash(rectP(-pad, top + (H - top) * 0.55, W + pad, H + pad), wc2, p.deep, wd * 0.35, soft = 20.0, hard = 0.3, rag = 1.0, edge = 0.7, gran = 0.5)
        // Shoreline: a dark wet line where the land meets the water.
        sheet.wash(rectP(-pad, top - 5 * u, W + pad, top + 3 * u), p.ink, p.deep, if (mood == Mood.MIST) 0.15 else 0.5, soft = 1.5, hard = 0.12, rag = 1.0, edge = 0.6, gran = 0.4, miss = 0.15)
    }

    /** Horizontal ripple pattern: long in x, short in y, and never evenly spaced. */
    private fun ripple(n: Noise, x: Double, y: Double): Double {
        val warp = 26 * u * (n.at(x / (330 * u) + 5.0, y / (90 * u)) - 0.5)
        val k = 1 + 0.8 * smooth(horizon, H, y)
        return n.fbm(x / (240 * u), (y + warp) / (11 * u * k), 3)
    }

    private fun street() {
        val top = horizon
        val streak = Noise(ctx.seed + 55)
        // Wet cobbles: a dark wash that is almost a mirror.
        sheet.wash(rectP(-pad, top - 4 * u, W + pad, H + pad), p.shade, p.deep, 0.75, soft = 6.0, hard = 0.2, rag = 0.8, edge = 0.6, gran = 0.6,
            mix = { _, y -> smooth(top, H, y) }, grade = { x, y -> 0.7 + 0.5 * streak.fbm(x / (40 * u), y / (160 * u), 2) })
        // Reflections stretch straight down on a wet street.
        val c = mosque
        val down = Path()
        for (m in c.minarets) down.rect(m.x - m.w * 0.5, top, m.x + m.w * 0.5, top + (H - top) * 0.55)
        down.rect(c.hallLeft, top, c.hallRight, top + (H - top) * 0.35)
        sheet.wash(down, p.deep, p.shade, 0.45, soft = 6.0, hard = 0.3, rag = 1.0, edge = 0.4, gran = 0.4,
            grade = { x, y -> (1 - smooth(top, H, y)) * (0.5 + streak.at(x / (8 * u), y / (60 * u))) })
        // Street lamps and their long lifted reflections.
        val lr = Random(ctx.seed + 17)
        val lamps = listOf(ctx.x(0.08), ctx.x(0.3), ctx.x(0.88), ctx.x(1.0) + 60 * u, ctx.x(0.0) - 70 * u)
        for (lx in lamps) {
            val ly = top - 150 * u
            sheet.lift(rectP(lx - 7 * u, top + 10 * u, lx + 7 * u, top + (H - top) * (0.5 + lr.nextDouble() * 0.3)), 0.8, soft = 4.0, hard = 0.3, rag = 1.2)
            sheet.wash(rectP(lx - 7 * u, top + 10 * u, lx + 7 * u, top + (H - top) * 0.55), p.glow, p.rose, 0.4, soft = 4.0, hard = 0.3, rag = 1.0, edge = 0.4, gran = 0.0)
            sheet.lift(Path().circle(lx, ly, 34 * u), 0.6, soft = 12.0, hard = 0.4, rag = 0.6)
            sheet.wash(Path().circle(lx, ly, 26 * u), p.glow, p.rose, 0.45, soft = 10.0, hard = 0.35, rag = 0.6, edge = 0.3, gran = 0.0)
            lampPosts += Vec(lx, ly)
        }
        // Figures under umbrellas, a few drops of dark paint each.
        figures(listOf(ctx.x(0.22) to top + (H - top) * 0.16, ctx.x(0.7) to top + (H - top) * 0.3), umbrella = true)
    }

    val lampPosts = ArrayList<Vec>()
    val figurePts = ArrayList<Pair<Vec, Double>>()

    private fun figures(at: List<Pair<Double, Double>>, umbrella: Boolean) {
        for ((i, xy) in at.withIndex()) {
            val (x, y) = xy
            val s = (1.0 + (y - horizon) / (H - horizon) * 1.2) * u
            val body = Path().polygon(listOf(Vec(x - 9 * s, y - 52 * s), Vec(x + 9 * s, y - 52 * s), Vec(x + 13 * s, y), Vec(x + 3 * s, y + 3 * s), Vec(x - 12 * s, y)))
            body.circle(x, y - 58 * s, 7 * s)
            val col = if (i % 2 == 0) p.ink else p.rose
            sheet.wash(body, col, p.deep, 0.9, soft = 1.0, hard = 0.15, rag = 0.8, edge = 0.8, edgeWidth = 2.0, gran = 0.3)
            if (umbrella) {
                val um = Shapes.hemiDome(x, y - 62 * s, 30 * s, 20 * s)
                sheet.wash(um, if (i % 2 == 0) p.rose else p.ink, p.deep, 0.95, soft = 1.0, hard = 0.12, rag = 0.6, edge = 1.0, edgeWidth = 2.0, gran = 0.2)
            }
            sheet.wash(rectP(x - 4 * s, y + 2 * s, x + 4 * s, y + 70 * s), p.ink, p.ink, 0.35, soft = 2.0, hard = 0.3, rag = 1.0, edge = 0.3, gran = 0.4,
                grade = { _, yy -> 1 - smooth(y, y + 70 * s, yy) })
            figurePts += Vec(x, y) to s
        }
    }

    private fun snowField() {
        val top = horizon
        // The snow is the paper; only its shadows are painted, in blue-violet.
        val n = Noise(ctx.seed + 31)
        val drift = ArrayList<Vec>()
        var x = -pad
        while (x <= W + pad) {
            drift += Vec(x, top + 30 * u + 50 * u * n.fbm(x / (260 * u), 0.2, 3))
            x += 10 * u
        }
        sheet.wash(Path().polygon(drift + listOf(Vec(W + pad, H + pad), Vec(-pad, H + pad))), p.sky, p.shade, 0.32, soft = 10.0, hard = 0.25,
            rag = 1.0, edge = 0.8, gran = 0.5, grade = { xx, y -> 0.3 + 0.8 * n.fbm(xx / (300 * u), y / (60 * u), 3) })
        sheet.wash(rectP(-pad, top - 6 * u, W + pad, top + 18 * u), p.shade, p.deep, 0.4, soft = 4.0, hard = 0.2, rag = 1.0, edge = 0.8, gran = 0.4)
        // Long blue shadows lying across the snow.
        val sr = Random(ctx.seed + 8)
        repeat(5) {
            val y = top + (H - top) * (0.12 + 0.75 * sr.nextDouble())
            val x = sr.nextDouble() * W
            sheet.wash(cloudBand(y, (10 + sr.nextDouble() * 16) * u).moved(x - W / 2, 0.0), p.sky, p.shade, 0.3, soft = 6.0, hard = 0.2, rag = 1.0, edge = 0.9, gran = 0.5)
        }
        figures(listOf(ctx.x(0.55) to top + (H - top) * 0.38), umbrella = false)
    }

    private fun garden() {
        val top = horizon
        val n = Noise(ctx.seed + 32)
        // Grass, wet-in-wet greens and ochre.
        sheet.wash(rectP(-pad, top - 8 * u, W + pad, H + pad), p.green, p.glow, 0.55, soft = 12.0, hard = 0.25, rag = 1.0, edge = 0.6, gran = 0.45,
            mix = { x, y -> 0.5 * n.fbm(x / (200 * u), y / (100 * u), 2) }, grade = { _, y -> 0.6 + 0.5 * smooth(top, H, y) })
        // Erguvan (Judas tree) blossom beside the mosque: dabs of rose.
        val tx = if (flip) ctx.x(0.88) else ctx.x(0.1)
        val tr = Random(ctx.seed + 3)
        val blossom = Path()
        repeat(70) {
            val a = tr.nextDouble() * 2 * PI
            val d = 150 * u * sqrt(tr.nextDouble())
            blossom.circle(tx + cos(a) * d * 1.1, top - 170 * u + sin(a) * d * 0.75, (8 + tr.nextDouble() * 16) * u)
        }
        sheet.wash(rectP(tx - 6 * u, top - 120 * u, tx + 8 * u, top + 10 * u), p.earth, p.ink, 0.9, soft = 1.0, hard = 0.12, rag = 0.8, edge = 0.6, gran = 0.4)
        sheet.wash(blossom, p.rose, Colors.mix(p.rose, p.shade, 0.5f), 0.55, soft = 2.0, hard = 0.15, rag = 0.9, edge = 1.0, gran = 0.25, miss = 0.1,
            mix = { _, y -> smooth(top - 300 * u, top - 50 * u, y) })
        // Tulips in the foreground, painted big and loose.
        tulips()
    }

    val stems = ArrayList<Pair<List<Vec>, Double>>()

    private fun tulips() {
        val tr = Random(ctx.seed + 19)
        val baseY = H + 20 * u
        val rows = listOf(ctx.h - (H - horizon) * 0.55, H - (H - horizon) * 0.28)
        val colors = listOf(p.rose, Colors.mix(p.rose, p.earth, 0.4f), p.glow, Colors.mix(p.rose, p.deep, 0.25f))
        val leaves = Path()
        var idx = 0
        for ((ri, rowY) in rows.withIndex()) {
            val size = (if (ri == 0) 70.0 else 115.0) * u
            var x = -size + tr.nextDouble() * size
            while (x < W + size) {
                // Leave a gap in the middle so the mosque reads through.
                val y = rowY + (tr.nextDouble() - 0.5) * size * 1.2
                val sz = size * (0.75 + tr.nextDouble() * 0.5)
                val lean = (tr.nextDouble() - 0.5) * 0.35
                val headY = y - sz * 0.4
                val headX = x + lean * sz
                val cup = Path()
                // Cup of three petals: a centre petal and two side ones.
                cup.moveTo(headX - sz * 0.42, headY - sz * 0.2)
                    .cubicTo(headX - sz * 0.5, headY + sz * 0.45, headX + sz * 0.5, headY + sz * 0.45, headX + sz * 0.42, headY - sz * 0.2)
                    .lineTo(headX + sz * 0.3, headY - sz * 0.62).lineTo(headX + sz * 0.12, headY - sz * 0.3)
                    .lineTo(headX, headY - sz * 0.7).lineTo(headX - sz * 0.12, headY - sz * 0.3)
                    .lineTo(headX - sz * 0.3, headY - sz * 0.62).close()
                val col = colors[idx++ % colors.size]
                sheet.wash(cup, col, Colors.mix(col, p.deep, 0.35f), 0.75, soft = 1.6, hard = 0.12, rag = 0.8, edge = 1.3, edgeWidth = 3.0, gran = 0.2,
                    mix = { _, yy -> smooth(headY - sz * 0.6, headY + sz * 0.4, yy) * 0.8 }, blooms = if (tr.nextDouble() < 0.3) listOf(Bloom(headX, headY, sz * 0.25)) else emptyList())
                // Long leaves.
                for (sgn in listOf(-1.0, 1.0)) {
                    if (tr.nextDouble() < 0.35) continue
                    val lx = x + sgn * sz * 0.2
                    val tip = Vec(lx + sgn * sz * (0.3 + tr.nextDouble() * 0.5), y - sz * (0.1 + tr.nextDouble() * 0.6))
                    leaves.moveTo(x, baseY).quadTo(lx + sgn * sz * 0.4, y + sz * 0.5, tip.x, tip.y).quadTo(lx - sgn * sz * 0.05, y + sz * 0.6, x + sgn * sz * 0.05, baseY).close()
                }
                stems += listOf(Vec(headX, headY + sz * 0.25), Vec(x + lean * sz * 0.4, y + sz * 0.6), Vec(x, baseY)) to sz
                x += sz * (0.9 + tr.nextDouble() * 0.9)
            }
        }
        sheet.wash(leaves, p.green, Colors.mix(p.green, p.deep, 0.4f), 0.75, soft = 1.5, hard = 0.12, rag = 0.8, edge = 1.0, edgeWidth = 3.0, gran = 0.35, miss = 0.06,
            mix = { _, y -> smooth(horizon, H, y) * 0.8 })
    }

    // ----------------------------------------------------------- marks

    private fun marks() {
        val c = mosque
        val lk = look()
        val s = c.s
        val inkA = lk.inkA
        val ink = lk.ink
        val wInk = 2.6 * u
        val sd = if (!(mood == Mood.SUNSET && sunX > mosqueX)) 1.0 else -1.0
        // Minarets: the shaded edge as one confident stroke, the lit edge barely touched.
        for ((i, m) in c.minarets.withIndex()) {
            val fadeTop = if (mood == Mood.MIST) 0.6 else 1.0
            hand.line(listOf(Vec(m.x + sd * m.w * 0.5, m.capBase + 4 * u), Vec(m.x + sd * m.w * 0.5, m.base - (if (mood == Mood.MIST) (m.base - m.capBase) * 0.55 else 0.0))), wInk, ink, inkA, breaks = 2)
            if (i % 2 == 0) hand.line(listOf(Vec(m.x - sd * m.w * 0.5, m.capBase + 30 * u), Vec(m.x - sd * m.w * 0.5, m.capBase + (m.base - m.capBase) * 0.45)), wInk * 0.6, ink, inkA * 0.6f, breaks = 1)
            // The lead cap: two quick strokes to the point.
            hand.line(listOf(Vec(m.x - m.w * 0.62, m.capBase), Vec(m.x, m.top)), wInk * 0.8, ink, inkA * fadeTop.toFloat(), breaks = 0, wob = 0.5)
            hand.line(listOf(Vec(m.x, m.top), Vec(m.x + m.w * 0.62, m.capBase)), wInk * 0.9, ink, inkA * fadeTop.toFloat(), breaks = 0, wob = 0.5)
            hand.line(listOf(Vec(m.x, m.top + 2 * u), Vec(m.x, m.top - 22 * s)), wInk * 0.6, ink, inkA, breaks = 0, wob = 0.3)
            // Balconies: the rail, then a row of quick ticks for the balusters.
            for (y in m.balconies) {
                hand.line(listOf(Vec(m.x - m.w * 1.05, y + 1 * u), Vec(m.x + m.w * 1.05, y)), wInk * 1.1, ink, inkA, breaks = 0, wob = 0.4)
                hand.line(listOf(Vec(m.x - m.w * 0.9, y + 9 * s), Vec(m.x - m.w * 0.5, y + 24 * s), Vec(m.x + m.w * 0.5, y + 24 * s), Vec(m.x + m.w * 0.9, y + 9 * s)), wInk * 0.55, ink, inkA * 0.7f, breaks = 1, wob = 0.4)
                val ticks = Path()
                var tx = m.x - m.w * 0.9
                while (tx <= m.x + m.w * 0.9) {
                    ticks.moveTo(tx, y - 1 * u).lineTo(tx + 0.5 * u, y - 7 * s)
                    tx += m.w * 0.3
                }
                b.stroke(ticks, ink, (1.0 * u).toFloat().coerceAtLeast(0.5f), inkA * 0.7f)
            }
        }
        // Domes: the shaded curve and the base, a few ribs on the big one.
        for ((i, d) in c.domes.withIndex()) {
            if (d.r < 16 * s && i % 2 == 1) continue
            val from = if (sd > 0) -0.15 else 0.65
            val pts = (0..16).map { k ->
                val t = (from + k / 16.0 * 0.55) * PI
                Vec(d.cx - cos(t) * d.r, d.base - sin(t) * d.h)
            }
            hand.line(if (sd > 0) pts.reversed() else pts, wInk * (if (d.r > 60 * s) 1.0 else 0.7), ink, inkA * (if (mood == Mood.MIST) 0.6f else 1f), breaks = 1, wob = 0.6)
        }
        val main = c.domes.maxBy { it.r }
        for (k in listOf(-0.55, -0.2, 0.2, 0.55)) {
            hand.line(listOf(Vec(main.cx + k * main.r, main.base - 2 * u), Vec(main.cx + k * main.r * 0.9, main.base - 14 * s)), wInk * 0.5, ink, inkA * 0.6f, breaks = 0)
        }
        // Cornice and base of the hall, quick and slightly askew.
        hand.line(listOf(Vec(c.hallLeft - 4 * u, c.hallTop + 2 * u), Vec(c.hallRight + 6 * u, c.hallTop)), wInk * 0.8, ink, inkA * 0.8f, breaks = 2)
        if (mood != Mood.MIST) hand.line(listOf(Vec(c.left - 20 * u, c.ground + 1 * u), Vec(c.right + 30 * u, c.ground - 1 * u)), wInk * 1.2, ink, inkA * 0.85f, breaks = 3)

        // Mood-specific marks.
        val r = Random(ctx.seed + 123)
        when (mood) {
            Mood.NIGHT, Mood.KANDIL -> {
                val stars = (0 until 70).map { Vec(r.nextDouble() * W, horizon * 0.62 * r.nextDouble().pow(1.3)) }
                hand.gouache(stars, { it.nextDouble().pow(3) * 2.6 * u + 0.8 * u }, p.paper, 0.85f, r)
            }
            else -> {}
        }
        if (mood == Mood.KANDIL) kandilLights(r)
        if (mood == Mood.RAIN) {
            for (lp in lampPosts) {
                hand.line(listOf(Vec(lp.x, lp.y + 20 * u), Vec(lp.x + 1 * u, horizon + 12 * u)), 4.0 * u, p.ink, 0.85f, breaks = 0, wob = 0.5)
                hand.line(listOf(Vec(lp.x - 14 * u, lp.y - 8 * u), Vec(lp.x + 14 * u, lp.y - 10 * u)), 3.0 * u, p.ink, 0.85f, breaks = 0)
            }
            // Rain: a few long faint diagonal strokes.
            val rain = Path()
            repeat(140) {
                val x = r.nextDouble() * (W + 200 * u) - 100 * u
                val y = r.nextDouble() * H
                val len = (40 + r.nextDouble() * 90) * u
                rain.moveTo(x, y).lineTo(x - len * 0.18, y + len)
            }
            b.stroke(rain, p.paper, (1.2 * u).toFloat().coerceAtLeast(0.5f), 0.35f)
        }
        if (mood == Mood.WINTER) {
            trees(r)
            val flakes = (0 until 260).map { Vec(r.nextDouble() * W, r.nextDouble() * H) }
            hand.gouache(flakes, { (1.2 + it.nextDouble().pow(3) * 4.0) * u }, Colors.WHITE, 0.9f, r)
        }
        if (mood == Mood.SPRING) {
            for ((pts, sz) in stems) hand.line(pts, sz * 0.07, Colors.mix(p.green, p.ink, 0.5f), 0.8f, breaks = 0, wob = 0.8)
        }
        for ((pt, sc) in figurePts) {
            hand.line(listOf(Vec(pt.x - 2 * sc, pt.y - 50 * sc), Vec(pt.x - 4 * sc, pt.y + 2 * sc)), 2.2 * sc, p.ink, 0.7f, breaks = 0)
        }

        // Birds over the water or the domes, never in a line.
        if (mood != Mood.RAIN) {
            val flock = if (night) 3 else 7
            val bx = if (flip) ctx.x(0.72) else ctx.x(0.3)
            val by = ctx.y(if (night) 0.38 else 0.3)
            repeat(flock) {
                val x = bx + (r.nextDouble() - 0.5) * 260 * u
                val y = by + (r.nextDouble() - 0.5) * 160 * u
                hand.bird(x, y, (8 + r.nextDouble() * 9) * u, if (night) p.paper else p.ink, if (night) 0.7f else 0.75f, r)
            }
        }
        // Water: broken horizontal brush strokes under the shore.
        if (water) {
            repeat(18) {
                val y = horizon + (H - horizon) * (0.04 + 0.6 * r.nextDouble().pow(1.5))
                val x = r.nextDouble() * W
                val len = (30 + r.nextDouble() * 120) * u
                hand.line(listOf(Vec(x, y), Vec(x + len, y + (r.nextDouble() - 0.5) * 3 * u)), (1.6 + r.nextDouble() * 1.8) * u,
                    if (night) p.ink else Colors.mix(p.deep, p.ink, 0.5f), 0.45f, breaks = 1, wob = 0.6)
            }
            if (mood == Mood.SUNSET || night) {
                repeat(7) {
                    val y = horizon + (H - horizon) * (0.03 + 0.4 * r.nextDouble())
                    val x = (moon?.x ?: sunX) + (r.nextDouble() - 0.5) * 140 * u
                    val len = (12 + r.nextDouble() * 50) * u
                    hand.line(listOf(Vec(x, y), Vec(x + len, y)), 2.0 * u, p.paper, 0.75f, breaks = 0, wob = 0.3)
                }
            }
        }
        // Splatter, flicked off the brush in one corner.
        val cornerX = if (flip) ctx.w * 0.85 else ctx.w * 0.15
        hand.splatter(cornerX, ctx.h * 0.88, 160 * u, 34, listOf(p.deep, p.earth, p.sky), 0.5f, r)
        hand.splatter(ctx.w - cornerX, ctx.h * 0.12, 90 * u, 12, listOf(p.deep, p.rose), 0.35f, r)
    }

    private fun kandilLights(r: Random) {
        val c = mosque
        // Each balcony ringed with lamps, a soft halo around it.
        for (m in c.minarets) {
            for (y in m.balconies) {
                Common.glow(b, m.x, y, 46 * u, p.glow, 0.35f)
                val dots = (0 until 7).map { k -> Vec(m.x - m.w + k * m.w * 2 / 6.0, y - 1.5 * u) }
                hand.gouache(dots, { 2.2 * u }, Colors.lighten(p.glow, 0.5f), 0.95f, r)
            }
        }
        // Mahya: a string of lights hung between two minarets.
        val ms = c.minarets.sortedBy { it.x }
        if (ms.size >= 2) {
            val a = ms.first(); val z = ms.last()
            val ya = a.balconies.firstOrNull() ?: return
            val yz = z.balconies.firstOrNull() ?: return
            val pts = (0..40).map { k ->
                val t = k / 40.0
                Vec(a.x + (z.x - a.x) * t, ya + (yz - ya) * t + sin(t * PI) * 40 * u)
            }
            hand.gouache(pts.filterIndexed { i, _ -> i % 2 == 0 }, { 2.0 * u }, Colors.lighten(p.glow, 0.4f), 0.9f, r)
            val line = Path().polygon(pts, closed = false)
            b.stroke(line, p.glow, (0.8 * u).toFloat(), 0.4f)
        }
    }

    private fun trees(r: Random) {
        val bases = listOf(ctx.x(if (flip) 0.86 else 0.08), ctx.x(if (flip) 0.98 else 0.2))
        for ((i, bx) in bases.withIndex()) {
            val by = horizon + (30 + i * 20) * u
            branch(Vec(bx, by), -PI / 2 + (r.nextDouble() - 0.5) * 0.2, (230 - i * 40) * u, 9.0 * u, 0, r)
        }
    }

    private fun branch(from: Vec, angle: Double, len: Double, width: Double, depth: Int, r: Random) {
        if (depth > 5 || len < 8 * u) return
        val mid = from + Vec.polar(len * 0.5, angle + (r.nextDouble() - 0.5) * 0.3)
        val to = from + Vec.polar(len, angle)
        hand.line(listOf(from, mid, to), width, p.ink, 0.85f, breaks = 0, wob = 0.8)
        val n = 2 + (if (r.nextDouble() < 0.3) 1 else 0)
        repeat(n) {
            val a = angle + (r.nextDouble() - 0.5) * 1.3
            branch(to, a, len * (0.55 + r.nextDouble() * 0.2), width * 0.62, depth + 1, r)
        }
    }
}
