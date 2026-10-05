package com.noor.wallpapers.art

import java.util.stream.IntStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Çini: Ottoman İznik tiles of the 16th century, painted the way they were made.
 *
 * A design (tulips, carnations, saz leaves, hatayi blossoms...) is laid out once
 * over the whole wall, as the workshop's pounced stencil was. Every tile is then
 * painted on its own: its outlines are inked with a slightly different tremor,
 * its cobalt and bole are a slightly different mix and pool darker at the edges
 * of each brush-filled area, the colour misses or overflows the outline here and
 * there, and the fired tile is set into the grout a fraction of a degree askew.
 * So the pattern repeats, but no two tiles are the same, and it breaks a little
 * at every grout line, as on a real wall.
 *
 * Colour is a raster (glaze is soft), outlines are vectors (the black line is the
 * crisp thing the eye reads), and the surface (glossy light, crazing, pinholes,
 * chipped corners) is drawn per tile on top.
 */
object CiniArt {

    // ------------------------------------------------------------------ glazes

    /** Underglaze pigments. SLIP is the white ground itself, used to paint reserve details back over colour. */
    enum class Pig { COBALT, TURQ, GREEN, RED, SLIP }

    class Glaze(
        val ground: Int, val line: Int, val cobalt: Int, val turq: Int, val green: Int, val red: Int,
        val grout: Int, val body: Int,
    ) {
        fun of(p: Pig) = when (p) {
            Pig.COBALT -> cobalt
            Pig.TURQ -> turq
            Pig.GREEN -> green
            Pig.RED -> red
            Pig.SLIP -> ground
        }
    }

    /**
     * Hand-mixed glaze sets per palette. "cobalt" is the dominant dark colour,
     * "turq" the second, "green" the leaf colour, "red" the raised bole.
     */
    fun glaze(p: Palette): Glaze {
        fun g(ground: String, line: String, cobalt: String, turq: String, green: String, red: String, grout: String, body: String) =
            Glaze(
                Colors.hex(ground), Colors.hex(line), Colors.hex(cobalt), Colors.hex(turq), Colors.hex(green),
                Colors.hex(red), Colors.hex(grout), Colors.hex(body),
            )
        return when (p.id) {
            // Classic İznik: cobalt, turquoise, raised tomato-red bole, sage, on a faintly blue-white slip.
            "iznik" -> g("#F2F5F1", "#1B2326", "#1E3E8E", "#2B9A96", "#6F8C58", "#C43C2B", "#A39C8F", "#E6DCCB")
            "betul" -> g("#F7F0EA", "#2E282C", "#1F5661", "#C97B84", "#7E9B83", "#A9404F", "#C7B6AB", "#EADBCF")
            "emerald" -> g("#F4F2E6", "#1A2520", "#1A5A44", "#3C9C88", "#8BA25A", "#B8412F", "#B7B09B", "#E4D8C0")
            "lapis" -> g("#F1F3F3", "#131B2E", "#1B2F7C", "#4C7FC4", "#2C9893", "#B2372C", "#AFB1B5", "#E1DCCF")
            "sand" -> g("#F4E9D3", "#2B1F17", "#6A3D26", "#C38B36", "#6C7B4B", "#A9482A", "#BEAA8B", "#E7D4B6")
            "isfahan" -> g("#F7F2ED", "#25202A", "#1E5E79", "#3A9FA7", "#6F8E5E", "#B0466E", "#BFB1AC", "#E8DAD2")
            "alhambra" -> g("#F4EAD7", "#29190F", "#295E6E", "#3C8B79", "#C2963C", "#B5462A", "#BDA98F", "#E8D5B8")
            "onyx" -> g("#F1EEE6", "#121212", "#26262A", "#7A8388", "#8C8778", "#A8812F", "#8F8B83", "#DCD5C8")
            "amethyst" -> g("#F5F2F7", "#211833", "#3C2572", "#8B6CC7", "#5E8C7A", "#B43F5E", "#B5AFBD", "#E2DAE6")
            // Marmara: sea cobalt and turquoise with a coral red, like the evening water off Pendik.
            "marmara" -> g("#F3F5F2", "#14232E", "#1F4E7A", "#3A9DB8", "#5E8F7A", "#D0644C", "#AEB4B5", "#E3DCCF")
            else -> Glaze(
                Colors.lighten(p.line, 0.85f), Colors.darken(p.bgBottom, 0.3f), p.accentB, p.accentA,
                Colors.mix(p.accentA, p.accentB, 0.5f), p.accentC, Colors.mix(p.bgTop, Colors.WHITE, 0.5f),
                Colors.lighten(p.line, 0.6f),
            )
        }
    }

    // ------------------------------------------------------------------ geometry helpers

    /**
     * A local drawing frame: motifs are authored with their base at (0, 0) growing
     * towards -y (up), about 100 units tall, then placed with a frame.
     */
    class Frame(val o: Vec, val ang: Double, val s: Double, val mirror: Boolean = false) {
        private val c = cos(ang)
        private val sn = sin(ang)

        fun p(x: Double, y: Double): Vec {
            val xx = if (mirror) -x else x
            return Vec(o.x + (xx * c - y * sn) * s, o.y + (xx * sn + y * c) * s)
        }

        fun p(x: Number, y: Number) = p(x.toDouble(), y.toDouble())

        /** Local "up" as a canvas direction. */
        val up get() = (p(0.0, -1.0) - o).angle

        companion object {
            /** A frame at [o] whose local "up" points along canvas direction [dir] (0 = +x, y down). */
            fun toward(o: Vec, dir: Double, s: Double, mirror: Boolean = false) = Frame(o, dir + PI / 2, s, mirror)
        }
    }

    fun cubic(p0: Vec, p1: Vec, p2: Vec, p3: Vec, t: Double): Vec {
        val m = 1 - t
        return p0 * (m * m * m) + p1 * (3 * m * m * t) + p2 * (3 * m * t * t) + p3 * (t * t * t)
    }

    fun bez(p0: Vec, p1: Vec, p2: Vec, p3: Vec, step: Double): List<Vec> {
        val len = (p1 - p0).length + (p2 - p1).length + (p3 - p2).length
        val n = (len / step).toInt().coerceIn(4, 600)
        return (0..n).map { cubic(p0, p1, p2, p3, it / n.toDouble()) }
    }

    /** A smooth curve through [pts] (Catmull-Rom). */
    fun spline(pts: List<Vec>, step: Double): List<Vec> {
        if (pts.size < 3) return pts
        val out = ArrayList<Vec>()
        for (i in 0 until pts.size - 1) {
            val p0 = pts[max(0, i - 1)]; val p1 = pts[i]; val p2 = pts[i + 1]; val p3 = pts[min(pts.size - 1, i + 2)]
            val c1 = p1 + (p2 - p0) * (1 / 6.0)
            val c2 = p2 - (p3 - p1) * (1 / 6.0)
            val seg = bez(p1, c1, c2, p2, step)
            out.addAll(if (i == 0) seg else seg.drop(1))
        }
        return out
    }

    /** Pen in a frame's local coordinates, collecting canvas points. */
    class Pen(private val f: Frame, x: Number, y: Number, private val step: Double) {
        val pts = arrayListOf(f.p(x, y))
        private var lx = x.toDouble()
        private var ly = y.toDouble()

        fun c(x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number): Pen {
            val p0 = f.p(lx, ly); val p1 = f.p(x1, y1); val p2 = f.p(x2, y2); val p3 = f.p(x3, y3)
            pts.addAll(bez(p0, p1, p2, p3, step).drop(1))
            lx = x3.toDouble(); ly = y3.toDouble()
            return this
        }

        fun l(x: Number, y: Number): Pen {
            val a = f.p(lx, ly); val b = f.p(x, y)
            val n = ((b - a).length / step).toInt().coerceAtLeast(1)
            for (i in 1..n) pts += a + (b - a) * (i / n.toDouble())
            lx = x.toDouble(); ly = y.toDouble()
            return this
        }
    }

    fun polyLen(p: List<Vec>): Double {
        var d = 0.0
        for (i in 1 until p.size) d += (p[i] - p[i - 1]).length
        return d
    }

    /** Evenly spaced points along a polyline. */
    fun resample(line: List<Vec>, step: Double): List<Vec> {
        val len = polyLen(line)
        val n = (len / step).toInt().coerceIn(2, 4000)
        val out = ArrayList<Vec>(n + 1)
        var seg = 0
        var segStart = 0.0
        for (k in 0..n) {
            val target = len * k / n
            while (seg < line.size - 2 && segStart + (line[seg + 1] - line[seg]).length < target) {
                segStart += (line[seg + 1] - line[seg]).length; seg++
            }
            val a = line[seg]; val b = line[min(seg + 1, line.size - 1)]
            val sl = (b - a).length
            out += if (sl < 1e-9) a else a + (b - a) * ((target - segStart) / sl).coerceIn(0.0, 1.0)
        }
        return out
    }

    /** Point and unit tangent at fraction [t] of a polyline's length. */
    fun along(line: List<Vec>, t: Double): Pair<Vec, Vec> {
        val target = polyLen(line) * t.coerceIn(0.0, 1.0)
        var d = 0.0
        for (i in 1 until line.size) {
            val sl = (line[i] - line[i - 1]).length
            if (d + sl >= target || i == line.size - 1) {
                val k = if (sl < 1e-9) 0.0 else ((target - d) / sl).coerceIn(0.0, 1.0)
                return (line[i - 1] + (line[i] - line[i - 1]) * k) to (line[i] - line[i - 1]).normalized()
            }
            d += sl
        }
        return line.last() to Vec(1.0, 0.0)
    }

    class Rib(val left: List<Vec>, val right: List<Vec>, val spine: List<Vec>) {
        val loop get() = left + right.asReversed()
    }

    private fun frac(x: Double) = x - floor(x)

    /**
     * A band of varying width around [spine]: leaves, petals, stems. With [teeth]
     * the edges are serrated, each tooth leaning towards the tip like a saz leaf's.
     */
    fun ribbon(
        spine: List<Vec>, step: Double, wl: (Double) -> Double, wr: (Double) -> Double = wl,
        teeth: Int = 0, depth: Double = 0.0, phase: Double = 0.0,
    ): Rib {
        val len = polyLen(spine).coerceAtLeast(1e-6)
        val st = if (teeth > 0) min(step, len / (teeth * 9.0)) else step
        val sp = resample(spine, max(st, 0.25))
        val n = sp.size
        val left = ArrayList<Vec>(n); val right = ArrayList<Vec>(n)
        var d = 0.0
        for (i in 0 until n) {
            if (i > 0) d += (sp[i] - sp[i - 1]).length
            val t = d / len
            val dir = (sp[min(n - 1, i + 1)] - sp[max(0, i - 1)]).normalized()
            val nrm = Vec(-dir.y, dir.x)
            var tl = 0.0; var tr = 0.0
            if (teeth > 0) {
                val env = sin(PI * t).coerceAtLeast(0.0).pow(0.5) * (if (t > 0.94) 0.0 else 1.0)
                tl = depth * env * frac(t * teeth + phase).pow(1.7)
                tr = depth * env * frac(t * teeth + phase + 0.45).pow(1.7)
            }
            left += sp[i] + nrm * (wl(t) + tl)
            right += sp[i] - nrm * (wr(t) + tr)
        }
        return Rib(left, right, sp)
    }

    /** A hand-drawn roundish blob: never a perfect circle. */
    fun blob(c: Vec, r: Double, rnd: Random, step: Double, lump: Double = 0.07): List<Vec> {
        val n = (2 * PI * r / step).toInt().coerceIn(10, 160)
        val p1 = rnd.nextDouble() * 6.28; val p2 = rnd.nextDouble() * 6.28
        val sq = 1 + (rnd.nextDouble() - 0.5) * 0.12
        return (0 until n).map {
            val a = 2 * PI * it / n
            val rr = r * (1 + lump * sin(2 * a + p1) * 0.6 + lump * 0.5 * sin(3 * a + p2))
            c + Vec(rr * cos(a) * sq, rr * sin(a) / sq)
        }
    }

    // ------------------------------------------------------------------ design model

    /** One painted area (closed loops) or, with no pigment, one inked line. */
    class El(val loops: List<List<Vec>>, val pig: Pig?, val outline: Boolean, val occludes: Boolean, val lw: Double) {
        val minX: Double; val minY: Double; val maxX: Double; val maxY: Double

        init {
            var a = Double.MAX_VALUE; var b = Double.MAX_VALUE; var c = -Double.MAX_VALUE; var d = -Double.MAX_VALUE
            for (l in loops) for (p in l) {
                if (p.x < a) a = p.x; if (p.y < b) b = p.y; if (p.x > c) c = p.x; if (p.y > d) d = p.y
            }
            minX = a; minY = b; maxX = c; maxY = d
        }

        /** Visible runs of this element's outline, once later areas have covered parts of it. */
        var ink: List<List<Vec>> = emptyList()

        fun overlaps(o: El) = o.maxX >= minX && o.minX <= maxX && o.maxY >= minY && o.minY <= maxY

        /** True when [p] is inside this area by more than [margin]. */
        fun covers(p: Vec, margin: Double): Boolean {
            if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) return false
            var inside = false
            for (l in loops) {
                var j = l.size - 1
                for (i in l.indices) {
                    val a = l[i]; val b = l[j]
                    if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
                    j = i
                }
            }
            if (!inside) return false
            val m2 = margin * margin
            for (l in loops) {
                var j = l.size - 1
                for (i in l.indices) {
                    if (segDist2(p, l[j], l[i]) < m2) return false
                    j = i
                }
            }
            return true
        }
    }

    private fun segDist2(p: Vec, a: Vec, b: Vec): Double {
        val dx = b.x - a.x; val dy = b.y - a.y
        val l2 = dx * dx + dy * dy
        val t = if (l2 < 1e-12) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0.0, 1.0)
        val ex = a.x + dx * t - p.x; val ey = a.y + dy * t - p.y
        return ex * ex + ey * ey
    }

    /** Everything painted on the wall, in painting order. */
    class Design(val u: Double, val step: Double, var lw: Double) {
        val els = ArrayList<El>()

        fun area(loop: List<Vec>, pig: Pig, outline: Boolean = true, occludes: Boolean = true, w: Double = 1.0) {
            if (loop.size >= 3) els += El(listOf(loop), pig, outline, occludes, lw * w)
        }

        fun areaLoops(loops: List<List<Vec>>, pig: Pig, outline: Boolean = true, occludes: Boolean = true, w: Double = 1.0) {
            val ok = loops.filter { it.size >= 3 }
            if (ok.isNotEmpty()) els += El(ok, pig, outline, occludes, lw * w)
        }

        fun line(pts: List<Vec>, w: Double = 0.75) {
            if (pts.size >= 2) els += El(listOf(pts), null, true, false, lw * w)
        }
    }

    // ------------------------------------------------------------------ motifs

    /** Ottoman tulip: narrow, with long dagger-pointed petals flaring at the tips. */
    fun Design.tulip(f: Frame, rnd: Random, body: Pig, stripe: Pig?, inner: Pig = body) {
        fun j(v: Double, a: Double = 2.5) = v + (rnd.nextDouble() - 0.5) * 2 * a
        // The back petal, mostly hidden.
        area(
            Pen(f, j(-8.0), -30, step).c(j(-10.0), -60, -5, j(-88.0), j(0.0, 2.5), j(-108.0, 3.0))
                .c(5, -88, 10, -60, j(8.0), -30).c(4, -24, -4, -24, -8, -30).pts,
            inner,
        )
        for (m in doubleArrayOf(-1.0, 1.0)) {
            val tipX = j(25.0, 3.0); val tipY = j(-99.0, 3.0); val bulge = j(22.0, 1.5)
            val pen = Pen(f, 2 * m, 0, step)
                .c(14 * m, -2, bulge * m, -16, bulge * m, -36)
                .c(bulge * m, -56, j(17.0) * m, -74, tipX * m, tipY)
                .c(14 * m, -86, 8 * m, -70, 4 * m, -50)
                .c(1 * m, -36, -2 * m, -18, -2 * m, -4)
            area(pen.pts, body)
            if (stripe != null) {
                val sp = Pen(f, 7 * m, -10, step).c(12 * m, -26, 15 * m, -52, j(18.0, 1.5) * m, -82).pts
                area(ribbon(sp, step, { t -> 2.3 * f.s * sin(PI * t).pow(0.7) }).loop, stripe, outline = false, occludes = false)
            }
        }
    }

    /** Carnation seen from the side: a striped calyx and a serrated fan of petals. */
    fun Design.carnation(f: Frame, rnd: Random, head: Pig, inner: Pig?, calyx: Pig) {
        fun j(v: Double, a: Double = 2.0) = v + (rnd.nextDouble() - 0.5) * 2 * a
        val lobes = 5
        val r = j(50.0, 3.0)
        val cy = -36.0
        val a0 = -PI + 0.28; val a1 = -0.28
        // Lobes are not all the same width or length: a painter's fan, not a protractor's.
        val widths = DoubleArray(lobes) { 0.8 + rnd.nextDouble() * 0.4 }
        val sum = widths.sum()
        val lens = DoubleArray(lobes) { 0.92 + rnd.nextDouble() * 0.12 }
        val pts = ArrayList<Vec>()
        pts += f.p(-12, cy + 3)
        val n = 26 * lobes
        var acc = 0.0
        val bounds = ArrayList<Double>()
        for (k in 0 until lobes) {
            val wk = widths[k] / sum
            val per = 26
            for (q in 0 until per) {
                val uu = q / per.toDouble()
                val a = a0 + (a1 - a0) * (acc + wk * uu)
                val teeth = 0.045 * frac(uu * 4).pow(1.5)
                val rr = r * lens[k] * (0.76 + 0.24 * sqrt(sin(PI * uu)) + teeth)
                pts += f.p(rr * cos(a), cy + rr * sin(a))
            }
            acc += wk
            if (k < lobes - 1) bounds += a0 + (a1 - a0) * acc
        }
        pts += f.p(r * 0.76 * cos(a1), cy + r * 0.76 * sin(a1))
        pts += f.p(12, cy + 3)
        area(pts, head)
        if (inner != null) {
            val ip = ArrayList<Vec>()
            ip += f.p(-9, cy + 2)
            val m = 60
            for (q in 0..m) {
                val a = (a0 + 0.2) + (a1 - a0 - 0.4) * q / m
                val rr = r * (0.44 + 0.05 * sin(q / m.toDouble() * PI * lobes))
                ip += f.p(rr * cos(a), cy + rr * sin(a))
            }
            ip += f.p(9, cy + 2)
            area(ip, inner)
        }
        for (a in bounds) line(listOf(f.p(r * 0.5 * cos(a), cy + r * 0.5 * sin(a)), f.p(r * 0.74 * cos(a), cy + r * 0.74 * sin(a))), 0.55)
        // Calyx, over the base of the fan.
        val cal = Pen(f, -6, 0, step).c(-7, -10, -12, -24, j(-14.0, 1.0), -35)
            .l(-9, -41).l(-5, -36).l(0, -42).l(5, -36).l(9, -41).l(j(14.0, 1.0), -35).c(12, -24, 7, -10, 6, 0).pts
        area(cal, calyx)
        line(Pen(f, -2.5, -4, step).c(-4, -14, -6, -24, -7, -33).pts, 0.5)
        line(Pen(f, 2.5, -4, step).c(4, -14, 6, -24, 7, -33).pts, 0.5)
    }

    /** Hyacinth: an arching stem hung with small bells, larger at the bottom. */
    fun Design.hyacinth(f: Frame, rnd: Random, bell: Pig, bend: Double = 14.0, count: Int = 7) {
        val stem = Pen(f, 0, 0, step).c(0, -40, bend * 0.5, -80, bend, -112).pts
        line(stem, 0.85)
        for (k in 0 until count) {
            val t = 0.3 + 0.66 * k / (count - 1).toDouble()
            val (p, tan) = along(stem, t)
            val side = if (k % 2 == 0) -1.0 else 1.0
            val dir = tan.angle + side * (1.95 + (rnd.nextDouble() - 0.5) * 0.35)
            val size = (1 - 0.4 * k / (count - 1).toDouble()) * f.s * (1.1 + rnd.nextDouble() * 0.12)
            val bf = Frame.toward(p, dir, size, mirror = side < 0)
            area(
                Pen(bf, -2, 0, step).c(-3, -4, -4, -8, -4.5, -11).c(-6, -13, -8.5, -14, -9, -17)
                    .c(-7, -16.5, -5, -16, -3.5, -18).c(-2, -16, 2, -16, 3.5, -18).c(5, -16, 7, -16.5, 9, -17)
                    .c(8.5, -14, 6, -13, 4.5, -11).c(4, -8, 3, -4, 2, 0).pts,
                bell,
            )
        }
        val (tip, tan) = along(stem, 1.0)
        val bud = Frame.toward(tip, tan.angle, f.s)
        area(Pen(bud, -3, 2, step).c(-6, -4, -3, -10, 0, -13).c(3, -10, 6, -4, 3, 2).pts, bell)
    }

    /**
     * Rosette (penç) seen from above: notched outer petals, an inner ring offset
     * by half a petal, and a core. Petals differ a little in size.
     */
    fun Design.rosette(c: Vec, r: Double, n: Int, rnd: Random, outer: Pig, ring: Pig?, core: Pig?, rot: Double = rnd.nextDouble() * 6.28) {
        val per = 22
        val rj = DoubleArray(n) { 1 + (rnd.nextDouble() - 0.5) * 0.1 }
        val aj = DoubleArray(n) { (rnd.nextDouble() - 0.5) * 0.08 }
        val pts = ArrayList<Vec>()
        for (i in 0 until n * per) {
            val k = i / per
            val uu = (i % per) / per.toDouble()
            val a = rot + 2 * PI * (k + uu) / n + aj[k] * sin(PI * uu)
            val notch = 0.11 * exp(-((uu - 0.5) / 0.07).pow(2))
            pts += c + Vec.polar(r * rj[k] * (0.6 + 0.4 * sin(PI * uu).pow(0.55) - notch), a)
        }
        area(pts, outer)
        for (k in 0 until n) {
            val a = rot + 2 * PI * k / n
            line(listOf(c + Vec.polar(r * 0.5, a), c + Vec.polar(r * 0.66, a)), 0.55)
        }
        if (ring != null) {
            val ip = ArrayList<Vec>()
            for (i in 0 until n * per) {
                val k = i / per
                val uu = (i % per) / per.toDouble()
                val a = rot + PI / n + 2 * PI * (k + uu) / n
                ip += c + Vec.polar(r * 0.52 * (0.72 + 0.28 * sin(PI * uu).pow(0.8)) * (1 + (rnd.nextDouble() - 0.5) * 0.02), a)
            }
            area(ip, ring)
        }
        if (core != null) area(blob(c, r * 0.2, rnd, step), core)
    }

    /** Hatayi: the lotus-palmette seen in section, a lobed crown around a heart and a tongue. */
    fun Design.hatayi(f: Frame, rnd: Random, crown: Pig, heart: Pig, core: Pig, cup: Pig = Pig.GREEN) {
        fun j(v: Double, a: Double = 2.0) = v + (rnd.nextDouble() - 0.5) * 2 * a
        val c = Vec(0.0, -42.0)
        val r = j(46.0, 2.0)
        val lobes = 5
        val a0 = PI * 0.87; val a1 = PI * 2.13
        val pts = ArrayList<Vec>()
        pts += f.p(-13, -12)
        val notches = ArrayList<Double>()
        val lj = DoubleArray(lobes) { 0.94 + rnd.nextDouble() * 0.1 }
        for (k in 0 until lobes) {
            for (q in 0 until 24) {
                val uu = q / 24.0
                val a = a0 + (a1 - a0) * (k + uu) / lobes
                // Lobe tips lean outward, away from the centre line.
                val skew = if (k < lobes / 2) uu.pow(0.8) else if (k > lobes / 2) uu.pow(1.25) else uu
                val rr = r * lj[k] * (0.7 + 0.3 * sin(PI * skew).pow(0.6))
                pts += f.p(c.x + rr * cos(a), c.y + rr * sin(a))
            }
            if (k > 0) notches += a0 + (a1 - a0) * k / lobes
        }
        pts += f.p(13, -12)
        area(pts, crown)
        for (a in notches) line(listOf(f.p(c.x + r * 0.62 * cos(a), c.y + r * 0.62 * sin(a)), f.p(c.x + r * 0.8 * cos(a), c.y + r * 0.8 * sin(a))), 0.55)
        area(
            Pen(f, 0, -12, step).c(-28, -18, j(-33.0), -56, -10, -66).c(-4, -69, -1, -72, j(0.0, 1.5), -80)
                .c(1, -72, 4, -69, 10, -66).c(j(33.0), -56, 28, -18, 0, -12).pts,
            heart,
        )
        area(Pen(f, 0, -20, step).c(-12, -30, -9, -46, j(0.0, 1.0), -60).c(9, -46, 12, -30, 0, -20).pts, core)
        area(Pen(f, -27, -15, step).c(-20, 3, 20, 3, 27, -15).c(16, -7, -16, -7, -27, -15).pts, cup)
        line(Pen(f, -27, -15, step).c(-34, -22, -40, -14, -34, -9).pts, 0.6)
        line(Pen(f, 27, -15, step).c(34, -22, 40, -14, 34, -9).pts, 0.6)
    }

    /** Saz leaf: long, curving, serrated, with a midrib; optionally two-toned along the rib. */
    fun Design.saz(spine: List<Vec>, width: Double, pig: Pig, half: Pig?, teethK: Double = 1.0, asym: Double = 1.0, phase: Double = 0.0) {
        val len = polyLen(spine)
        val teeth = max(6, (len / (width * 0.8) * teethK).toInt())
        val prof = { t: Double -> width * sin(PI * (0.07 + 0.93 * t).pow(0.8)).coerceAtLeast(0.0).pow(0.85) }
        val rib = ribbon(spine, step, prof, { t -> prof(t) * asym }, teeth, width * 0.17, phase)
        area(rib.loop, pig)
        if (half != null) area(rib.left + rib.spine.asReversed(), half, outline = false, occludes = false)
        val n = rib.spine.size
        line(rib.spine.subList((n * 0.03).toInt(), max(2, (n * 0.86).toInt())), 0.6)
    }

    /** A simple pointed leaf from [base] to [tip], bowed sideways by [bend]. */
    fun Design.leaf(base: Vec, tip: Vec, width: Double, bend: Double, pig: Pig, vein: Boolean = true) {
        val d = tip - base
        val nrm = Vec(-d.y, d.x).normalized()
        val sp = bez(base, base + d * 0.33 + nrm * (bend * d.length), base + d * 0.7 + nrm * (bend * d.length * 0.8), tip, step)
        val rib = ribbon(sp, step, { t -> width * sin(PI * (0.05 + 0.95 * t)).coerceAtLeast(0.0).pow(0.75) })
        area(rib.loop, pig)
        if (vein) {
            val n = rib.spine.size
            line(rib.spine.subList((n * 0.08).toInt(), max(2, (n * 0.75).toInt())), 0.5)
        }
    }

    /** A green stem with outlines on both sides, narrowing as it rises. */
    fun Design.stem(pts: List<Vec>, width: Double, pig: Pig = Pig.GREEN) {
        area(ribbon(pts, step, { t -> width / 2 * (1 - 0.35 * t) }).loop, pig)
    }

    /** Prunus or small five-petalled blossom. */
    fun Design.blossom(c: Vec, r: Double, rnd: Random, pig: Pig, eye: Pig?, petals: Int = 5) {
        val rot = rnd.nextDouble() * 6.28
        val per = 16
        val rj = DoubleArray(petals) { 0.9 + rnd.nextDouble() * 0.2 }
        val pts = (0 until petals * per).map { i ->
            val k = i / per; val uu = (i % per) / per.toDouble()
            c + Vec.polar(r * rj[k] * (0.42 + 0.58 * sin(PI * uu).pow(0.65)), rot + 2 * PI * (k + uu) / petals)
        }
        area(pts, pig)
        if (eye != null) area(blob(c, r * 0.3, rnd, step), eye)
    }

    /** Rumi: the split, hooked arabesque leaf. */
    fun Design.rumi(f: Frame, pig: Pig) {
        area(
            Pen(f, 0, 0, step).c(-18, -12, -26, -44, -14, -70).c(-6, -86, 14, -96, 30, -88).c(38, -84, 42, -74, 44, -60)
                .c(34, -70, 22, -75, 12, -70).c(4, -64, 2, -54, 8, -44).c(14, -36, 24, -32, 33, -29).c(20, -22, 10, -14, 6, 0).pts,
            pig,
        )
    }

    // ------------------------------------------------------------------ the wall

    class Tile(val x0: Double, val y0: Double, val size: Double, grout: Double, u: Double, val seed: Int) {
        private val rnd = Random(seed)
        var id = 0
        val cx = x0 + size / 2
        val cy = y0 + size / 2
        val rot = (rnd.nextDouble() - 0.5) * 2 * 0.0065
        private val dx = (rnd.nextDouble() - 0.5) * 2 * 1.4 * u
        private val dy = (rnd.nextDouble() - 0.5) * 2 * 1.4 * u
        private val cr = cos(rot)
        private val sr = sin(rot)
        private val half = size / 2 - grout / 2 + (rnd.nextDouble() - 0.5) * 1.4 * u
        val tint = 1 + (rnd.nextDouble() - 0.5) * 0.045
        val warm = (rnd.nextDouble() - 0.5) * 0.035
        val pigShift = (rnd.nextDouble() - 0.5) * 0.12
        val gloss = 0.65 + rnd.nextDouble() * 0.7

        fun map(p: Vec): Vec {
            val x = p.x - cx; val y = p.y - cy
            return Vec(cx + dx + x * cr - y * sr, cy + dy + x * sr + y * cr)
        }

        val corners = listOf(Vec(-half, -half), Vec(half, -half), Vec(half, half), Vec(-half, half)).map { map(Vec(cx + it.x, cy + it.y)) }

        fun near(e: El, m: Double) = e.maxX >= x0 - m && e.minX <= x0 + size + m && e.maxY >= y0 - m && e.minY <= y0 + size + m

        /** The fired tile's outline: slightly rounded corners, edges not quite straight. */
        fun outline(u: Double): Path {
            val r = 2.4 * u
            val nz = Noise(seed + 3)
            val pts = ArrayList<Vec>()
            for (k in 0 until 4) {
                val a = corners[k]; val b = corners[(k + 1) % 4]
                val d = (b - a).normalized()
                val nrm = Vec(-d.y, d.x)
                val len = (b - a).length
                val steps = 10
                for (i in 0..steps) {
                    val t = i / steps.toDouble()
                    val along = r + (len - 2 * r) * t
                    val wob = if (i == 0 || i == steps) 0.0 else (nz.at(k * 7.0 + t * 3, 0.5) - 0.5) * 0.9 * u
                    pts += a + d * along + nrm * wob
                }
                val c = b; val dn = (corners[(k + 2) % 4] - b).normalized()
                pts += c + d * (-r * 0.35) + dn * (r * 0.35)
            }
            return Path().polygon(pts)
        }
    }

    class Wall(val ox: Double, val oy: Double, val t: Double, grout: Double, ctx: RenderContext) {
        private val c0 = floor(-ox / t).toInt()
        private val c1 = floor((ctx.w - ox) / t).toInt()
        private val r0 = floor(-oy / t).toInt()
        private val r1 = floor((ctx.h - oy) / t).toInt()
        private val cols = c1 - c0 + 1
        val tiles: List<Tile> = buildList {
            for (r in r0..r1) for (c in c0..c1) add(Tile(ox + c * t, oy + r * t, t, grout, ctx.u, ctx.seed * 7349 + r * 977 + c * 131 + 17))
        }

        init { tiles.forEachIndexed { i, tile -> tile.id = i } }

        fun at(x: Double, y: Double): Tile {
            val c = floor((x - ox) / t).toInt().coerceIn(c0, c1)
            val r = floor((y - oy) / t).toInt().coerceIn(r0, r1)
            return tiles[(r - r0) * cols + (c - c0)]
        }
    }

    // ------------------------------------------------------------------ cropping

    /** The parts of polylines inside a rectangle (keeping one point beyond each end so strokes run off cleanly). */
    private fun cropRuns(runs: List<List<Vec>>, x0: Double, y0: Double, x1: Double, y1: Double): List<List<Vec>> {
        val out = ArrayList<List<Vec>>()
        for (r in runs) {
            var cur = ArrayList<Vec>()
            for ((i, p) in r.withIndex()) {
                val inside = p.x >= x0 && p.x <= x1 && p.y >= y0 && p.y <= y1
                if (inside) {
                    if (cur.isEmpty() && i > 0) cur += r[i - 1]
                    cur += p
                } else if (cur.isNotEmpty()) {
                    cur += p
                    if (cur.size > 1) out += cur
                    cur = ArrayList()
                }
            }
            if (cur.size > 1) out += cur
        }
        return out
    }

    /** Sutherland-Hodgman clip of a closed polygon to a rectangle; untouched when it already fits. */
    private fun clipLoop(loop: List<Vec>, x0: Double, y0: Double, x1: Double, y1: Double): List<Vec> {
        if (loop.all { it.x >= x0 && it.x <= x1 && it.y >= y0 && it.y <= y1 }) return loop
        var pts = loop
        for (edge in 0 until 4) {
            if (pts.isEmpty()) break
            fun inside(p: Vec) = when (edge) { 0 -> p.x >= x0; 1 -> p.x <= x1; 2 -> p.y >= y0; else -> p.y <= y1 }
            fun cross(a: Vec, b: Vec): Vec {
                val t = when (edge) {
                    0 -> (x0 - a.x) / (b.x - a.x); 1 -> (x1 - a.x) / (b.x - a.x)
                    2 -> (y0 - a.y) / (b.y - a.y); else -> (y1 - a.y) / (b.y - a.y)
                }
                return a + (b - a) * t
            }
            val out = ArrayList<Vec>(pts.size)
            for (i in pts.indices) {
                val cur = pts[i]; val prev = pts[(i + pts.size - 1) % pts.size]
                val ci = inside(cur); val pi = inside(prev)
                if (ci) { if (!pi) out += cross(prev, cur); out += cur } else if (pi) out += cross(prev, cur)
            }
            pts = out
        }
        return pts
    }

    // ------------------------------------------------------------------ occlusion

    /** Outlines of an area disappear where a later area is painted over them. */
    private fun computeInk(d: Design) {
        val els = d.els
        IntStream.range(0, els.size).parallel().forEach { i ->
            val e = els[i]
            if (!e.outline) return@forEach
            val occ = ArrayList<El>()
            for (j in i + 1 until els.size) {
                val o = els[j]
                if (o.pig != null && o.occludes && o.overlaps(e)) occ += o
            }
            val closed = e.pig != null
            val margin = e.lw * 0.7
            val runs = ArrayList<List<Vec>>()
            for (loop in e.loops) {
                // Closed outlines overlap their start a little so the pen joint doesn't show.
                fun whole() = if (closed) loop + loop[0] + loop[1] else loop
                if (occ.isEmpty()) { runs += whole(); continue }
                val hidden = BooleanArray(loop.size) { k -> occ.any { it.covers(loop[k], margin) } }
                if (hidden.none { it }) { runs += whole(); continue }
                if (hidden.all { it }) continue
                val n = loop.size
                val start = if (closed) hidden.indexOfFirst { it } else 0
                var cur = ArrayList<Vec>()
                for (k in 0 until if (closed) n + 1 else n) {
                    val idx = (start + k) % n
                    if (hidden[idx]) {
                        if (cur.size > 1) runs += cur
                        cur = ArrayList()
                    } else cur += loop[idx]
                }
                if (cur.size > 1) runs += cur
            }
            e.ink = runs
        }
    }

    // ------------------------------------------------------------------ glaze raster

    private const val PIXEL_BUDGET = 900_000.0

    private class Buf(val w: Int, val h: Int) {
        val r = FloatArray(w * h)
        val g = FloatArray(w * h)
        val b = FloatArray(w * h)
        val mot = FloatArray(w * h)
        val cover = FloatArray(w * h)
    }

    /** A tile's four edges as half-planes in raster pixels: inside where nx*x + ny*y >= nc. */
    private class Edges(t: Tile, s: Double) {
        val nx = DoubleArray(4)
        val ny = DoubleArray(4)
        val nc = DoubleArray(4)

        init {
            val q = t.corners.map { it / s }
            for (k in 0 until 4) {
                val a = q[k]; val dd = (q[(k + 1) % 4] - a).normalized()
                nx[k] = -dd.y; ny[k] = dd.x; nc[k] = nx[k] * a.x + ny[k] * a.y
            }
        }

        /** Distance inside the tile in raster px (negative in the grout). */
        fun dist(x: Double, y: Double): Double {
            var dm = Double.MAX_VALUE
            for (k in 0 until 4) dm = min(dm, nx[k] * x + ny[k] * y - nc[k])
            return dm
        }
    }

    /**
     * The glazed wall as pixels: the slip ground of each tile (each its own
     * whiteness), the sandy grout in the gaps with the tiles' shadow on it, and
     * every painted area laid on tile by tile.
     */
    private fun glazeRaster(ctx: RenderContext, gl: Glaze, wall: Wall, d: Design): RasterItem {
        val u = ctx.u
        val s = max(1.0, sqrt(ctx.w * ctx.h / PIXEL_BUDGET))
        val rw = ceil(ctx.w / s).toInt().coerceAtLeast(1)
        val rh = ceil(ctx.h / s).toInt().coerceAtLeast(1)
        val buf = Buf(rw, rh)
        val edges = wall.tiles.map { Edges(it, s) }
        val gN = Noise(ctx.seed * 13 + 5)
        val pN = Noise(ctx.seed * 17 + 9)
        val fN = Noise(ctx.seed * 19 + 3)
        val gr = Colors.red(gl.ground) / 255f
        val gg = Colors.green(gl.ground) / 255f
        val gb = Colors.blue(gl.ground) / 255f
        val mr = Colors.red(gl.grout) / 255f
        val mg = Colors.green(gl.grout) / 255f
        val mb = Colors.blue(gl.grout) / 255f
        IntStream.range(0, rh).parallel().forEach { y ->
            val yy = (y + 0.5) * s
            for (x in 0 until rw) {
                val xx = (x + 0.5) * s
                val t = wall.at(xx, yy)
                val e = edges[t.id]
                var dm = Double.MAX_VALUE
                var side = 0
                for (k in 0 until 4) {
                    val v = e.nx[k] * (x + 0.5) + e.ny[k] * (y + 0.5) - e.nc[k]
                    if (v < dm) { dm = v; side = k }
                }
                val cover = (dm + 0.5).coerceIn(0.0, 1.0).toFloat()
                val m = gN.fbm(xx / (80 * u), yy / (80 * u), 3)
                val f = fN.at(xx / (3.2 * u), yy / (3.2 * u))
                val i = y * rw + x
                // Tile: slip ground, a little whiter or greyer on each tile; glaze pools greenish at the edge.
                val k = (t.tint * (1 + 0.05 * (m - 0.5) + 0.03 * (f - 0.5))).toFloat()
                val cast = ((m - 0.52) * 0.6).coerceIn(-0.1, 0.2).toFloat()
                val warm = t.warm.toFloat()
                val rim = (0.075 * exp(-max(0.0, dm) * s / (2.4 * u))).toFloat()
                val tr = gr * k * (1 + warm - cast * 0.05f) * (1 - rim * 1.4f)
                val tg = gg * k * (1 + warm * 0.4f - cast * 0.02f) * (1 - rim * 0.8f)
                val tb = gb * k * (1 - warm * 0.6f + cast * 0.01f) * (1 - rim)
                // Grout: gritty mortar, in the shadow of the tile above-left.
                val grit = (0.88 + 0.16 * f + 0.1 * (m - 0.5) + 0.1 * (fN.at(xx / (1.1 * u), yy / (1.1 * u)) - 0.5)).toFloat()
                val shade = if (dm < 0 && (side == 1 || side == 2)) (1 - 0.42 * exp(dm * s / (2.2 * u))).toFloat() else 1f
                val g = grit * shade
                buf.r[i] = mr * g + (tr - mr * g) * cover
                buf.g[i] = mg * g + (tg - mg * g) * cover
                buf.b[i] = mb * g + (tb - mb * g) * cover
                buf.cover[i] = cover
                buf.mot[i] = (pN.fbm(xx / (15 * u), yy / (15 * u), 2) * 0.75 + f * 0.25).toFloat()
            }
        }
        val streaks = Noise(ctx.seed * 23 + 1)
        IntStream.range(0, wall.tiles.size).parallel().forEach { paintTile(wall.tiles[it], edges[it], d, gl, s, u, buf, streaks) }
        // Broad sheen of the glaze from the room's light, upper left.
        val lx = ctx.x(0.3); val ly = ctx.y(0.15)
        val lr = 0.95 * max(ctx.w, ctx.h)
        val px = IntArray(rw * rh)
        IntStream.range(0, rh).parallel().forEach { y ->
            val yy = (y + 0.5) * s
            for (x in 0 until rw) {
                val i = y * rw + x
                val xx = (x + 0.5) * s
                val dd = sqrt((xx - lx) * (xx - lx) + (yy - ly) * (yy - ly)) / lr
                val sh = (0.13 * wall.at(xx, yy).gloss * (1 - dd).coerceAtLeast(0.0).pow(1.5)).toFloat() * buf.cover[i]
                val r = buf.r[i] + sh * (1 - buf.r[i]); val g = buf.g[i] + sh * (1 - buf.g[i]); val b = buf.b[i] + sh * (1 - buf.b[i])
                px[i] = Colors.argb(255, (r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())
            }
        }
        return RasterItem(rw, rh, px, 0f, 0f, (rw * s).toFloat(), (rh * s).toFloat())
    }

    private class Look(val pool: Float, val mottle: Float, val streak: Float, val opacity: Float)

    private fun look(p: Pig) = when (p) {
        Pig.COBALT -> Look(0.2f, 0.22f, 0.2f, 0.97f)
        Pig.TURQ -> Look(0.15f, 0.16f, 0.15f, 0.9f)
        Pig.GREEN -> Look(0.15f, 0.16f, 0.15f, 0.9f)
        Pig.RED -> Look(0.08f, 0.1f, 0.06f, 1f)
        Pig.SLIP -> Look(0f, 0.03f, 0f, 1f)
    }

    /** Paints one tile's share of every area, with this tile's own hand and glaze. */
    private fun paintTile(t: Tile, edges: Edges, d: Design, gl: Glaze, s: Double, u: Double, buf: Buf, streaks: Noise) {
        val rw = buf.w; val rh = buf.h
        val q = t.corners.map { it / s }
        val bx0 = (floor(q.minOf { it.x }) - 2).toInt().coerceIn(0, rw - 1)
        val by0 = (floor(q.minOf { it.y }) - 2).toInt().coerceIn(0, rh - 1)
        val bx1 = (ceil(q.maxOf { it.x }) + 2).toInt().coerceIn(0, rw - 1)
        val by1 = (ceil(q.maxOf { it.y }) + 2).toInt().coerceIn(0, rh - 1)
        val bw = bx1 - bx0 + 1; val bh = by1 - by0 + 1
        if (bw < 2 || bh < 2) return
        // Colour stops at the tile's own edge.
        val clip = FloatArray(bw * bh)
        for (y in 0 until bh) for (x in 0 until bw) {
            clip[y * bw + x] = (edges.dist(bx0 + x + 0.5, by0 + y + 0.5) + 0.5).coerceIn(0.0, 1.0).toFloat()
        }
        val cov = FloatArray(bw * bh)
        val tmp = FloatArray(bw * bh)
        val blur = FloatArray(bw * bh)
        val rp = max(1, (2.6 * u / s).roundToInt())
        val margin = 10 * u
        val tr = Colors.red(gl.ground) / 255f * t.tint.toFloat()
        val tg = Colors.green(gl.ground) / 255f * t.tint.toFloat()
        val tb = Colors.blue(gl.ground) / 255f * t.tint.toFloat()
        val sAlong = 1.0 / (17 * u)
        val sAcross = 1.0 / (1.9 * u)
        for ((ei, e) in d.els.withIndex()) {
            val pig = e.pig ?: continue
            if (!t.near(e, margin)) continue
            val rnd = Random(t.seed * 7919 + ei * 104729)
            val amp = (0.9 + rnd.nextDouble() * 1.8) * u
            val cm = 8 * u
            val clipped = e.loops.map { clipLoop(it, t.x0 - cm, t.y0 - cm, t.x0 + t.size + cm, t.y0 + t.size + cm) }.filter { it.size >= 3 }
            if (clipped.isEmpty()) continue
            val loops = Brush.wobble(clipped.map { l -> l.map(t::map) }, amp, 10 * u, t.seed * 31 + ei)
            var mnx = Double.MAX_VALUE; var mny = Double.MAX_VALUE; var mxx = -Double.MAX_VALUE; var mxy = -Double.MAX_VALUE
            val xs = loops.map { l -> DoubleArray(l.size) { (l[it].x / s - bx0).also { v -> mnx = min(mnx, v); mxx = max(mxx, v) } } }
            val ys = loops.map { l -> DoubleArray(l.size) { (l[it].y / s - by0).also { v -> mny = min(mny, v); mxy = max(mxy, v) } } }
            val x0 = (floor(mnx).toInt() - rp - 1).coerceAtLeast(0)
            val y0 = (floor(mny).toInt() - rp - 1).coerceAtLeast(0)
            val x1 = (ceil(mxx).toInt() + rp + 1).coerceAtMost(bw - 1)
            val y1 = (ceil(mxy).toInt() + rp + 1).coerceAtMost(bh - 1)
            if (x1 < x0 || y1 < y0) continue
            for (y in y0..y1) java.util.Arrays.fill(cov, y * bw + x0, y * bw + x1 + 1, 0f)
            fillPolys(xs, ys, cov, bw, y0, y1)
            boxBlur(cov, blur, tmp, bw, x0, y0, x1, y1, rp)

            val lk = look(pig)
            var col = if (pig == Pig.SLIP) Colors.argb(255, (tr * 255).roundToInt(), (tg * 255).roundToInt(), (tb * 255).roundToInt())
            else gl.of(pig).jitter(rnd, 0.07f)
            if (pig != Pig.SLIP) {
                col = if (t.pigShift > 0) Colors.darken(col, t.pigShift.toFloat()) else Colors.lighten(col, (-t.pigShift).toFloat())
            }
            val cr = Colors.red(col) / 255f; val cg = Colors.green(col) / 255f; val cb = Colors.blue(col) / 255f
            // The brush was drawn through this area in one direction: its strokes show as faint streaks.
            val phi = rnd.nextDouble() * PI
            val ca = cos(phi); val sa = sin(phi)
            val so = rnd.nextDouble() * 500
            for (y in y0..y1) {
                val row = y * bw
                val grow = (by0 + y) * rw + bx0
                val yy = (by0 + y + 0.5) * s
                for (x in x0..x1) {
                    val li = row + x
                    val c = min(1f, cov[li])
                    if (c < 0.004f) continue
                    val a0 = c * clip[li]
                    if (a0 < 0.004f) continue
                    val gi = grow + x
                    val m = buf.mot[gi]
                    val bl = blur[li]
                    val pool = ((1f - bl) * 2f).coerceIn(0f, 1f)
                    var k = 1f - lk.pool * pool * (0.2f + 1.6f * m) + lk.mottle * (m - 0.5f)
                    if (lk.streak > 0f) {
                        val xx = (bx0 + x + 0.5) * s
                        val st = streaks.at((xx * ca + yy * sa) * sAlong + so, (-xx * sa + yy * ca) * sAcross)
                        k += lk.streak * (st.toFloat() - 0.5f) * 1.6f
                    }
                    if (pig == Pig.RED && x > 0 && x < bw - 1 && y > 0 && y < bh - 1) {
                        // Bole is laid on thick and stands proud: its upper-left rim catches the light.
                        val gx = blur[li + 1] - blur[li - 1]
                        val gy = blur[li + bw] - blur[li - bw]
                        k += 0.45f * (gx + gy)
                    }
                    val alpha = (a0 * lk.opacity * (0.85f + 0.3f * m)).coerceAtMost(1f)
                    val ia = 1f - alpha
                    buf.r[gi] = buf.r[gi] * ia + (cr * k).coerceIn(0f, 1f) * alpha
                    buf.g[gi] = buf.g[gi] * ia + (cg * k).coerceIn(0f, 1f) * alpha
                    buf.b[gi] = buf.b[gi] * ia + (cb * k).coerceIn(0f, 1f) * alpha
                }
            }
        }
    }

    /** Nonzero-winding coverage of polygons (local raster coords) added into [buf], 4 sub-scanlines per row. */
    private fun fillPolys(xs: List<DoubleArray>, ys: List<DoubleArray>, buf: FloatArray, bw: Int, ry0: Int, ry1: Int) {
        var n = 0
        for (l in xs) n += l.size
        val ex0 = DoubleArray(n); val ey0 = DoubleArray(n); val ex1 = DoubleArray(n); val ey1 = DoubleArray(n)
        val dir = IntArray(n)
        var m = 0
        for ((li, lx) in xs.withIndex()) {
            val ly = ys[li]
            for (i in lx.indices) {
                val j = if (i + 1 == lx.size) 0 else i + 1
                if (ly[i] == ly[j]) continue
                if (ly[i] < ly[j]) {
                    ex0[m] = lx[i]; ey0[m] = ly[i]; ex1[m] = lx[j]; ey1[m] = ly[j]; dir[m] = 1
                } else {
                    ex0[m] = lx[j]; ey0[m] = ly[j]; ex1[m] = lx[i]; ey1[m] = ly[i]; dir[m] = -1
                }
                m++
            }
        }
        if (m == 0) return
        val order = (0 until m).sortedBy { ey0[it] }.toIntArray()
        val active = IntArray(m)
        var na = 0
        var next = 0
        val cx = DoubleArray(m); val cd = IntArray(m)
        val yStart = max(ry0, floor(ey0[order[0]]).toInt())
        var yMax = -Double.MAX_VALUE
        for (i in 0 until m) yMax = max(yMax, ey1[i])
        val yEnd = min(ry1, ceil(yMax).toInt())
        for (row in yStart..yEnd) {
            for (sub in 0 until 4) {
                val yy = row + (sub + 0.5) * 0.25
                while (next < m && ey0[order[next]] <= yy) active[na++] = order[next++]
                var k = 0; var nc = 0
                for (q in 0 until na) {
                    val e = active[q]
                    if (ey1[e] <= yy) continue
                    active[k++] = e
                    val x = ex0[e] + (yy - ey0[e]) * (ex1[e] - ex0[e]) / (ey1[e] - ey0[e])
                    var p = nc++
                    while (p > 0 && cx[p - 1] > x) { cx[p] = cx[p - 1]; cd[p] = cd[p - 1]; p-- }
                    cx[p] = x; cd[p] = dir[e]
                }
                na = k
                var wind = 0
                var start = 0.0
                for (c in 0 until nc) {
                    val before = wind
                    wind += cd[c]
                    if (before == 0 && wind != 0) start = cx[c]
                    else if (before != 0 && wind == 0) span(buf, row * bw, bw, start, cx[c])
                }
            }
        }
    }

    private fun span(buf: FloatArray, row: Int, bw: Int, xa0: Double, xb0: Double) {
        val xa = xa0.coerceIn(0.0, bw.toDouble()); val xb = xb0.coerceIn(0.0, bw.toDouble())
        if (xb <= xa) return
        val w = 0.25f
        val ia = xa.toInt(); val ib = xb.toInt()
        if (ia == ib) { buf[row + min(ia, bw - 1)] += ((xb - xa) * w).toFloat(); return }
        buf[row + ia] += ((ia + 1 - xa) * w).toFloat()
        for (i in ia + 1 until ib) buf[row + i] += w
        if (ib < bw) buf[row + ib] += ((xb - ib) * w).toFloat()
    }

    private fun boxBlur(src: FloatArray, dst: FloatArray, tmp: FloatArray, bw: Int, x0: Int, y0: Int, x1: Int, y1: Int, r: Int) {
        val inv = 1f / (2 * r + 1)
        for (y in y0..y1) {
            val row = y * bw
            var acc = 0f
            for (x in x0..min(x1, x0 + r)) acc += min(1f, src[row + x])
            for (x in x0..x1) {
                tmp[row + x] = acc * inv
                val add = x + r + 1
                if (add <= x1) acc += min(1f, src[row + add])
                val rem = x - r
                if (rem >= x0) acc -= min(1f, src[row + rem])
            }
        }
        for (x in x0..x1) {
            var acc = 0f
            for (y in y0..min(y1, y0 + r)) acc += tmp[y * bw + x]
            for (y in y0..y1) {
                dst[y * bw + x] = acc * inv
                val add = y + r + 1
                if (add <= y1) acc += tmp[add * bw + x]
                val rem = y - r
                if (rem >= y0) acc -= tmp[rem * bw + x]
            }
        }
    }

    // ------------------------------------------------------------------ surface

    private fun paint(b: SceneBuilder, ctx: RenderContext, gl: Glaze, wall: Wall, d: Design) {
        computeInk(d)
        val u = ctx.u
        b.raster(glazeRaster(ctx, gl, wall, d))
        val outlines = wall.tiles.map { it.outline(u) }
        val union = Path()
        outlines.forEach { union.append(it) }
        // The crisp seam where glaze meets grout.
        b.stroke(union, Colors.darken(gl.grout, 0.55f), (1.1 * u).toFloat().coerceAtLeast(0.6f), 0.5f)
        val window = Window(ctx)
        wall.tiles.forEachIndexed { i, t -> b.group(outlines[i]) { tileSurface(this, ctx, gl, d, t, window) } }
        Common.vignette(b, ctx, 0.3f)
    }

    /** Where the window's reflection falls: a soft slanted band of light across the glaze. */
    private class Window(ctx: RenderContext) {
        val n = Vec(cos(-0.3), sin(-0.3))
        val c0 = Vec(ctx.x(0.22), ctx.y(0.3)) dot n
        val half = 0.17 * minOf(ctx.w, ctx.h)
    }

    private fun tileSurface(b: SceneBuilder, ctx: RenderContext, gl: Glaze, d: Design, t: Tile, win: Window) {
        val u = ctx.u
        val rnd = Random(t.seed * 3 + 1)
        val corners = t.corners
        fun anywhere() = t.map(Vec(t.x0 + rnd.nextDouble() * t.size, t.y0 + rnd.nextDouble() * t.size))

        // Ink: every tile outlined by hand, with its own tremor.
        val ink = Path()
        for ((ei, e) in d.els.withIndex()) {
            if (e.ink.isEmpty() || !t.near(e, 4 * u)) continue
            val wr = Random(t.seed * 977 + ei)
            val cm = 6 * u
            val cropped = cropRuns(e.ink, t.x0 - cm, t.y0 - cm, t.x0 + t.size + cm, t.y0 + t.size + cm)
            if (cropped.isEmpty()) continue
            val runs = Brush.wobble(cropped.map { r -> r.map(t::map) }, 0.55 * u, 26 * u, t.seed * 131 + ei)
            val w = e.lw * (0.82 + 0.36 * wr.nextDouble())
            runs.forEachIndexed { k, r ->
                ink.append(Brush.inkStroke(r, w, t.seed + ei * 13 + k, taper = e.pig == null, pressure = 0.42))
            }
        }
        b.fill(ink, gl.line, 0.93f)

        // Crazing: a fine net of cracks in the glaze, stronger on some tiles than others.
        if (rnd.nextDouble() < 0.75) {
            val craze = Path()
            repeat(6 + rnd.nextInt(14)) {
                var p = anywhere()
                var a = rnd.nextDouble() * 6.28
                craze.moveTo(p.x, p.y)
                repeat(3 + rnd.nextInt(8)) {
                    a += (rnd.nextDouble() - 0.5) * 1.3
                    p += Vec.polar((6 + rnd.nextDouble() * 18) * u, a)
                    craze.lineTo(p.x, p.y)
                }
            }
            b.stroke(craze, Colors.hex("#5A4A3A"), (0.6 * u).toFloat().coerceAtLeast(0.5f), (0.07 + rnd.nextDouble() * 0.09).toFloat())
        }

        // Pinholes and iron specks from the kiln; now and then a stray drop of pigment.
        val holes = Path()
        repeat(3 + rnd.nextInt(9)) { val p = anywhere(); holes.circle(p.x, p.y, (0.45 + rnd.nextDouble() * 0.6) * u) }
        b.fill(holes, Colors.hex("#4A4038"), 0.3f)
        val specks = Path()
        repeat(rnd.nextInt(4)) { specks.polygon(blob(anywhere(), (0.6 + rnd.nextDouble() * 1.1) * u, rnd, 0.6 * u, 0.25)) }
        b.fill(specks, Colors.hex("#6B4A2E"), 0.45f)
        if (rnd.nextDouble() < 0.18) {
            b.fill(Path().polygon(blob(anywhere(), (1.3 + rnd.nextDouble() * 2.0) * u, rnd, 0.6 * u, 0.3)), gl.cobalt, 0.5f)
        }

        // Rounded edges: the top and left catch the light, the bottom and right fall into shade.
        val inset = 1.7 * u
        fun pin(v: Vec) = v + (Vec(t.cx, t.cy) - v).normalized() * inset
        val tl = pin(corners[0]); val tr = pin(corners[1]); val br = pin(corners[2]); val bl = pin(corners[3])
        b.stroke(Path().polygon(listOf(bl, tl, tr), closed = false), Colors.WHITE, (1.8 * u).toFloat(), 0.3f)
        b.stroke(Path().polygon(listOf(tr, br, bl), closed = false), Colors.hex("#2A2A2A"), (2.0 * u).toFloat(), 0.1f)

        // The window's reflection; each tile, set at its own tiny angle, catches it a little differently.
        val off = t.rot * 5200 * u + (rnd.nextDouble() - 0.5) * 6 * u
        val c = win.c0 + off
        val p0 = win.n * (c - win.half); val p1 = win.n * (c + win.half)
        val a = (0.2 * t.gloss).toFloat()
        b.fill(
            Path().polygon(corners),
            LinearFill(
                p0.x.toFloat(), p0.y.toFloat(), p1.x.toFloat(), p1.y.toFloat(),
                intArrayOf(
                    0, Colors.withAlpha(Colors.WHITE, a), Colors.withAlpha(Colors.WHITE, a * 0.3f),
                    Colors.withAlpha(Colors.WHITE, a * 0.75f), 0,
                ),
                floatArrayOf(0f, 0.36f, 0.5f, 0.62f, 1f),
            ),
        )

        // A chipped corner on the odd tile shows the fritware body.
        if (rnd.nextDouble() < 0.14) {
            val cn = corners[rnd.nextInt(4)]
            val inward = (Vec(t.cx, t.cy) - cn).normalized()
            val chip = blob(cn + inward * ((1.5 + rnd.nextDouble() * 2.5) * u), (2.5 + rnd.nextDouble() * 3.5) * u, rnd, 0.8 * u, 0.35)
            b.fill(Path().polygon(chip), gl.body, 0.95f)
            b.stroke(Path().polygon(chip), Colors.hex("#7A6A58"), (0.7 * u).toFloat(), 0.45f)
        }
    }

    // ------------------------------------------------------------------ compositions

    private fun design(ctx: RenderContext) = Design(ctx.u, max(0.7, 1.8 * ctx.u), max(0.9, 2.3 * ctx.u))

    /** Tile size near [target] design px that fits a whole number of tiles across the safe area. */
    private fun tileSize(ctx: RenderContext, target: Double): Double {
        val n = max(2, (ctx.safeW / (target * ctx.u)).roundToInt())
        return ctx.safeW / n
    }

    private inline fun cells(ctx: RenderContext, ox: Double, oy: Double, t: Double, f: (i: Int, j: Int, x: Double, y: Double) -> Unit) {
        val c0 = floor(-ox / t).toInt() - 1; val c1 = floor((ctx.w - ox) / t).toInt() + 1
        val r0 = floor(-oy / t).toInt() - 1; val r1 = floor((ctx.h - oy) / t).toInt() + 1
        for (j in r0..r1) for (i in c0..c1) f(i, j, ox + i * t, oy + j * t)
    }

    enum class Flower { TULIP, CARNATION }

    /**
     * The classic four-way tile: a rosette at the centre, flowers on the
     * diagonals, saz leaves swirling between them, and quarter rosettes in the
     * corners that join with the neighbouring tiles into whole flowers.
     */
    private fun fourWay(ctx: RenderContext, flower: Flower): Pair<Wall, Design> {
        val t = tileSize(ctx, 470.0)
        val ox = ctx.safeLeft
        val oy = ctx.cy - t / 2
        val d = design(ctx)
        cells(ctx, ox, oy, t) { i, j, x, y ->
            val rnd = Random(ctx.seed * 1000 + i * 37 + j * 101)
            val c = Vec(x + t / 2, y + t / 2)
            for (k in 0 until 4) {
                val dir = Vec.polar(1.0, k * PI / 2 + 0.03)
                d.leaf(Vec(x, y) + dir * (0.15 * t), Vec(x, y) + dir * (0.31 * t), 0.028 * t, if (k % 2 == 0) 0.12 else -0.12, Pig.GREEN)
            }
            d.rosette(Vec(x, y), 0.2 * t, 8, rnd, Pig.COBALT, Pig.TURQ, Pig.RED, rot = 0.2)
            d.blossom(Vec(x + t / 2, y), 0.065 * t, rnd, Pig.RED, Pig.SLIP)
            d.blossom(Vec(x, y + t / 2), 0.065 * t, rnd, Pig.RED, Pig.SLIP)
            for (k in 0 until 4) {
                val th = PI / 4 + k * PI / 2 - 0.22
                fun pt(r: Double, a: Double) = c + Vec.polar(r * t, th + a)
                // A stem springs from the centre in an S and carries the flower out towards the corner.
                val stem = bez(pt(0.08, -0.25), pt(0.16, -0.45), pt(0.18, 0.15), pt(0.25, 0.3), d.step)
                val (sa, _) = along(stem, 0.42)
                // A saz leaf branches off and sweeps towards the edge, a small leaf curls the other way.
                val sazEnd = pt(0.45, -0.68)
                d.saz(bez(sa, sa + Vec.polar(0.16 * t, th - 1.0), sazEnd + Vec.polar(0.14 * t, th + 0.9), sazEnd, d.step), 0.05 * t, Pig.GREEN, Pig.COBALT, phase = 0.2)
                val (sb, _) = along(stem, 0.62)
                d.leaf(sb, pt(0.35, 0.62), 0.026 * t, -0.2, Pig.GREEN)
                d.stem(stem, 0.016 * t)
                val (tip, tan) = along(stem, 1.0)
                when (flower) {
                    Flower.TULIP -> {
                        val f = Frame.toward(tip - tan * (0.012 * t), tan.angle, 0.0027 * t)
                        if (k % 2 == 0) d.tulip(f, rnd, Pig.RED, Pig.SLIP, Pig.COBALT) else d.tulip(f, rnd, Pig.COBALT, Pig.TURQ, Pig.RED)
                    }
                    Flower.CARNATION -> {
                        val f = Frame.toward(tip - tan * (0.012 * t), tan.angle, 0.0028 * t)
                        d.carnation(f, rnd, if (k % 2 == 0) Pig.RED else Pig.COBALT, if (k % 2 == 0) Pig.COBALT else Pig.TURQ, Pig.GREEN)
                    }
                }
            }
            d.rosette(c, 0.1 * t, 6, rnd, Pig.RED, Pig.SLIP, Pig.COBALT)
        }
        return Wall(ox, oy, t, 7 * ctx.u, ctx) to d
    }

    /** A small flowering sprig: a curving stalk, two leaves and a flower. [size] is its height in px. */
    private fun Design.sprig(base: Vec, dir: Double, size: Double, kind: Flower, rnd: Random, a: Pig, b: Pig) {
        val bend = (rnd.nextDouble() - 0.5) * 0.6
        val tip = base + Vec.polar(size * 0.4, dir + bend * 0.4)
        val stalk = bez(base, base + Vec.polar(size * 0.16, dir - bend), tip - Vec.polar(size * 0.14, dir + bend), tip, step)
        val (p1, _) = along(stalk, 0.25)
        val (p2, _) = along(stalk, 0.5)
        leaf(p1, p1 + Vec.polar(size * 0.3, dir - 1.05 + bend * 0.5), size * 0.05, 0.18, Pig.GREEN)
        leaf(p2, p2 + Vec.polar(size * 0.25, dir + 0.95 + bend * 0.5), size * 0.045, -0.18, Pig.GREEN)
        stem(stalk, size * 0.032)
        val (tp, tt) = along(stalk, 1.0)
        val f = Frame.toward(tp - tt * (size * 0.02), tt.angle, size * 0.6 / 100)
        when (kind) {
            Flower.TULIP -> tulip(f, rnd, a, Pig.SLIP, b)
            Flower.CARNATION -> carnation(f, rnd, a, b, Pig.GREEN)
        }
    }

    // ---- panels: a framed composition in the safe area, set into a plainer wall

    private class Panel(val left: Double, val top: Double, val cols: Int, val rows: Int, val t: Double) {
        val right get() = left + cols * t
        val bottom get() = top + rows * t
        fun hasCell(x: Double, y: Double) = x > left - t * 0.5 && x < right - t * 0.5 && y > top - t * 0.5 && y < bottom - t * 0.5
        fun touches(x: Double, y: Double) = x >= left - 1 && x <= right + 1 && y >= top - 1 && y <= bottom + 1
    }

    private fun panel(ctx: RenderContext, cols: Int = 3): Panel {
        val tall = ctx.safeH > ctx.safeW * 1.2
        val mTop = if (tall) 0.12 else 0.07
        val mBot = if (tall) 0.08 else 0.07
        val aw = ctx.safeW * 0.86
        val ah = ctx.safeH * (1 - mTop - mBot)
        val t = min(aw / cols, ah / 4)
        val rows = floor(ah / t + 1e-6).toInt()
        return Panel(ctx.cx - cols * t / 2, ctx.safeTop + ctx.safeH * mTop + (ah - rows * t) / 2, cols, rows, t)
    }

    private fun rectLoop(x0: Double, y0: Double, x1: Double, y1: Double, step: Double): List<Vec> =
        resample(listOf(Vec(x0, y0), Vec(x1, y0), Vec(x1, y1), Vec(x0, y1), Vec(x0, y0)), step).dropLast(1)

    /** The plain wall around a panel: quarter rosettes meeting at the tile corners, a sprig of blossom in the middle. */
    private fun Design.plainTile(x: Double, y: Double, t: Double, p: Panel, rnd: Random) {
        val c = Vec(x + t / 2, y + t / 2)
        if (!p.touches(x, y)) {
            for (k in 0 until 4) {
                val dir = Vec.polar(1.0, PI / 4 + k * PI / 2)
                leaf(Vec(x, y) + dir * (0.1 * t), Vec(x, y) + dir * (0.26 * t), 0.03 * t, if (k % 2 == 0) 0.15 else -0.15, Pig.GREEN)
            }
            rosette(Vec(x, y), 0.15 * t, 8, rnd, Pig.COBALT, Pig.TURQ, Pig.RED, rot = 0.2)
        }
        for (k in 0 until 4) {
            val a = k * PI / 2 + 0.3
            leaf(c + Vec.polar(0.05 * t, a), c + Vec.polar(0.2 * t, a + 0.25), 0.022 * t, 0.15, Pig.GREEN, vein = false)
        }
        blossom(c, 0.07 * t, rnd, Pig.RED, Pig.SLIP)
    }

    /** A border band: cobalt ground, a white scroll waving through it, flowers in its bays, a turquoise guard. */
    private fun Design.border(p: Panel, bw: Double, rnd: Random) {
        val x0 = p.left; val y0 = p.top; val x1 = p.right; val y1 = p.bottom
        val inner = rectLoop(x0 + bw, y0 + bw, x1 - bw, y1 - bw, step)
        areaLoops(listOf(rectLoop(x0, y0, x1, y1, step), inner.asReversed()), Pig.COBALT, occludes = false)
        val g = bw * 0.13
        areaLoops(listOf(inner, rectLoop(x0 + bw + g, y0 + bw + g, x1 - bw - g, y1 - bw - g, step).asReversed()), Pig.TURQ, occludes = false)
        val mid = rectLoop(x0 + bw / 2, y0 + bw / 2, x1 - bw / 2, y1 - bw / 2, step)
        val ring = mid + mid[0]
        val len = polyLen(ring)
        val n = max(4, ((len / (bw * 2.4)).roundToInt() / 2) * 2)
        val amp = bw * 0.2
        val wave = ArrayList<Vec>(ring.size)
        val normals = ArrayList<Vec>(ring.size)
        var dist = 0.0
        for (i in mid.indices) {
            if (i > 0) dist += (mid[i] - mid[i - 1]).length
            val dd = (mid[(i + 1) % mid.size] - mid[(i - 1 + mid.size) % mid.size]).normalized()
            val nrm = Vec(-dd.y, dd.x)
            normals += nrm
            wave += mid[i] + nrm * (amp * sin(2 * PI * n * dist / len))
        }
        // The scroll as a white ribbon: two closed loops either side of the wave.
        val w = bw * 0.035
        areaLoops(listOf(wave.indices.map { wave[it] + normals[it] * w }, wave.indices.map { wave[it] - normals[it] * w }.asReversed()), Pig.SLIP, occludes = false)
        // In each bay of the wave, alternately a rosette and a small hatayi.
        for (k in 0 until n) {
            val s = len * (k + 0.25) / n
            val (pt, tan) = along(ring, s / len)
            val side = if (k % 2 == 0) -1.0 else 1.0
            val nrm = Vec(-tan.y, tan.x) * side
            val at = pt + nrm * (bw * 0.12)
            if (k % 2 == 0) {
                rosette(at, bw * 0.2, 6, rnd, Pig.RED, Pig.SLIP, Pig.TURQ)
            } else {
                hatayi(Frame.toward(pt + nrm * (-bw * 0.06), nrm.angle, bw * 0.4 / 100), rnd, Pig.TURQ, Pig.RED, Pig.SLIP, Pig.TURQ)
            }
            // Small leaves where the scroll crosses the middle.
            val (lp, lt) = along(ring, (s + len / (4.0 * n)) / len)
            val ln = Vec(-lt.y, lt.x) * side
            leaf(lp, lp + (lt * 0.8 + ln * 0.6).normalized() * (bw * 0.3), bw * 0.06, 0.2, Pig.SLIP, vein = false)
        }
        // Corner rosettes hide where the scroll turns.
        for (cn in listOf(Vec(x0 + bw / 2, y0 + bw / 2), Vec(x1 - bw / 2, y0 + bw / 2), Vec(x1 - bw / 2, y1 - bw / 2), Vec(x0 + bw / 2, y1 - bw / 2))) {
            rosette(cn, bw * 0.4, 8, rnd, Pig.TURQ, Pig.SLIP, Pig.RED)
        }
    }

    /**
     * Vazo kompozisyonu: from a vase, a main stem rises to a hatayi and throws
     * off branches to either side, each ending in a tulip, carnation, rose or
     * hyacinth; long saz leaves arch behind them. [halfWidth] gives the room
     * available either side of the centre line at a height (for arches).
     */
    private fun Design.bouquet(x0: Double, y0: Double, w: Double, h: Double, rnd: Random, halfWidth: (Double) -> Double = { w / 2 }) {
        fun pt(a: Double, b: Double) = Vec(x0 + a * w, y0 + b * h)
        val cxv = x0 + w / 2
        val sz = min(w, h * 0.62)
        val vaseH = min(0.17 * h, 0.5 * w)
        val vf = Frame(pt(0.5, 0.995), 0.0, vaseH / 100)
        val mouth = vf.p(0, -96)
        val deg = PI / 180
        val hatH = 0.4 * sz
        val hb = Vec(cxv, y0 + hatH + 0.02 * h)
        val reach = mouth.y - hb.y
        val main = bez(mouth, mouth + Vec(0.06 * w, -0.35 * reach), hb + Vec(-0.07 * w, 0.35 * reach), hb, step)

        // Long saz leaves behind everything: one rising up the left, one arching over and drooping to the right.
        saz(
            bez(mouth + Vec(-0.02 * w, 0.0), mouth + Vec(-0.2 * w, -0.25 * reach), Vec(x0 - 0.02 * w, mouth.y - 0.5 * reach), Vec(x0 + 0.08 * w, mouth.y - 0.7 * reach), step),
            0.085 * sz, Pig.GREEN, Pig.COBALT, asym = 0.85,
        )
        saz(
            bez(mouth + Vec(0.02 * w, 0.0), Vec(cxv + 0.25 * w, mouth.y - 0.45 * reach), Vec(x0 + w * 1.02, mouth.y - 0.35 * reach), Vec(x0 + 0.92 * w, mouth.y - 0.08 * reach), step),
            0.075 * sz, Pig.GREEN, Pig.TURQ, asym = 1.1, phase = 0.4,
        )

        // Branches, alternating sides, from low on the main stem to near the top.
        val lowest = 0.16 + 0.32 * sz / reach.coerceAtLeast(1.0)
        val n = max(3, ((reach * (0.84 - lowest)) / (0.135 * sz)).toInt() + 1)
        class Branch(val path: List<Vec>, val side: Double, val kind: Int)
        val branches = ArrayList<Branch>()
        for (k in 0 until n) {
            val f = lowest + (0.84 - lowest) * k / (n - 1).toDouble()
            val (b0, _) = along(main, f)
            val side = if (k % 2 == 0) -1.0 else 1.0
            val ty = b0.y - 0.16 * sz
            val room = halfWidth(ty)
            val dx = (min(0.27 * w, room - 0.2 * sz)).coerceAtLeast(0.1 * w) * (0.9 + rnd.nextDouble() * 0.15) * (if ((k / 2) % 2 == 0) 1.0 else 0.62)
            val target = Vec(cxv + side * dx, ty)
            val dir = -90 * deg + side * (40 + rnd.nextDouble() * 18) * deg
            val path = bez(b0, b0 + Vec(side * 0.12 * w, 0.02 * sz), target - Vec.polar(0.14 * sz, dir), target, step)
            branches += Branch(path, side, (k + (k / 2)) % 5)
        }
        // Hyacinths lean out low from the vase mouth.
        val hySize = min(0.5 * sz, reach * lowest * 1.2)
        // Leaves first, then stems, then flowers over them.
        for (b in branches) {
            val (lp, lt) = along(b.path, 0.45)
            leaf(lp, lp + (lt * 0.6 + Vec(0.0, -0.8)).normalized() * (0.15 * sz), 0.028 * sz, 0.2 * b.side, Pig.GREEN)
        }
        for (b in branches) stem(b.path, 0.02 * sz)
        stem(main, 0.028 * sz)
        hyacinth(Frame.toward(mouth + Vec(-0.025 * w, 0.0), -134 * deg, hySize / 115), rnd, Pig.COBALT, bend = -14.0, count = 8)
        hyacinth(Frame.toward(mouth + Vec(0.025 * w, 0.0), -46 * deg, hySize * 0.95 / 115), rnd, Pig.COBALT, bend = 14.0, count = 7)
        for (b in branches) {
            val (p, tan) = along(b.path, 1.0)
            val at = p - tan * (0.01 * sz)
            val a = tan.angle
            when (b.kind) {
                0 -> tulip(Frame.toward(at, a, 0.27 * sz / 100), rnd, Pig.RED, Pig.SLIP, Pig.COBALT)
                1 -> carnation(Frame.toward(at, a, 0.27 * sz / 100), rnd, Pig.RED, Pig.COBALT, Pig.GREEN)
                2 -> rosette(p + tan * (0.085 * sz), 0.1 * sz, 7, rnd, Pig.COBALT, Pig.TURQ, Pig.RED)
                3 -> tulip(Frame.toward(at, a, 0.26 * sz / 100), rnd, Pig.COBALT, Pig.TURQ, Pig.RED)
                else -> carnation(Frame.toward(at, a, 0.25 * sz / 100), rnd, Pig.COBALT, Pig.TURQ, Pig.GREEN)
            }
        }
        hatayi(Frame.toward(hb + Vec(0.0, 0.01 * sz), -90 * deg, hatH / 100), rnd, Pig.RED, Pig.COBALT, Pig.TURQ)
        // Prunus sprays either side of the hatayi.
        for (m in doubleArrayOf(-1.0, 1.0)) {
            val (sp, _) = along(main, 0.93)
            val c = Vec(cxv + m * min(0.22 * w, halfWidth(hb.y) - 0.05 * sz), hb.y - 0.12 * sz)
            line(bez(sp, sp + Vec(m * 0.06 * w, -0.02 * sz), c + Vec(-m * 0.04 * w, 0.04 * sz), c, step), 0.6)
            blossom(c, 0.045 * sz, rnd, Pig.SLIP, Pig.RED)
            val (bp, _) = along(bez(sp, sp + Vec(m * 0.06 * w, -0.02 * sz), c + Vec(-m * 0.04 * w, 0.04 * sz), c, step), 0.5)
            blossom(bp + Vec(0.0, 0.035 * sz), 0.025 * sz, rnd, Pig.RED, Pig.SLIP)
        }
        vase(vf, rnd)
    }

    /** An Ottoman vase: cobalt body, turquoise bands, white reserve flowers. */
    fun Design.vase(f: Frame, rnd: Random) {
        val prof = listOf(0.0 to 15.0, -4.0 to 14.0, -8.0 to 9.0, -12.0 to 13.0, -22.0 to 25.0, -36.0 to 32.0, -50.0 to 30.0, -61.0 to 21.0, -69.0 to 11.0, -78.0 to 8.5, -87.0 to 9.5, -94.0 to 14.0, -99.0 to 17.0)
        val right = spline(prof.map { f.p(it.second + (rnd.nextDouble() - 0.5) * 0.8, it.first) }, step)
        val left = spline(prof.map { f.p(-it.second + (rnd.nextDouble() - 0.5) * 0.8, it.first) }, step)
        area(right + left.asReversed(), Pig.COBALT)
        fun hw(y: Double): Double {
            for (i in 1 until prof.size) if (prof[i].first <= y) {
                val (y0, w0) = prof[i - 1]; val (y1, w1) = prof[i]
                return w0 + (w1 - w0) * (y - y0) / (y1 - y0)
            }
            return prof.last().second
        }
        fun band(ya: Double, yb: Double, pig: Pig) {
            val pts = ArrayList<Vec>()
            for (k in 0..12) { val x = -hw(ya) + 2 * hw(ya) * k / 12; pts += f.p(x, ya + 1.5 * sin(PI * k / 12)) }
            for (k in 12 downTo 0) { val x = -hw(yb) + 2 * hw(yb) * k / 12; pts += f.p(x, yb + 1.5 * sin(PI * k / 12)) }
            area(pts, pig)
        }
        band(-58.0, -65.0, Pig.TURQ)
        band(-93.0, -98.0, Pig.TURQ)
        band(-8.5, -12.0, Pig.TURQ)
        blossom(f.p(0, -38), 9 * f.s, rnd, Pig.SLIP, Pig.RED)
        blossom(f.p(-19, -34), 6 * f.s, rnd, Pig.SLIP, Pig.RED)
        blossom(f.p(19, -34), 6 * f.s, rnd, Pig.SLIP, Pig.RED)
        for (x in listOf(-14.0, 0.0, 14.0)) area(blob(f.p(x, -20), 2.2 * f.s, rnd, step), Pig.SLIP)
    }

    private fun vasePanel(ctx: RenderContext, arch: Boolean): Pair<Wall, Design> {
        val p = panel(ctx)
        val t = p.t
        val d = design(ctx)
        val rnd = Random(ctx.seed * 31 + 7)
        cells(ctx, p.left, p.top, t) { i, j, x, y ->
            if (!p.hasCell(x, y)) d.plainTile(x, y, t, p, Random(ctx.seed * 1000 + i * 37 + j * 101))
        }
        val bw = 0.3 * t
        d.border(p, bw, rnd)
        val g = bw * 1.13
        val fx0 = p.left + g; val fy0 = p.top + g; val fx1 = p.right - g; val fy1 = p.bottom - g
        val fw = fx1 - fx0
        if (!arch) {
            d.bouquet(fx0 + fw * 0.04, fy0 + fw * 0.03, fw * 0.92, fy1 - fy0 - fw * 0.05, rnd)
        } else {
            // A pointed arch: two arcs of radius 0.75 x the span meeting at the apex.
            val rho = 0.75 * fw
            val apexH = sqrt(rho * rho - (rho - fw / 2) * (rho - fw / 2))
            val ys = fy0 + fw * 0.05 + apexH
            val cl = Vec(fx0 + rho, ys); val cr = Vec(fx1 - rho, ys)
            val aTop = atan2(-apexH, fw / 2 - rho)
            val leftArc = (0..60).map { k -> cl + Vec.polar(rho, PI + (aTop + 2 * PI - PI) * k / 60.0) }
            val rightArc = (0..60).map { k -> cr + Vec.polar(rho, -PI - aTop + (aTop + PI) * k / 60.0) }
            // Spandrels: cobalt, each with a rosette and rumi leaves in white.
            d.area(listOf(Vec(fx0, fy0)) + leftArc.asReversed().dropLast(0) + listOf(Vec(fx0, fy0)), Pig.COBALT)
            d.area(listOf(Vec(fx1, fy0)) + rightArc + listOf(Vec(fx1, fy0)), Pig.COBALT)
            for (m in doubleArrayOf(-1.0, 1.0)) {
                val cx = if (m < 0) fx0 + fw * 0.13 else fx1 - fw * 0.13
                val cy = fy0 + (ys - fy0) * 0.22
                d.rosette(Vec(cx, cy), fw * 0.06, 6, rnd, Pig.SLIP, Pig.TURQ, Pig.RED)
                d.rumi(Frame.toward(Vec(cx, cy) + Vec(-m * fw * 0.02, fw * 0.06), PI / 2 - m * 0.5, fw * 0.0011, mirror = m > 0), Pig.TURQ)
                d.rumi(Frame.toward(Vec(cx, cy) + Vec(m * fw * 0.06, fw * 0.0), -m * 0.2, fw * 0.001, mirror = m < 0), Pig.SLIP)
            }
            // The arch's own band, turquoise between dark lines.
            val bandW = fw * 0.012
            d.area(ribbon(leftArc, d.step, { bandW }).loop, Pig.TURQ)
            d.area(ribbon(rightArc, d.step, { bandW }).loop, Pig.TURQ)
            d.blossom(leftArc.last() + Vec(0.0, -bandW * 0.5), fw * 0.03, rnd, Pig.RED, Pig.SLIP)
            val by = ys - apexH * 0.8
            d.bouquet(fx0 + fw * 0.06, by, fw * 0.88, fy1 - by - fw * 0.02, rnd) { y ->
                if (y >= ys) fw / 2 else {
                    val dy = ys - y
                    (sqrt((rho * rho - dy * dy).coerceAtLeast(0.0)) - (rho - fw / 2)).coerceAtLeast(0.0) - fw * 0.03
                }
            }
        }
        return Wall(p.left, p.top, t, 7 * ctx.u, ctx) to d
    }

    /** Lale bahçesi: a border band around a field of scattered tulip and carnation sprigs. */
    private fun garden(ctx: RenderContext): Pair<Wall, Design> {
        val p = panel(ctx)
        val t = p.t
        val d = design(ctx)
        val rnd = Random(ctx.seed * 31 + 3)
        cells(ctx, p.left, p.top, t) { i, j, x, y ->
            if (!p.hasCell(x, y)) d.plainTile(x, y, t, p, Random(ctx.seed * 1000 + i * 37 + j * 101))
        }
        val bw = 0.3 * t
        val g = bw * 1.13
        val fx0 = p.left + g; val fy0 = p.top + g; val fx1 = p.right - g; val fy1 = p.bottom - g
        val fw = fx1 - fx0; val fh = fy1 - fy0
        val cols = 3
        val gx = fw / cols
        val gy = gx * 1.0
        val rows = max(2, (fh / gy).toInt())
        val gy2 = fh / rows
        val size = min(gx * 1.25, gy2 * 1.1)
        for (r in 0 until rows) {
            val off = if (r % 2 == 0) 0.25 else 0.75
            val n = if (r % 2 == 0) cols else cols - 1
            for (c in 0 until n) {
                val base = Vec(fx0 + gx * (c + off) + (rnd.nextDouble() - 0.5) * gx * 0.08, fy0 + gy2 * (r + 0.97))
                val dir = -PI / 2 + (rnd.nextDouble() - 0.5) * 0.45
                val kind = if ((r + c) % 2 == 0) Flower.TULIP else Flower.CARNATION
                val (a, b) = if ((r * 3 + c) % 3 == 0) Pig.COBALT to Pig.TURQ else Pig.RED to Pig.COBALT
                d.sprig(base, dir, size, kind, rnd, a, b)
            }
            // Little blossoms in the gaps.
            for (c in 0..n) {
                val bx = fx0 + gx * (c + off - 0.5)
                if (bx < fx0 + gx * 0.15 || bx > fx1 - gx * 0.15) continue
                d.blossom(Vec(bx, fy0 + gy2 * (r + 0.32)), gx * 0.06, rnd, Pig.RED, Pig.SLIP)
            }
        }
        d.border(p, bw, rnd)
        return Wall(p.left, p.top, t, 7 * ctx.u, ctx) to d
    }

    /** Saz yolu: a vine climbing in steps, a great hatayi at every rise and saz leaves sweeping from it. */
    private fun sazVine(ctx: RenderContext): Pair<Wall, Design> {
        val t = tileSize(ctx, 430.0)
        val ox = ctx.safeLeft
        val oy = ctx.cy - t / 2
        val d = design(ctx)
        cells(ctx, ox, oy, t) { i, j, x, y ->
            val rnd = Random(ctx.seed * 1000 + i * 37 + j * 101)
            fun pt(a: Double, b: Double) = Vec(x + a * t, y + b * t)
            val vine = bez(pt(-0.08, 1.08), pt(0.34, 1.0), pt(0.46, 0.16), pt(0.92, 0.08), d.step)
            fun on(s: Double): Pair<Vec, Vec> = along(vine, s)
            // Big saz on the right of the rise, a smaller one crossing over it.
            val (s1, _) = on(0.42)
            d.saz(bez(s1, s1 + Vec(0.14 * t, 0.14 * t), s1 + Vec(0.4 * t, 0.12 * t), s1 + Vec(0.45 * t, -0.06 * t), d.step), 0.09 * t, Pig.GREEN, Pig.COBALT, phase = 0.3)
            val (s2, _) = on(0.6)
            d.saz(bez(s2, s2 + Vec(0.16 * t, 0.0), s2 + Vec(0.22 * t, 0.2 * t), s2 + Vec(0.1 * t, 0.3 * t), d.step), 0.06 * t, Pig.TURQ, null, asym = 0.8)
            d.stem(vine, 0.026 * t)
            // The hatayi faces out from the left of the rise.
            val (h0, ht) = on(0.5)
            val left = Vec(ht.y, -ht.x)
            d.hatayi(Frame.toward(h0 + left * (0.012 * t), left.angle + 0.35, 0.0045 * t), rnd, Pig.RED, Pig.COBALT, Pig.TURQ)
            // A tulip on a short stalk below, a rosette above.
            val (b0, bt) = on(0.16)
            val up = Vec(bt.y, -bt.x)
            val tip = b0 + (up * 0.9 + bt * -0.4).normalized() * (0.1 * t)
            val st = bez(b0, b0 + up * (0.05 * t), tip - (tip - b0).normalized() * (0.03 * t), tip, d.step)
            d.leaf(b0 + up * (0.01 * t), b0 + (up * 0.4 + bt).normalized() * (0.15 * t), 0.022 * t, -0.2, Pig.GREEN)
            d.stem(st, 0.012 * t)
            d.tulip(Frame.toward(tip, (tip - b0).angle, 0.0026 * t), rnd, Pig.COBALT, Pig.TURQ, Pig.RED)
            val (r0, rt) = on(0.84)
            val down = Vec(-rt.y, rt.x)
            d.leaf(r0, r0 + (down + rt * 0.6).normalized() * (0.13 * t), 0.02 * t, 0.2, Pig.GREEN)
            d.rosette(r0 + down * (0.1 * t) + rt * (-0.02 * t), 0.085 * t, 6, rnd, Pig.COBALT, Pig.SLIP, Pig.RED)
            d.blossom(pt(0.12, 0.4), 0.045 * t, rnd, Pig.RED, Pig.SLIP)
        }
        return Wall(ox, oy, t, 7 * ctx.u, ctx) to d
    }

    /** Rumi scroll: a white stem waving across a cobalt ground, throwing off spiral tendrils with split leaves. */
    private fun rumiScroll(ctx: RenderContext): Pair<Wall, Design> {
        val t = tileSize(ctx, 420.0)
        val ox = ctx.safeLeft
        val oy = ctx.cy - t / 2
        val d = design(ctx)
        val m = 0.5 * t
        d.area(listOf(Vec(-m, -m), Vec(ctx.w + m, -m), Vec(ctx.w + m, ctx.h + m), Vec(-m, ctx.h + m)), Pig.COBALT, outline = false, occludes = false)
        cells(ctx, ox, oy, t) { i, j, x, y ->
            val rnd = Random(ctx.seed * 1000 + i * 37 + j * 101)
            fun pt(a: Double, b: Double) = Vec(x + a * t, y + b * t)
            val wave = (0..48).map { k -> val a = k / 48.0; pt(a - 0.01, 0.5 + 0.17 * sin(2 * PI * a)) } + listOf(pt(1.02, 0.5 + 0.17 * sin(2 * PI * 1.02)))
            for (dirSign in doubleArrayOf(1.0, -1.0)) {
                // dirSign 1: tendril curls down from the wave's lowest point; -1: up from its highest.
                val a0 = if (dirSign > 0) 0.25 else 0.75
                val start = pt(a0 + 0.05 * dirSign, 0.5 + 0.17 * sin(2 * PI * (a0 + 0.05 * dirSign)))
                val c = start + Vec(-0.08 * t * dirSign, 0.22 * t * dirSign)
                val th0 = (start - c).angle
                val r0 = (start - c).length
                val turns = 1.25 * 2 * PI
                val spiral = (0..90).map { k ->
                    val ph = turns * k / 90.0
                    c + Vec.polar(r0 * (1 - 0.72 * ph / turns), th0 + dirSign * ph)
                }
                d.area(ribbon(spiral, d.step, { s -> 0.022 * t * (1 - 0.5 * s) }).loop, Pig.SLIP)
                for ((q, at) in listOf(0.22, 0.5, 0.74).withIndex()) {
                    val (lp, lt) = along(spiral, at)
                    val out = (lp - c).normalized()
                    val size = 0.0024 * t * (1 - 0.3 * at)
                    d.rumi(Frame.toward(lp, (out * 0.7 + lt * 0.7).angle, size, mirror = dirSign < 0), if (q % 2 == 0) Pig.TURQ else Pig.SLIP)
                }
                val end = spiral.last()
                d.rosette(end + (c - end) * 0.3, 0.075 * t, 6, rnd, Pig.RED, Pig.SLIP, Pig.TURQ)
            }
            d.area(ribbon(wave, d.step, { 0.024 * t }).loop, Pig.SLIP)
            // Split leaves where the stem crosses the middle.
            for (a in doubleArrayOf(0.0, 0.5)) {
                val (lp, lt) = along(wave, a + 0.01)
                val upDown = if (a == 0.0) -1.0 else 1.0
                d.rumi(Frame.toward(lp, lt.angle + upDown * 0.9, 0.0017 * t, mirror = a > 0), Pig.TURQ)
                d.blossom(lp + Vec(0.0, 0.13 * t * -upDown), 0.045 * t, rnd, Pig.RED, Pig.SLIP)
            }
        }
        return Wall(ox, oy, t, 7 * ctx.u, ctx) to d
    }

    /** Sümbül ve gül: hyacinths and roses in a half-drop repeat. */
    private fun hyacinthRose(ctx: RenderContext): Pair<Wall, Design> {
        val t = tileSize(ctx, 380.0)
        val ox = ctx.safeLeft
        val oy = ctx.cy - t / 2
        val d = design(ctx)
        cells(ctx, ox, oy, t) { i, j, x, y ->
            val rnd = Random(ctx.seed * 1000 + i * 37 + j * 101)
            // Half-drop: every other column of the design sits half a tile lower.
            val drop = if (Math.floorMod(i, 2) == 1) 0.5 * t else 0.0
            fun pt(a: Double, b: Double) = Vec(x + a * t, y + b * t + drop)
            // Hyacinth with two long leaves at its foot.
            val hb = pt(0.26, 0.97)
            d.leaf(hb, pt(0.06, 0.58), 0.04 * t, 0.25, Pig.GREEN)
            d.leaf(hb, pt(0.46, 0.66), 0.036 * t, -0.25, Pig.GREEN)
            d.hyacinth(Frame.toward(hb, -PI / 2 + 0.08, 0.0068 * t), rnd, Pig.COBALT, bend = 12.0, count = 8)
            // A rose on a leafy stalk.
            val rb = pt(0.78, 0.92)
            val rc = pt(0.74, 0.44)
            val st = bez(rb, pt(0.82, 0.75), pt(0.7, 0.62), rc, d.step)
            d.leaf(along(st, 0.4).first, pt(0.98, 0.6), 0.042 * t, 0.2, Pig.GREEN)
            d.leaf(along(st, 0.6).first, pt(0.53, 0.58), 0.038 * t, -0.25, Pig.GREEN)
            d.stem(st, 0.02 * t)
            d.rosette(rc, 0.14 * t, 7, rnd, Pig.RED, Pig.SLIP, Pig.COBALT)
            d.blossom(pt(0.5, 0.12), 0.05 * t, rnd, Pig.TURQ, Pig.RED)
            d.blossom(pt(0.02, 0.36), 0.045 * t, rnd, Pig.RED, Pig.SLIP)
        }
        return Wall(ox, oy, t, 7 * ctx.u, ctx) to d
    }

    enum class Kind { LALE, KARANFIL, SAZ, RUMI, VAZO, KEMER, BAHCE, SUMBUL }

    fun scene(ctx: RenderContext, kind: Kind): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val gl = glaze(ctx.palette)
        val (wall, d) = when (kind) {
            Kind.LALE -> fourWay(ctx, Flower.TULIP)
            Kind.KARANFIL -> fourWay(ctx, Flower.CARNATION)
            Kind.SAZ -> sazVine(ctx)
            Kind.RUMI -> rumiScroll(ctx)
            Kind.VAZO -> vasePanel(ctx, arch = false)
            Kind.KEMER -> vasePanel(ctx, arch = true)
            Kind.BAHCE -> garden(ctx)
            Kind.SUMBUL -> hyacinthRose(ctx)
        }
        paint(b, ctx, gl, wall, d)
        return b.build()
    }

    private fun entry(id: String, title: String, palette: String, seed: Int, kind: Kind) =
        Entry(id, title, Category.CINI, Palette.byId(palette), seed) { ctx -> scene(ctx, kind) }

    fun entries(): List<Entry> = listOf(
        entry("cini-vazo", "Vazoda Bahar", "iznik", 3, Kind.VAZO),
        entry("cini-lale", "Lale Çinisi", "iznik", 3, Kind.LALE),
        entry("cini-saz-yolu", "Saz Yolu", "emerald", 4, Kind.SAZ),
        entry("cini-kemer-vazo", "Kemer Altında Vazo", "betul", 6, Kind.KEMER),
        entry("cini-rumi", "Rumi Kıvrımları", "lapis", 2, Kind.RUMI),
        entry("cini-lale-bahcesi", "Lale Bahçesi", "isfahan", 9, Kind.BAHCE),
        entry("cini-karanfil", "Karanfil Çinisi", "amethyst", 5, Kind.KARANFIL),
        entry("cini-sumbul-gul", "Sümbül ve Gül", "alhambra", 8, Kind.SUMBUL),
    )
}
