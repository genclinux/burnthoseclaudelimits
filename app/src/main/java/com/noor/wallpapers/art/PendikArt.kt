package com.noor.wallpapers.art

import com.noor.wallpapers.prayer.Qibla
import com.noor.wallpapers.prayer.TurkishText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pendik: her home on the Marmara shore. Looking west from the promenade the
 * Princes' Islands (Büyükada, Heybeliada, Burgazada, Kınalıada and little
 * Sedef) lie low on the water, the sun goes down behind them, ferries cross,
 * gulls follow, and the marina's masts stand in rows by the breakwater light.
 * Behind the town Aydos, the highest hill on the Anatolian side, looks down
 * on all of it.
 */
object PendikArt {
    const val LAT = 40.8769
    const val LON = 29.2346

    enum class Kind { SUNSET, DAWN, NIGHT }

    fun entries(): List<Entry> {
        val c = Category.PENDIK
        fun e(id: String, title: String, palette: String, seed: Int, alsoIn: Set<Category> = emptySet(), text: Boolean = false, build: (RenderContext) -> Scene) =
            Entry("pendik-$id", title, c, Palette.byId(palette), seed, editableText = text, alsoIn = alsoIn, builder = build)
        // Each also shows under the kind of art it is, so every tab has a little of Pendik in it.
        return listOf(
            e("adalar-gun-batimi", "Adalar'a Gün Batımı", "marmara", 5, setOf(Category.SULUBOYA)) { PendikPainting(it, Kind.SUNSET).paint() },
            e("marina-safak", "Pendik Marina'da Şafak", "betul", 3, setOf(Category.SULUBOYA)) { PendikPainting(it, Kind.DAWN).paint() },
            e("sahil-gece", "Pendik Sahili · Gece", "lapis", 7, setOf(Category.SULUBOYA, Category.NIGHT)) { PendikPainting(it, Kind.NIGHT).paint() },
            e("vapur", "Martılar ve Vapur", "marmara", 4) { VapurPoster.scene(it) },
            e("aydos", "Aydos'tan Pendik", "emerald", 6, setOf(Category.NIGHT)) { AydosNight.scene(it) },
            e("mahya", "Pendik'te Mahya", "lapis", 2, setOf(Category.RAMADAN), text = true) { MahyaArt.scene(it) },
            e("kible", "Pendik'ten Kıble", "betul", 1, setOf(Category.GEOMETRIC)) { PendikQibla.scene(it) },
            e("gemiler", "Denizdeki Gemiler · Rahmân 55:24", "marmara", 8, setOf(Category.LEVHA, Category.CALLIGRAPHY)) { ctx ->
                LevhaArt.scene(ctx, LevhaArt.Params(SHIPS, CalligraphyArt.Script.NASKH, Ebru.Style.GELGIT, wide = true))
            },
            e("marmara-ebru", "Marmara Dalgaları", "marmara", 11, setOf(Category.EBRU)) { ctx -> EbruArt.scene(ctx, EbruArt.Params(Ebru.Style.GELGIT)) },
            e("marmara-cini", "Marmara Çinisi", "marmara", 5, setOf(Category.CINI)) { ctx -> CiniArt.scene(ctx, CiniArt.Kind.SAZ) },
        )
    }

    /** Rahmân 55:24, for a town of ferries. */
    val SHIPS = Phrase(
        "gemiler", "وَلَهُ الْجَوَارِ الْمُنْشَآتُ فِي الْبَحْرِ كَالْأَعْلَامِ", "وله الجوار المنشآت في البحر كالأعلام",
        "Ve lehü'l-cevâri'l-münşeâtü fi'l-bahri ke'l-a'lâm", "Denizde dağlar gibi yükselen gemiler de O'nundur · Rahmân 55:24",
    )

    /**
     * The islands as seen from Pendik's shore: (centre as a fraction of the
     * safe width, width, height, hump positions), nearest and largest first.
     */
    class Island(val name: String, val x: Double, val w: Double, val h: Double, val humps: List<Double>)

    val ISLANDS = listOf(
        Island("Sedef", 0.08, 120.0, 26.0, listOf(0.5)),
        Island("Büyükada", 0.33, 560.0, 104.0, listOf(0.32, 0.72)),
        Island("Heybeliada", 0.66, 400.0, 82.0, listOf(0.38, 0.74)),
        Island("Burgazada", 0.88, 210.0, 76.0, listOf(0.5)),
        Island("Kınalıada", 1.06, 180.0, 46.0, listOf(0.45)),
    )

    /** An island's outline standing on [horizon]: gentle humps, tapering into the water. */
    fun islandPath(ctx: RenderContext, isl: Island, horizon: Double, scale: Double = 1.0, seed: Int = 1): Path {
        val u = ctx.u * scale
        val cx = ctx.x(isl.x)
        val w = isl.w * u
        val n = Noise(seed + isl.name.hashCode())
        val pts = ArrayList<Vec>()
        val steps = 48
        for (i in 0..steps) {
            val t = i / steps.toDouble()
            var y = 0.0
            for (hp in isl.humps) y += exp(-((t - hp) / 0.2).pow(2))
            y = (y / isl.humps.size.coerceAtMost(2)).coerceAtMost(1.0)
            val taper = sin(PI * t).pow(0.6)
            val rough = 0.9 + 0.2 * n.at(t * 6, 0.5)
            pts += Vec(cx - w / 2 + w * t, horizon - isl.h * u * y * taper * rough)
        }
        pts += Vec(cx + w / 2, horizon + 2 * ctx.u)
        pts += Vec(cx - w / 2, horizon + 2 * ctx.u)
        return Path().polygon(pts)
    }

    /** A Bosphorus-style ferry facing left, waterline at ([x], [y]), [len] long. Returns (body, windows, funnel). */
    fun vapur(x: Double, y: Double, len: Double): Triple<Path, Path, Path> {
        val l = len
        val body = Path()
        body.moveTo(x - l * 0.5, y - l * 0.1).lineTo(x + l * 0.47, y - l * 0.1)
            .lineTo(x + l * 0.5, y - l * 0.12).lineTo(x + l * 0.46, y).lineTo(x - l * 0.44, y).close()
        body.rect(x - l * 0.34, y - l * 0.19, x + l * 0.4, y - l * 0.1)
        body.rect(x - l * 0.2, y - l * 0.26, x + l * 0.3, y - l * 0.19)
        body.rect(x - l * 0.29, y - l * 0.245, x - l * 0.16, y - l * 0.19)
        body.rect(x - l * 0.215, y - l * 0.39, x - l * 0.205, y - l * 0.26)
        val funnel = Path().moveTo(x + l * 0.03, y - l * 0.26).lineTo(x + l * 0.06, y - l * 0.37)
            .lineTo(x + l * 0.14, y - l * 0.37).lineTo(x + l * 0.13, y - l * 0.26).close()
        body.append(funnel)
        val windows = Path()
        var wx = x - l * 0.3
        while (wx < x + l * 0.36) {
            windows.rect(wx, y - l * 0.165, wx + l * 0.025, y - l * 0.125)
            wx += l * 0.045
        }
        wx = x - l * 0.16
        while (wx < x + l * 0.27) {
            windows.rect(wx, y - l * 0.24, wx + l * 0.02, y - l * 0.21)
            wx += l * 0.05
        }
        return Triple(body, windows, funnel)
    }

    /** A moored sailboat facing [dir]: hull and a mast line. */
    fun sailboatHull(x: Double, y: Double, len: Double, dir: Double): Path =
        Path().moveTo(x - dir * len * 0.5, y - len * 0.1).lineTo(x + dir * len * 0.5, y - len * 0.12)
            .quadTo(x + dir * len * 0.42, y, x + dir * len * 0.2, y + len * 0.02)
            .lineTo(x - dir * len * 0.38, y + len * 0.02).quadTo(x - dir * len * 0.48, y - len * 0.02, x - dir * len * 0.5, y - len * 0.1).close()
}

// ======================================================= watercolour Pendik

/** Pendik's shore painted the same way as the Suluboya mosques: washes, lifts and a few brush lines. */
private class PendikPainting(val ctx: RenderContext, val kind: PendikArt.Kind) {
    val p = SuluboyaArt.pigments(ctx.palette)
    val u = ctx.u
    val W = ctx.w
    val H = ctx.h
    val rnd = ctx.random(5151)
    val sheet = WetSheet(ctx, p.paper)
    val b = SceneBuilder(ctx.width, ctx.height)
    val hand = Hand(b, ctx, ctx.seed)
    val pad = 60 * u
    val horizon = ctx.y(if (kind == PendikArt.Kind.DAWN) 0.5 else 0.56)
    val night = kind == PendikArt.Kind.NIGHT
    var sunX = ctx.x(0.5)
    var moon: Vec? = null

    fun paint(): Scene {
        sky()
        islands()
        sea()
        when (kind) {
            PendikArt.Kind.SUNSET -> { shoreMosque(); ferry(ctx.x(0.62), horizon + (H - horizon) * 0.16, 330 * u); promenade() }
            PendikArt.Kind.DAWN -> marina()
            PendikArt.Kind.NIGHT -> { shoreMosque(); ferry(ctx.x(0.36), horizon + (H - horizon) * 0.13, 280 * u); promenade() }
        }
        b.items.add(0, sheet.toRaster())
        finishing()
        return b.build()
    }

    private fun down(y: Double) = (y / horizon).coerceIn(0.0, 1.0)

    private fun sky() {
        val hz = horizon
        val bloomsAt = { n: Int, y0: Double, y1: Double ->
            (0 until n).map { Bloom(rnd.nextDouble() * W, y0 + rnd.nextDouble() * (y1 - y0), (40 + rnd.nextDouble() * 70) * u, 0.6 + rnd.nextDouble() * 0.4) }
        }
        when (kind) {
            PendikArt.Kind.SUNSET -> {
                // Warm low down, cool high up, and only a narrow overlap where they meet as mauve
                // (the two laid over each other everywhere would go to mud).
                sheet.wash(rectP(-pad, hz * 0.3, W + pad, hz + 30 * u), p.glow, p.rose, 0.95, soft = 40.0, hard = 0.3, rag = 0.8, edge = 0.25, gran = 0.15,
                    mix = { _, y -> 1 - smooth(0.55, 1.0, down(y)) }, grade = { _, y -> 0.35 + 0.85 * smooth(0.3, 1.0, down(y)) })
                sheet.wash(rectP(-pad, -pad, W + pad, hz * 0.52), p.sky, p.shade, 0.55, soft = 50.0, hard = 0.38, rag = 1.1, edge = 0.5, gran = 0.5,
                    mix = { _, y -> smooth(0.1, 0.5, down(y)) }, grade = { _, y -> 1.1 - 1.4 * down(y) }, blooms = bloomsAt(2, hz * 0.15, hz * 0.4))
                for (k in 0 until 3) {
                    val y = hz * (0.42 + k * 0.15)
                    sheet.wash(cloud(y, (26 + k * 12) * u), p.rose, p.earth, 0.42, soft = 10.0, hard = 0.16, rag = 1.0, edge = 1.0, gran = 0.25)
                }
                // The sun going down just above Büyükada's hills.
                sunX = ctx.x(0.42)
                val sy = hz - 170 * u
                sheet.lift(Path().circle(sunX, sy, 190 * u), 0.45, soft = 50.0, hard = 0.45, rag = 0.8)
                sheet.lift(Path().circle(sunX, sy, 62 * u), 0.95, soft = 3.0, hard = 0.15, rag = 0.3)
                sheet.wash(Path().circle(sunX, sy, 64 * u), p.glow, p.rose, 0.3, soft = 3.0, edge = 1.4, edgeWidth = 3.0, gran = 0.0)
            }
            PendikArt.Kind.DAWN -> {
                sheet.wash(rectP(-pad, hz * 0.35, W + pad, hz + 30 * u), p.glow, p.rose, 0.55, soft = 40.0, hard = 0.32, rag = 1.0,
                    edge = 0.3, gran = 0.15, mix = { x, _ -> smooth(0.0, W, x) * 0.9 }, grade = { _, y -> 0.25 + 0.9 * smooth(0.35, 1.0, down(y)) })
                sheet.wash(rectP(-pad, -pad, W + pad, hz * 0.7), p.sky, p.deep, 0.5, soft = 45.0, hard = 0.35, rag = 1.0,
                    edge = 0.45, gran = 0.45, mix = { _, y -> (1 - down(y)) * 0.35 }, grade = { _, y -> 1.1 - 0.9 * down(y) },
                    blooms = bloomsAt(2, hz * 0.3, hz * 0.6))
                repeat(3) {
                    val cx = rnd.nextDouble() * W
                    val cy = hz * (0.25 + rnd.nextDouble() * 0.4)
                    val pth = Path()
                    repeat(4) { j -> pth.circle(cx + (j - 2) * 60 * u, cy + (rnd.nextDouble() - 0.5) * 30 * u, (40 + rnd.nextDouble() * 30) * u) }
                    sheet.lift(pth, 0.4, soft = 22.0, hard = 0.35, rag = 1.0)
                }
            }
            PendikArt.Kind.NIGHT -> {
                sheet.wash(rectP(-pad, hz * 0.55, W + pad, hz + 30 * u), p.glow, p.rose, 0.38, soft = 40.0, hard = 0.35, rag = 1.0,
                    edge = 0.2, gran = 0.1, grade = { _, y -> smooth(0.55, 1.0, down(y)) })
                sheet.wash(rectP(-pad, -pad, W + pad, hz + 10 * u), p.deep, p.sky, 1.35, soft = 40.0, hard = 0.38, rag = 1.0, edge = 0.55, gran = 0.6,
                    flowVar = 0.4, mix = { _, y -> smooth(0.35, 1.0, down(y)) * 0.9 }, grade = { _, y -> 1.3 - 0.8 * smooth(0.2, 1.0, down(y)) },
                    blooms = bloomsAt(3, hz * 0.15, hz * 0.7))
                sheet.wash(rectP(-pad, -pad, W + pad, hz * 0.42), p.deep, p.shade, 0.55, soft = 35.0, hard = 0.25, rag = 1.2, edge = 0.9, gran = 0.6)
                val m = Vec(ctx.x(0.74), ctx.y(0.16))
                moon = m
                sheet.lift(Path().circle(m.x, m.y, 150 * u), 0.35, soft = 40.0, hard = 0.4, rag = 0.8)
                sheet.lift(Common.crescent(m.x, m.y, 62 * u, rotation = -0.6), 0.97, soft = 1.0, hard = 0.1, rag = 0.25)
                sheet.wash(Common.crescent(m.x, m.y, 62 * u, rotation = -0.6), p.glow, p.glow, 0.18, soft = 1.0, edge = 1.4, gran = 0.0, frame = false)
            }
        }
    }

    /** A long, irregular band of cloud centred on [y]. */
    private fun cloud(y: Double, thick: Double): Path {
        val n = Noise(ctx.seed + (y / u).toInt())
        val top = ArrayList<Vec>(); val bot = ArrayList<Vec>()
        val left = -pad + rnd.nextDouble() * W * 0.3
        val right = W + pad - rnd.nextDouble() * W * 0.3
        var x = left
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

    private val islandPaths = ArrayList<Path>()

    private fun islands() {
        val (col, dens) = when (kind) {
            PendikArt.Kind.SUNSET -> p.deep to 0.75
            PendikArt.Kind.DAWN -> Colors.mix(p.shade, p.sky, 0.5f) to 0.32
            PendikArt.Kind.NIGHT -> p.deep to 1.0
        }
        // The farthest first, palest.
        for ((i, isl) in PendikArt.ISLANDS.withIndex().reversed()) {
            val path = PendikArt.islandPath(ctx, isl, horizon, seed = ctx.seed)
            val far = 1.0 - 0.12 * (i - 1).coerceAtLeast(0)
            sheet.wash(path, col, p.shade, dens * far, soft = 3.0, hard = 0.14, rag = 0.9, edge = 0.9, gran = 0.4, miss = 0.04)
            islandPaths += path
            // Houses and trees at the foot of Büyükada and Heybeliada, as tiny dabs.
            if (isl.w > 300) {
                val dabs = Path()
                val r = Random(ctx.seed + i)
                val cx = ctx.x(isl.x)
                repeat(26) {
                    val x = cx + (r.nextDouble() - 0.5) * isl.w * u * 0.8
                    val y = horizon - (6 + r.nextDouble() * 22) * u
                    dabs.rect(x, y, x + (4 + r.nextDouble() * 5) * u, y + 3 * u)
                }
                if (night) {
                    sheet.lift(dabs, 0.85, soft = 0.4, hard = 0.12, rag = 0.3)
                    sheet.wash(dabs, p.glow, p.rose, 0.35, soft = 0.6, edge = 0.5, gran = 0.0, frame = false)
                } else {
                    sheet.wash(dabs, p.ink, p.deep, 0.35, soft = 0.5, hard = 0.15, rag = 0.3, edge = 0.5, gran = 0.2, miss = 0.3)
                }
            }
        }
        hand.pencil(islandPaths.take(2), 0.12f)
    }

    private fun ripple(n: Noise, x: Double, y: Double): Double {
        val warp = 26 * u * (n.at(x / (330 * u) + 5.0, y / (90 * u)) - 0.5)
        val k = 1 + 0.8 * smooth(horizon, H, y)
        return n.fbm(x / (240 * u), (y + warp) / (11 * u * k), 3)
    }

    private fun sea() {
        val top = horizon
        val streak = Noise(ctx.seed + 55)
        val (wc, wc2, wd) = when (kind) {
            PendikArt.Kind.SUNSET -> Triple(p.glow, p.deep, 0.75)
            PendikArt.Kind.DAWN -> Triple(p.sky, p.rose, 0.42)
            PendikArt.Kind.NIGHT -> Triple(p.deep, p.sky, 1.0)
        }
        sheet.wash(rectP(-pad, top - 2 * u, W + pad, H + pad), wc, wc2, wd, soft = 8.0, hard = 0.2, rag = 0.6, edge = 0.6, gran = 0.5,
            mix = { _, y -> smooth(top, H, y) * (if (kind == PendikArt.Kind.SUNSET) 1.0 else 0.5) },
            grade = { x, y -> (0.55 + 0.6 * smooth(top, H, y)) * (0.7 + 0.6 * ripple(streak, x, y)) })
        // The islands mirrored, broken by the ripples.
        val rip = Noise(ctx.seed + 66)
        for (path in islandPaths) {
            val mirrored = Path()
            for (l in Brush.flatten(path, 3 * u)) mirrored.polygon(l.map { v ->
                val y = 2 * top - v.y
                Vec(v.x + (rip.at(v.x / (200 * u), y / (10 * u)) - 0.5) * 16 * u, y)
            })
            sheet.wash(mirrored, p.deep, p.shade, if (kind == PendikArt.Kind.DAWN) 0.15 else 0.35, soft = 3.0, hard = 0.2, rag = 1.0, edge = 0.5, gran = 0.4,
                clip = rectP(-pad, top, W + pad, H + pad),
                grade = { x, y -> (1 - smooth(top, top + 120 * u, y)) * smooth(0.3, 0.5, ripple(streak, x * 3 + 917 * u, y * 0.6)) })
        }
        // Light on the water.
        val lr = Random(ctx.seed + 88)
        val lifts = Path()
        repeat(16) {
            val y = top + (H - top) * lr.nextDouble().pow(1.4)
            val x = lr.nextDouble() * W
            val len = (60 + lr.nextDouble() * 220) * u
            lifts.rect(x - len / 2, y, x + len / 2, y + (2 + lr.nextDouble() * 3) * u)
        }
        sheet.lift(lifts, 0.55, soft = 1.0, hard = 0.15, rag = 0.8)
        val pathX = moon?.x ?: if (kind == PendikArt.Kind.SUNSET) sunX else -1.0
        if (pathX > 0) {
            val gl = Path()
            repeat(15) {
                val t = (it + lr.nextDouble() * 0.6) / 15.0
                val y = top + 8 * u + (H - top) * 0.6 * t.pow(1.3)
                val half = (14 + 70 * t + lr.nextDouble() * 50) * u
                val x = pathX + (lr.nextDouble() - 0.5) * 40 * u
                gl.rect(x - half, y, x + half, y + (4 + 6 * t + lr.nextDouble() * 4) * u)
            }
            sheet.lift(gl, 0.78, soft = 0.8, hard = 0.15, rag = 0.7)
        }
        sheet.wash(rectP(-pad, top + (H - top) * 0.6, W + pad, H + pad), wc2, p.deep, wd * 0.35, soft = 20.0, hard = 0.3, rag = 1.0, edge = 0.7, gran = 0.5)
    }

    /** A small mosque on a spit of the Pendik shore to the left, its minaret lit at night. */
    private fun shoreMosque() {
        val ground = horizon + 46 * u
        // The spit of land it stands on, running off the left edge.
        val land = Path().moveTo(-pad, ground - 30 * u).lineTo(ctx.x(0.32), ground - 10 * u)
            .quadTo(ctx.x(0.38), ground, ctx.x(0.36), ground + 14 * u).lineTo(-pad, ground + 30 * u).close()
        val c = camii(ctx.x(0.15), ground, 0.34 * u, 2, flip = true, rnd = Random(ctx.seed + 9))
        val dark = if (night) p.deep else p.ink
        sheet.wash(land, dark, p.deep, if (night) 1.0 else 0.85, soft = 2.0, hard = 0.12, rag = 0.9, edge = 0.9, gran = 0.4)
        sheet.wash(c.body, dark, p.deep, if (night) 1.1 else 0.9, soft = 1.2, hard = 0.1, rag = 0.6, edge = 1.0, edgeWidth = 3.0, gran = 0.3)
        // Lit windows and şerefe.
        sheet.lift(c.windows, 0.9, soft = 0.4, hard = 0.12, rag = 0.3)
        sheet.wash(c.windows, p.glow, p.rose, 0.35, soft = 0.8, hard = 0.2, edge = 1.0, edgeWidth = 1.5, gran = 0.0, frame = false)
        if (night) {
            for (m in c.minarets) for (y in m.balconies) {
                sheet.lift(Path().circle(m.x, y, 30 * u), 0.6, soft = 12.0, hard = 0.4, rag = 0.6)
                sheet.wash(Path().circle(m.x, y, 22 * u), p.glow, p.rose, 0.4, soft = 10.0, hard = 0.4, rag = 0.6, edge = 0.4, gran = 0.0, frame = false)
            }
        }
        for (m in c.minarets) {
            hand.line(listOf(Vec(m.x + m.w * 0.5, m.capBase), Vec(m.x + m.w * 0.5, m.base)), 1.8 * u, p.ink, 0.7f, breaks = 1)
            hand.line(listOf(Vec(m.x - m.w * 0.62, m.capBase), Vec(m.x, m.top), Vec(m.x + m.w * 0.62, m.capBase)), 1.6 * u, p.ink, 0.7f, breaks = 0, wob = 0.4)
        }
    }

    /** A ferry crossing towards the islands, smoke trailing, gulls behind. */
    private fun ferry(x: Double, y: Double, len: Double) {
        val (body, windows, funnel) = PendikArt.vapur(x, y, len)
        val col = if (kind == PendikArt.Kind.DAWN) p.shade else p.ink
        sheet.wash(body, col, p.deep, if (night) 1.0 else 0.85, soft = 1.0, hard = 0.1, rag = 0.5, edge = 1.0, edgeWidth = 2.5, gran = 0.25)
        sheet.wash(funnel, p.rose, p.earth, 0.5, soft = 0.8, hard = 0.1, rag = 0.4, edge = 1.0, gran = 0.2)
        sheet.lift(windows, 0.9, soft = 0.3, hard = 0.1, rag = 0.2)
        sheet.wash(windows, p.glow, p.rose, if (night) 0.4 else 0.25, soft = 0.6, edge = 0.8, edgeWidth = 1.2, gran = 0.0, frame = false)
        // Smoke drifting back.
        val smoke = Path()
        repeat(6) { k ->
            smoke.circle(x + len * (0.1 + k * 0.08), y - len * (0.42 + k * 0.03), len * (0.04 + k * 0.012))
        }
        sheet.wash(smoke, p.shade, p.sky, 0.18, soft = 14.0, hard = 0.4, rag = 1.0, edge = 0.3, gran = 0.3)
        // Wake: lifted lines fanning out behind.
        val wake = Path()
        repeat(7) { k ->
            val wy = y + (2 + k * 5) * u
            wake.rect(x + len * 0.42, wy, x + len * (0.6 + k * 0.12), wy + 2.5 * u)
        }
        sheet.lift(wake, 0.7, soft = 1.0, hard = 0.15, rag = 0.8)
        gulls(x + len * 0.5, y - len * 0.75, len * 0.9, 6)
    }

    private fun gulls(cx: Double, cy: Double, spread: Double, n: Int) {
        val r = Random(ctx.seed + 404)
        val col = if (night) Colors.lighten(p.glow, 0.4f) else p.ink
        repeat(n) {
            hand.bird(cx + (r.nextDouble() - 0.5) * spread, cy + (r.nextDouble() - 0.5) * spread * 0.4, (12 + r.nextDouble() * 12) * u, col, 0.8f, r)
        }
    }

    /** The promenade at the bottom: railing, a lamp post and the line of the sea wall. */
    private fun promenade() {
        val y = H - (H - horizon) * 0.17
        val wall = rectP(-pad, y, W + pad, H + pad)
        sheet.wash(wall, if (night) p.deep else p.shade, p.ink, if (night) 1.1 else 0.7, soft = 2.0, hard = 0.14, rag = 0.9, edge = 0.9, gran = 0.6)
        // Railing.
        val railY = y - 70 * u
        hand.line(listOf(Vec(-pad, railY), Vec(W + pad, railY + 4 * u)), 3.2 * u, p.ink, 0.85f, breaks = 2)
        hand.line(listOf(Vec(-pad, railY + 34 * u), Vec(W + pad, railY + 37 * u)), 2.0 * u, p.ink, 0.6f, breaks = 2)
        var x = 30 * u
        while (x < W) {
            hand.line(listOf(Vec(x, railY - 2 * u), Vec(x, y + 2 * u)), 2.6 * u, p.ink, 0.85f, breaks = 0, wob = 0.4)
            x += 150 * u
        }
        // A lamp post on the right, its light pooled on the paper.
        val lx = ctx.x(0.82)
        val top = railY - 340 * u
        hand.line(listOf(Vec(lx, y), Vec(lx, top + 30 * u)), 6.0 * u, p.ink, 0.9f, breaks = 0, wob = 0.3)
        sheet.wash(Path().polygon(listOf(Vec(lx - 26 * u, top + 30 * u), Vec(lx + 26 * u, top + 30 * u), Vec(lx + 16 * u, top - 30 * u), Vec(lx - 16 * u, top - 30 * u))),
            p.ink, p.deep, 0.9, soft = 1.0, hard = 0.12, rag = 0.5, edge = 1.0, gran = 0.2)
        if (night || kind == PendikArt.Kind.SUNSET) {
            sheet.lift(Path().circle(lx, top, 70 * u), 0.55, soft = 20.0, hard = 0.4, rag = 0.6)
            sheet.wash(Path().circle(lx, top, 50 * u), p.glow, p.rose, 0.42, soft = 14.0, hard = 0.35, rag = 0.6, edge = 0.3, gran = 0.0)
        }
    }

    /** Pendik Marina at dawn: the breakwater light, then rows of yachts with their masts. */
    private fun marina() {
        val moleY = horizon + 90 * u
        // Breakwater (mendirek) of stacked rocks, the harbour light at its end.
        val mole = Path().moveTo(-pad, moleY - 16 * u).lineTo(ctx.x(0.62), moleY - 12 * u)
            .quadTo(ctx.x(0.66), moleY, ctx.x(0.62), moleY + 14 * u).lineTo(-pad, moleY + 18 * u).close()
        sheet.wash(mole, p.shade, p.earth, 0.75, soft = 2.0, hard = 0.14, rag = 1.0, edge = 1.0, gran = 0.7, miss = 0.05)
        val lx = ctx.x(0.6)
        val tower = rectP(lx - 13 * u, moleY - 120 * u, lx + 13 * u, moleY - 12 * u)
        sheet.wash(tower, p.paper, p.paper, 0.0)
        sheet.wash(tower, Colors.hex("#C8463C"), p.rose, 0.85, soft = 0.8, hard = 0.1, rag = 0.3, edge = 1.0, gran = 0.2,
            grade = { _, y -> if (((y - (moleY - 120 * u)) / (24 * u)).toInt() % 2 == 0) 1.0 else 0.08 })
        sheet.wash(rectP(lx - 18 * u, moleY - 138 * u, lx + 18 * u, moleY - 120 * u), p.ink, p.deep, 0.8, soft = 0.6, hard = 0.1, rag = 0.3, edge = 0.8, gran = 0.1)
        sheet.lift(Path().circle(lx, moleY - 129 * u, 28 * u), 0.5, soft = 10.0, hard = 0.4, rag = 0.5)

        // Yachts in two rows, the far row smaller. Hulls are the paper; shadows and masts are painted.
        // Sizes and gaps vary and about a third are motor boats with no mast, so the rows don't read as a grid.
        val r = Random(ctx.seed + 21)
        val rows = listOf(horizon + (H - horizon) * 0.34 to 0.55, horizon + (H - horizon) * 0.58 to 0.9)
        for ((rowY, k) in rows) {
            var x = -40 * u + r.nextDouble() * 120 * u
            while (x < W + 60 * u) {
                val len = (150 + r.nextDouble() * 90) * u * k * (0.8 + r.nextDouble() * 0.4)
                val dir = if (r.nextBoolean()) 1.0 else -1.0
                val motor = r.nextDouble() < 0.33
                val y = rowY + (r.nextDouble() - 0.5) * 8 * u * k
                val hull = PendikArt.sailboatHull(x, y, len, dir)
                sheet.wash(hull, p.shade, p.sky, 0.28, soft = 0.8, hard = 0.1, rag = 0.4, edge = 1.3, edgeWidth = 2.0, gran = 0.2)
                sheet.wash(rectP(x - len * 0.42, y - len * 0.02, x + len * 0.42, y + len * 0.02), p.ink, p.deep, 0.6, soft = 0.6, hard = 0.12, rag = 0.4, edge = 0.6, gran = 0.2)
                if (motor) {
                    // Two-deck cabin with a dark band of windows and a short radar mast.
                    sheet.wash(rectP(x - len * 0.28, y - len * 0.2, x + len * 0.22, y - len * 0.08), p.shade, p.shade, 0.3, soft = 0.6, edge = 1.0, gran = 0.2)
                    sheet.wash(rectP(x - len * 0.16, y - len * 0.3, x + len * 0.1, y - len * 0.2), p.shade, p.shade, 0.26, soft = 0.6, edge = 1.0, gran = 0.2)
                    sheet.wash(rectP(x - len * 0.25, y - len * 0.17, x + len * 0.19, y - len * 0.13), p.ink, p.deep, 0.55, soft = 0.5, edge = 0.6, gran = 0.1)
                    hand.line(listOf(Vec(x, y - len * 0.3), Vec(x, y - len * 0.42)), 1.6 * u * k, p.ink, 0.75f, breaks = 0, wob = 0.3)
                    hand.line(listOf(Vec(x - len * 0.05, y - len * 0.38), Vec(x + len * 0.05, y - len * 0.38)), 1.4 * u * k, p.ink, 0.7f, breaks = 0, wob = 0.3)
                } else {
                    sheet.wash(rectP(x - len * 0.12, y - len * 0.17, x + len * 0.15, y - len * 0.08), p.shade, p.shade, 0.35, soft = 0.6, edge = 1.0, gran = 0.2)
                    val mastH = len * (1.9 + r.nextDouble() * 0.7)
                    val lean = (r.nextDouble() - 0.5) * 10 * u
                    val head = Vec(x + lean, y - len * 0.08 - mastH)
                    hand.line(listOf(Vec(x, y - len * 0.08), head), 2.2 * u * k, p.ink, 0.85f, breaks = 0, wob = 0.3)
                    hand.line(listOf(Vec(x, y - len * 0.3), Vec(x + dir * len * 0.4, y - len * 0.26)), 1.6 * u * k, p.ink, 0.7f, breaks = 0, wob = 0.3)
                    // Stays down to bow and stern, very fine.
                    hand.line(listOf(head, Vec(x + dir * len * 0.48, y - len * 0.1)), 0.8 * u, p.ink, 0.45f, breaks = 1, wob = 0.2)
                    hand.line(listOf(head, Vec(x - dir * len * 0.42, y - len * 0.09)), 0.8 * u, p.ink, 0.35f, breaks = 1, wob = 0.2)
                    // Reflection of the mast, wavering.
                    val refl = ArrayList<Vec>()
                    for (i in 0..12) {
                        val t = i / 12.0
                        refl += Vec(x + sin(t * 9 + x) * 6 * u, y + len * 0.03 + t * mastH * 0.6)
                    }
                    hand.line(refl, 1.6 * u * k, p.deep, 0.3f, breaks = 3, wob = 1.5)
                }
                x += len * (0.95 + r.nextDouble() * 0.7)
                // Now and then an empty berth.
                if (r.nextDouble() < 0.15) x += len * 0.8
            }
            // The pontoon the row is moored to.
            hand.line(listOf(Vec(-pad, rowY + 30 * u * k), Vec(W + pad, rowY + 32 * u * k)), 4.0 * u * k, p.ink, 0.6f, breaks = 3)
        }
        // The wooden pontoon underfoot: planks, a bollard and a mooring line running out of the picture.
        val deckY = H - (H - horizon) * 0.13
        val deck = Path().moveTo(-pad, deckY + 10 * u).lineTo(W + pad, deckY - 6 * u).lineTo(W + pad, H + pad).lineTo(-pad, H + pad).close()
        sheet.wash(deck, p.earth, p.shade, 0.62, soft = 1.5, hard = 0.14, rag = 0.8, edge = 1.0, gran = 0.7,
            grade = { x, y -> 0.8 + 0.4 * smooth(deckY, H, y) + 0.15 * sin(x / (17 * u)) })
        var px = -20 * u + r.nextDouble() * 40 * u
        while (px < W + 40 * u) {
            hand.line(listOf(Vec(px, deckY + 10 * u - (px + pad) / (W + 2 * pad) * 16 * u), Vec(px - 50 * u, H + pad)), 1.6 * u, p.ink, 0.45f, breaks = 2, wob = 0.5)
            px += (110 + r.nextDouble() * 30) * u
        }
        hand.line(listOf(Vec(-pad, deckY + 10 * u), Vec(W + pad, deckY - 6 * u)), 3.0 * u, p.ink, 0.75f, breaks = 2)
        val bx = ctx.x(0.24)
        val bTop = deckY - 34 * u
        sheet.wash(rectP(bx - 16 * u, bTop, bx + 16 * u, deckY + 18 * u), p.ink, p.deep, 0.85, soft = 0.6, hard = 0.1, rag = 0.3, edge = 1.0, gran = 0.2)
        sheet.wash(rectP(bx - 22 * u, bTop - 8 * u, bx + 22 * u, bTop + 4 * u), p.ink, p.deep, 0.9, soft = 0.6, hard = 0.1, rag = 0.3, edge = 1.0, gran = 0.2)
        // Slack, so it sags below the straight line between bollard and stern.
        val end = Vec(ctx.x(0.58), rows[1].first - 6 * u)
        val rope = (0..20).map { i ->
            val t = i / 20.0
            Vec(bx + (end.x - bx) * t, bTop + 6 * u + (end.y - bTop - 6 * u) * t + 46 * u * sin(t * PI))
        }
        hand.line(rope, 2.4 * u, p.ink, 0.7f, breaks = 1, wob = 0.4)
        gulls(ctx.x(0.3), horizon - 300 * u, 600 * u, 7)
    }

    private fun finishing() {
        val r = Random(ctx.seed + 7)
        if (night) {
            val stars = (0 until 80).map { Vec(r.nextDouble() * W, horizon * 0.62 * r.nextDouble().pow(1.3)) }
            hand.gouache(stars, { it.nextDouble().pow(3) * 2.6 * u + 0.8 * u }, p.paper, 0.85f, r)
            // Lights strung along the islands' shores and their reflections.
            for (isl in PendikArt.ISLANDS.take(4)) {
                val cx = ctx.x(isl.x)
                val pts = (0 until (isl.w / 18).toInt()).map { Vec(cx - isl.w * u * 0.42 + it * 18 * u + r.nextDouble() * 6 * u, horizon - (3 + r.nextDouble() * 18) * u) }
                hand.gouache(pts, { (1.2 + it.nextDouble() * 1.4) * u }, Colors.lighten(p.glow, 0.4f), 0.9f, r)
            }
            // The HB constellation, as everywhere she looks up at night.
            Common.initialsConstellation(b, ctx, ctx.x(0.12), ctx.y(0.1), 60 * u, alpha = 0.8f)
        }
        hand.splatter(ctx.x(0.08), ctx.h * 0.9, 140 * u, 26, listOf(p.deep, p.earth, p.sky), 0.45f, r)
    }
}

// ================================================================ vector Pendik

/** Martılar ve Vapur: a ferry on the Marmara in a flat poster style, gulls wheeling, the islands behind, a band of geometric waves. */
private object VapurPoster {
    fun scene(ctx: RenderContext): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        val horizon = ctx.y(0.52)
        // Sky: a calm evening gradient with a low sun.
        b.fill(
            Path().rect(0, 0, ctx.w, horizon),
            LinearFill(0f, 0f, 0f, horizon.toFloat(), intArrayOf(Colors.darken(pal.bgTop, 0.25f), pal.bgTop, Colors.mix(pal.accentC, pal.glow, 0.5f)), floatArrayOf(0f, 0.6f, 1f)),
        )
        Common.starfield(b, ctx, 0.0, horizon * 0.45, 40, salt = 81)
        val sunX = ctx.x(0.3)
        Common.glow(b, sunX, horizon - 120 * u, 420 * u, pal.glow, 0.45f)
        b.fill(Path().circle(sunX, horizon - 120 * u, 70 * u), Colors.lighten(pal.glow, 0.2f))
        // Islands.
        for (isl in PendikArt.ISLANDS.reversed()) {
            b.fill(PendikArt.islandPath(ctx, isl, horizon, seed = ctx.seed), Colors.mix(pal.bgBottom, pal.accentA, 0.25f), 0.9f)
        }
        // Sea in bands.
        b.fill(
            Path().rect(0, horizon, ctx.w, ctx.h),
            LinearFill(0f, horizon.toFloat(), 0f, ctx.h.toFloat(), intArrayOf(Colors.mix(pal.accentA, pal.glow, 0.25f), pal.accentA, Colors.darken(pal.bgBottom, 0.2f))),
        )
        val streaks = Path()
        val r = Random(ctx.seed + 3)
        repeat(28) {
            val y = horizon + (ctx.h - horizon) * r.nextDouble().pow(1.5)
            val half = (40 + r.nextDouble() * 160) * u
            val x = sunX + (r.nextDouble() - 0.5) * 300 * u
            streaks.roundRect(x - half, y, x + half, y + 4 * u, 2 * u)
        }
        b.fill(streaks, pal.glow, 0.35f)
        // The ferry, big, crossing left.
        val fy = horizon + (ctx.h - horizon) * 0.22
        val len = minOf(820 * u, ctx.safeW * 0.7)
        val fx = ctx.x(0.55)
        val (body, windows, funnel) = PendikArt.vapur(fx, fy, len)
        b.fill(Path().rect(fx - len * 0.5, fy - 2 * u, fx + len * 0.6, fy + 10 * u), Colors.WHITE, 0.25f)
        b.fill(body, Colors.mix(Colors.WHITE, pal.line, 0.15f))
        b.fill(Path().rect(fx - len * 0.48, fy - len * 0.035, fx + len * 0.47, fy), Colors.darken(pal.bgBottom, 0.3f))
        b.fill(funnel, Colors.darken(pal.bgBottom, 0.4f))
        b.fill(Path().rect(fx + len * 0.035, fy - len * 0.33, fx + len * 0.135, fy - len * 0.31), pal.accentB)
        b.fill(windows, Colors.darken(pal.bgBottom, 0.2f), 0.85f)
        b.stroke(body, Colors.darken(pal.bgBottom, 0.5f), (2 * u).toFloat(), 0.5f)
        // Wake.
        val wake = Path()
        repeat(6) { k -> wake.roundRect(fx + len * 0.45, fy + (6 + k * 10) * u, fx + len * (0.7 + k * 0.1), fy + (9 + k * 10) * u, 2 * u) }
        b.fill(wake, Colors.WHITE, 0.6f)
        // Gulls: simple M strokes.
        val gulls = Path()
        repeat(9) {
            val x = fx + (r.nextDouble() - 0.1) * len
            val y = fy - len * (0.45 + r.nextDouble() * 0.5)
            val s = (14 + r.nextDouble() * 18) * u
            gulls.moveTo(x - s, y - s * 0.2).quadTo(x - s * 0.5, y - s * 0.7, x, y).quadTo(x + s * 0.5, y - s * 0.7, x + s, y - s * 0.2)
        }
        b.stroke(gulls, Colors.WHITE, (3.5 * u).toFloat(), 0.95f)
        // Geometric wave band at the foot.
        waveBand(b, ctx, ctx.y(0.86), 60 * u)
        if (ctx.options.captions) {
            b.text(TextItem("PENDİK", FontId.LATIN, (52 * u).toFloat(), ctx.cx.toFloat(), ctx.y(0.075).toFloat(), SolidFill(pal.line), alpha = 0.9f, letterSpacing = 0.45f))
            b.text(TextItem("Marmara · Adalar · Vapur", FontId.LATIN, (26 * u).toFloat(), ctx.cx.toFloat(), (ctx.y(0.075) + 50 * u).toFloat(), SolidFill(Colors.lighten(pal.line, 0.4f)), alpha = 0.75f, letterSpacing = 0.12f))
        }
        Textures.grainOverlay(b, ctx, 0.35f)
        Common.vignette(b, ctx, 0.35f)
        return b.build()
    }

    /** A row of stylised waves like an İznik border, centred on [y]. */
    fun waveBand(b: SceneBuilder, ctx: RenderContext, y: Double, s: Double) {
        val pal = ctx.palette
        b.fill(Path().rect(0, y - s * 0.9, ctx.w, y + s * 0.9), Colors.darken(pal.bgBottom, 0.2f), 0.85f)
        val waves = Path()
        var x = -s
        while (x < ctx.w + s) {
            waves.moveTo(x, y + s * 0.4).cubicTo(x + s * 0.2, y - s * 0.5, x + s * 0.9, y - s * 0.55, x + s * 1.1, y - s * 0.05)
                .quadTo(x + s * 0.75, y - s * 0.25, x + s * 0.6, y + s * 0.05).quadTo(x + s * 0.9, y + s * 0.4, x + s * 1.4, y + s * 0.4).close()
            x += s * 1.4
        }
        b.fill(waves, pal.accentA, 0.9f)
        b.stroke(waves, pal.line, (1.5 * ctx.u).toFloat(), 0.8f)
        b.stroke(Path().moveTo(0, y - s * 0.9).lineTo(ctx.w, y - s * 0.9).moveTo(0, y + s * 0.9).lineTo(ctx.w, y + s * 0.9), pal.line, (3 * ctx.u).toFloat(), 0.9f)
    }
}

/** Aydos'tan Pendik: stone pines on Aydos hill at night, the town's lights below, the sea and the islands beyond. */
private object AydosNight {
    fun scene(ctx: RenderContext): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        val horizon = ctx.y(0.5)
        b.fill(
            Path().rect(0, 0, ctx.w, horizon),
            LinearFill(0f, 0f, 0f, horizon.toFloat(), intArrayOf(Colors.darken(pal.bgBottom, 0.4f), pal.bgBottom, Colors.mix(pal.bgTop, pal.glow, 0.25f)), floatArrayOf(0f, 0.55f, 1f)),
        )
        Common.starfield(b, ctx, 0.0, horizon * 0.85, 220, salt = 17)
        val mx = ctx.x(0.7); val my = ctx.y(0.17)
        Common.glow(b, mx, my, 300 * u, pal.glow, 0.3f)
        b.fill(Common.crescent(mx, my, 80 * u, rotation = -0.7), Colors.lighten(pal.glow, 0.3f))
        Common.initialsConstellation(b, ctx, ctx.x(0.14), ctx.y(0.12), 64 * u, alpha = 0.85f)
        // The sea, the islands with their lights.
        val sea = Colors.mix(pal.bgBottom, pal.bgTop, 0.35f)
        b.fill(Path().rect(0, horizon, ctx.w, ctx.h), LinearFill(0f, horizon.toFloat(), 0f, ctx.h.toFloat(), intArrayOf(sea, Colors.darken(sea, 0.4f)), floatArrayOf(0f, 0.6f)))
        for (isl in PendikArt.ISLANDS) b.fill(PendikArt.islandPath(ctx, isl, horizon, scale = 0.7, seed = ctx.seed), Colors.darken(pal.bgBottom, 0.55f))
        val r = Random(ctx.seed + 5)
        val lights = Path()
        for (isl in PendikArt.ISLANDS) repeat((isl.w / 30).toInt()) {
            val x = ctx.x(isl.x) + (r.nextDouble() - 0.5) * isl.w * u * 0.6
            lights.circle(x, horizon - r.nextDouble() * 10 * u, (1.5 + r.nextDouble()) * u)
        }
        b.fill(lights, pal.glow, 0.85f)
        // Moon path on the sea.
        val path = Path()
        repeat(16) {
            val y = horizon + 8 * u + it * 14 * u
            val half = (20 + it * 9 + r.nextDouble() * 30) * u
            path.rect(mx - half, y, mx + half, y + 2.5 * u)
        }
        b.fill(path, pal.glow, 0.3f)
        // The town sloping down to the shore: layers of hills dotted with windows, paler with distance.
        val layers = 4
        val near = Colors.mix(Colors.darken(pal.bgBottom, 0.5f), Colors.BLACK, 0.3f)
        val far = Colors.mix(pal.bgBottom, pal.bgTop, 0.25f)
        var mosqueY = 0.0
        for (k in 0 until layers) {
            val t = k / (layers - 1.0)
            val baseY = horizon + 40 * u + (ctx.h - horizon) * (0.12 + t * 0.32)
            val n = Noise(ctx.seed + 30 + k)
            val pts = ArrayList<Vec>()
            var x = -10 * u
            while (x <= ctx.w + 10 * u) {
                pts += Vec(x, baseY - (40 + 80 * n.fbm(x / (420 * u), 0.3, 3)) * u * (0.6 + t))
                x += 12 * u
            }
            val hill = Path().polygon(pts + listOf(Vec(ctx.w + 10 * u, ctx.h), Vec(-10 * u, ctx.h)))
            val tone = Colors.mix(far, near, t.toFloat())
            b.fill(hill, LinearFill(0f, (baseY - 120 * u).toFloat(), 0f, (baseY + 200 * u).toFloat(), intArrayOf(Colors.mix(tone, pal.glow, 0.08f), tone), floatArrayOf(0f, 1f)))
            // The mosque stands on the second ridge, in front of the far one and behind the near ones.
            if (k == 1) {
                val mosqueX = ctx.x(0.38)
                mosqueY = pts[((mosqueX + 10 * u) / (12 * u)).toInt().coerceIn(0, pts.size - 1)].y + 30 * u
                val (body, windows) = NightArt.mosque(ctx, NightArt.Architecture.OTTOMAN, 0.0, 0.0, Random(ctx.seed + 9))
                val s = 0.32
                Common.glow(b, mosqueX, mosqueY - 120 * u, 260 * u, pal.glow, 0.22f)
                // Floodlit, as İstanbul's mosques are at night.
                b.fill(body.transformed { px, py -> (mosqueX + px * s).toFloat() to (mosqueY + py * s).toFloat() }, Colors.mix(pal.bgTop, pal.glow, 0.45f))
                b.fill(windows.transformed { px, py -> (mosqueX + px * s).toFloat() to (mosqueY + py * s).toFloat() }, Colors.lighten(pal.glow, 0.3f), 0.9f)
            }
            val win = Path()
            // Windows fill the slope down to the next ridge; the nearest slope runs to the foot of the picture.
            val last = k == layers - 1
            repeat((if (last) 260 else 150 * (1 - t * 0.45)).toInt()) {
                val wx = r.nextDouble() * ctx.w
                val idx = ((wx + 10 * u) / (12 * u)).toInt().coerceIn(0, pts.size - 1)
                val spread = if (last) (ctx.h - pts[idx].y) * 0.8 else (ctx.h - horizon) * 0.12 + 20 * u
                val wy = pts[idx].y + 8 * u + r.nextDouble() * spread
                win.rect(wx, wy, wx + (2 + r.nextDouble() * 3) * u * (1 + t), wy + 2 * u * (1 + t))
            }
            b.fill(win, Colors.mix(pal.glow, Colors.hex("#FFB46A"), 0.4f), (0.6 + 0.35 * (1 - t)).toFloat())
            // Street lamps strung along the coast road below the first ridge.
            if (k == 0) {
                val road = Path()
                var lx = r.nextDouble() * 14 * u
                while (lx < ctx.w) {
                    road.circle(lx, horizon + 30 * u + sin(lx / (300 * u)) * 6 * u, 2.2 * u)
                    lx += (16 + r.nextDouble() * 10) * u
                }
                b.fill(road, Colors.hex("#FFC27A"), 0.9f)
            }
        }
        // Stone pines of Aydos in the foreground, tall enough that their crowns stand against the sky.
        val pr = Random(ctx.seed + 11)
        val pineCol = Colors.mix(Colors.darken(pal.bgBottom, 0.85f), Colors.BLACK, 0.4f)
        for ((fx, s) in listOf(0.04 to 0.58, 0.93 to 0.5, 0.78 to 0.3)) pine(b, ctx.x(fx), ctx.h + 20 * u, ctx.h * s, pineCol, pr)
        // The hillside they grow on.
        val ground = Noise(ctx.seed + 77)
        val gp = ArrayList<Vec>()
        var gx = -10 * u
        while (gx <= ctx.w + 10 * u) {
            gp += Vec(gx, ctx.h - (90 + 70 * ground.fbm(gx / (300 * u), 0.5, 3)) * u - 60 * u * (1 - gx / ctx.w))
            gx += 12 * u
        }
        b.fill(Path().polygon(gp + listOf(Vec(ctx.w + 10 * u, ctx.h + 10 * u), Vec(-10 * u, ctx.h + 10 * u))), pineCol)
        if (ctx.options.captions) {
            b.text(TextItem("Aydos'tan Pendik", FontId.LATIN, (34 * u).toFloat(), ctx.cx.toFloat(), ctx.y(0.94).toFloat(), SolidFill(pal.line), alpha = 0.85f, letterSpacing = 0.12f))
        }
        Textures.grainOverlay(b, ctx, 0.3f)
        Common.vignette(b, ctx, 0.4f)
        return b.build()
    }

    /** A stone pine (fıstık çamı): a leaning trunk and a flat, layered umbrella crown. */
    fun pine(b: SceneBuilder, x: Double, ground: Double, h: Double, color: Int, r: Random) {
        val lean = (r.nextDouble() - 0.5) * 0.25 * h
        val top = Vec(x + lean, ground - h)
        val trunk = Path().moveTo(x - h * 0.03, ground).quadTo(x + lean * 0.3, ground - h * 0.5, top.x - h * 0.012, top.y + h * 0.05)
            .lineTo(top.x + h * 0.012, top.y + h * 0.05).quadTo(x + lean * 0.3 + h * 0.02, ground - h * 0.5, x + h * 0.03, ground).close()
        b.fill(trunk, color)
        // Branches fork from the top of the trunk out under the crown.
        val crown = Path()
        val width = h * 0.5
        val fork = Vec(top.x, top.y + h * 0.08)
        for (i in 0 until 5) {
            val ex = top.x + (i / 4.0 - 0.5) * width * 0.8 + (r.nextDouble() - 0.5) * h * 0.03
            val ey = top.y + h * (0.005 + r.nextDouble() * 0.02)
            val bw = h * 0.006
            crown.moveTo(fork.x - bw, fork.y).quadTo((fork.x + ex) / 2, fork.y - h * 0.01, ex, ey)
                .quadTo((fork.x + ex) / 2 + bw, fork.y - h * 0.005, fork.x + bw, fork.y).close()
        }
        // Clumps of needles along a flat umbrella: many small lumps, highest in the middle.
        repeat(34) {
            val t = r.nextDouble() * 2 - 1
            val cx = top.x + t * width / 2
            val dome = 1 - t * t
            val cy = top.y - dome * h * 0.035 + (r.nextDouble() - 0.4) * h * 0.035
            val rx = h * (0.035 + r.nextDouble() * 0.04) * (0.6 + 0.4 * dome)
            crown.append(Path().circle(cx, cy, rx).transformed { px, py -> px to (cy + (py - cy) * 0.55).toFloat() })
        }
        b.fill(crown, color)
    }
}

/**
 * Pendik'ten Kıble: a compass medallion in gold with the needle set to the
 * real direction of the Kaaba from Pendik, and how far it is.
 */
private object PendikQibla {
    fun scene(ctx: RenderContext): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        Common.background(b, ctx)
        val backdrop = GeometricArt.Params(Tiling.DODECAGON_HEXAGON_SQUARE, PatternStyle.LINEWORK, edge = 86.0)
        b.group(alpha = 0.3f) { GeometricArt.drawPattern(this, ctx, backdrop, GeometricArt.tiles(ctx, backdrop)) }
        Common.vignette(b, ctx, 0.6f)

        val cx = ctx.cx; val cy = ctx.cy - 40 * u
        val r = minOf(ctx.safeW * 0.40, ctx.safeH * 0.26)
        CalligraphyArt.medallion(b, ctx, cx, cy, r)
        // Compass ticks and the four letters.
        val ticks = Path()
        for (d in 0 until 360 step 10) {
            val a = Math.toRadians(d - 90.0)
            val r0 = if (d % 90 == 0) r * 0.62 else r * 0.7
            ticks.moveTo(cx + r0 * cos(a), cy + r0 * sin(a)).lineTo(cx + r * 0.76 * cos(a), cy + r * 0.76 * sin(a))
        }
        b.stroke(ticks, pal.line, (2 * u).toFloat(), 0.7f)
        for ((label, d) in listOf("K" to 0, "D" to 90, "G" to 180, "B" to 270)) {
            val a = Math.toRadians(d - 90.0)
            b.text(TextItem(label, FontId.LATIN, (30 * u).toFloat(), (cx + r * 0.52 * cos(a)).toFloat(), (cy + r * 0.52 * sin(a)).toFloat(), SolidFill(pal.line), alpha = 0.85f))
        }
        // The needle, to the Kaaba.
        val bearing = Qibla.bearing(PendikArt.LAT, PendikArt.LON)
        val a = Math.toRadians(bearing - 90)
        val tip = Vec(cx + r * 0.58 * cos(a), cy + r * 0.58 * sin(a))
        val tail = Vec(cx - r * 0.3 * cos(a), cy - r * 0.3 * sin(a))
        val side = Vec(-sin(a), cos(a)) * (r * 0.05)
        b.fill(Path().polygon(listOf(tip, Vec(cx, cy) + side, tail, Vec(cx, cy) - side)), Colors.lighten(pal.line, 0.2f))
        b.fill(Path().polygon(listOf(tip, Vec(cx, cy) + side, Vec(cx, cy))), Colors.darken(pal.line, 0.25f), 0.6f)
        b.fill(Path().circle(cx, cy, r * 0.06), pal.glow)
        // The Kaaba at the needle's tip.
        val k = r * 0.11
        val kc = Vec(cx + r * 0.68 * cos(a), cy + r * 0.68 * sin(a))
        Common.glow(b, kc.x, kc.y, k * 3, pal.glow, 0.5f)
        b.fill(Path().rect(kc.x - k / 2, kc.y - k / 2, kc.x + k / 2, kc.y + k / 2), Colors.hex("#111111"))
        b.fill(Path().rect(kc.x - k / 2, kc.y - k * 0.26, kc.x + k / 2, kc.y - k * 0.14), pal.line)

        val km = Qibla.distanceKm(PendikArt.LAT, PendikArt.LON).roundToInt()
        val ty = cy + r * 1.25
        b.text(TextItem("قِبْلَة", FontId.NASKH_BOLD, (90 * u).toFloat(), cx.toFloat(), (cy - r * 1.3).toFloat(), SolidFill(pal.line), maxHeight = (110 * u).toFloat(), inkCentered = true))
        if (ctx.options.captions) {
            b.text(TextItem("Pendik'ten Kıble", FontId.LATIN, (42 * u).toFloat(), cx.toFloat(), ty.toFloat(), SolidFill(pal.line), letterSpacing = 0.06f))
            b.text(
                TextItem(
                    "${bearing.roundToInt()}° ${Qibla.compassName(bearing)} · Kâbe ${"%,d".format(TurkishText.TR, km)} km",
                    FontId.LATIN, (30 * u).toFloat(), cx.toFloat(), (ty + 56 * u).toFloat(), SolidFill(Colors.lighten(pal.line, 0.45f)), alpha = 0.85f,
                ),
            )
            b.text(
                TextItem(
                    "40°52′ K · 29°14′ D", FontId.LATIN, (24 * u).toFloat(), cx.toFloat(), (ty + 100 * u).toFloat(),
                    SolidFill(Colors.lighten(pal.line, 0.45f)), alpha = 0.6f, letterSpacing = 0.1f,
                ),
            )
        }
        Textures.grainOverlay(b, ctx, 0.35f)
        Common.topShade(b, ctx)
        return b.build()
    }
}

/**
 * Mahya: İstanbul's Ramadan custom of words written in light bulbs strung
 * between two minarets. Here over a mosque on Pendik's shore, the message
 * hers to change (Tasarım → Kendi sözün).
 */
internal object MahyaArt {
    const val DEFAULT_TEXT = "HOŞ GELDİN RAMAZAN"

    fun scene(ctx: RenderContext): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        val r = ctx.random(61)
        val ground = ctx.y(0.7)
        b.fill(
            Path().rect(0, 0, ctx.w, ground),
            LinearFill(0f, 0f, 0f, ground.toFloat(), intArrayOf(Colors.darken(pal.bgBottom, 0.4f), pal.bgBottom, Colors.mix(pal.bgTop, pal.accentA, 0.3f)), floatArrayOf(0f, 0.6f, 1f)),
        )
        Common.starfield(b, ctx, 0.0, ground * 0.7, 160, salt = 23)
        val mx = ctx.x(0.8); val my = ctx.y(0.1)
        Common.glow(b, mx, my, 220 * u, pal.glow, 0.25f)
        b.fill(Common.crescent(mx, my, 54 * u, rotation = -0.5), Colors.lighten(pal.glow, 0.3f))
        Common.initialsConstellation(b, ctx, ctx.x(0.1), ctx.y(0.08), 50 * u, alpha = 0.7f)

        // Pendik's hills behind, their windows lit for sahur.
        val hillN = Noise(ctx.seed + 3)
        val hill = ArrayList<Vec>()
        var hx = -10 * u
        while (hx <= ctx.w + 10 * u) {
            hill += Vec(hx, ground - (120 + 160 * hillN.fbm(hx / (500 * u), 0.2, 3)) * u)
            hx += 12 * u
        }
        val hillCol = Colors.mix(pal.bgBottom, Colors.BLACK, 0.35f)
        b.fill(Path().polygon(hill + listOf(Vec(ctx.w + 10 * u, ground), Vec(-10 * u, ground))), hillCol)
        val win = Path()
        repeat(160) {
            val wx = r.nextDouble() * ctx.w
            val top = hill[((wx + 10 * u) / (12 * u)).toInt().coerceIn(0, hill.size - 1)].y
            val wy = top + 10 * u + r.nextDouble() * (ground - top - 10 * u)
            win.rect(wx, wy, wx + (2 + r.nextDouble() * 3) * u, wy + 2.5 * u)
        }
        b.fill(win, Colors.mix(pal.glow, Colors.hex("#FFB46A"), 0.4f), 0.7f)

        // The mosque, sized so its two minarets frame the mahya.
        val s = minOf(ctx.safeW * 0.82 / 600.0, (ground - ctx.y(0.08)) / 690.0)
        val c = camii(ctx.cx + 3 * s, ground, s, 2, flip = false, rnd = Random(ctx.seed + 4))
        Common.glow(b, ctx.cx, ground - 300 * s, 520 * s, pal.glow, 0.16f)
        val stone = Colors.mix(Colors.darken(pal.bgBottom, 0.3f), pal.glow, 0.12f)
        b.fill(c.body, LinearFill(0f, c.top.toFloat(), 0f, ground.toFloat(), intArrayOf(Colors.mix(stone, pal.glow, 0.1f), stone), floatArrayOf(0f, 1f)))
        b.fill(c.windows, Colors.mix(stone, pal.glow, 0.75f), 0.75f)
        b.fill(c.arcade, Colors.darken(pal.bgBottom, 0.6f))
        for (m in c.minarets) for (y in m.balconies) b.fill(Path().rect(m.x - m.w, y - 1.5 * u, m.x + m.w, y + 1.5 * u), Colors.lighten(pal.glow, 0.4f), 0.9f)

        // The quay, then the sea.
        val quay = ground + 26 * u
        b.fill(Path().rect(0, ground, ctx.w, quay), Colors.darken(pal.bgBottom, 0.55f))
        val lamps = Path()
        var lx = 20 * u
        while (lx < ctx.w) { lamps.circle(lx, ground + 6 * u, 2.6 * u); lx += 54 * u }
        b.fill(lamps, Colors.hex("#FFC27A"), 0.9f)
        val sea = Colors.mix(pal.bgBottom, pal.accentA, 0.2f)
        b.fill(Path().rect(0, quay, ctx.w, ctx.h), LinearFill(0f, quay.toFloat(), 0f, ctx.h.toFloat(), intArrayOf(sea, Colors.darken(pal.bgBottom, 0.45f)), floatArrayOf(0f, 1f)))

        // The mahya itself between the two minarets' upper balconies.
        val (ma, mb) = c.minarets.sortedBy { it.x }.let { it.first() to it.last() }
        val left = ma.x + ma.w * 1.4
        val right = mb.x - mb.w * 1.4
        val ropeY = maxOf(ma.balconies.first(), mb.balconies.first()) + 14 * s
        val text = (ctx.options.text?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_TEXT).uppercase(TurkishText.TR)
        // It hangs clear of the dome, however many lines her words need.
        val domeTop = c.domes.minOf { it.base - it.h } - 40 * s
        val (lines, pitch) = MahyaFont.layout(text, right - left, domeTop - ropeY, 10 * u, 17 * u)
        val bulbs = Path(); val halos = Path(); val ropes = Path()
        val sag = 10 * u
        fun sagAt(x: Double) = sag * sin(PI * ((x - left) / (right - left)).coerceIn(0.0, 1.0))
        for ((i, line) in lines.withIndex()) {
            val cols = MahyaFont.width(line)
            val x0 = (left + right) / 2 - (cols - 1) * pitch / 2
            val y0 = ropeY + i * MahyaFont.LINE_ROWS * pitch
            // The rope each line hangs from, minaret to minaret.
            val rope = (0..24).map { k -> val x = left - ma.w + (right - left + 2 * ma.w) * k / 24.0; Vec(x, y0 - pitch * 0.4 + sagAt(x)) }
            ropes.moveTo(rope[0].x, rope[0].y); for (v in rope.drop(1)) ropes.lineTo(v.x, v.y)
            for ((cx, cy) in MahyaFont.dots(line)) {
                val x = x0 + cx * pitch
                val y = y0 + cy * pitch + sagAt(x)
                bulbs.circle(x, y, pitch * 0.27)
                halos.circle(x, y, pitch * 0.75)
            }
        }
        b.stroke(ropes, Colors.darken(pal.bgBottom, 0.6f), (1.2 * u).toFloat(), 0.6f)
        b.fill(halos, pal.glow, 0.16f)
        b.fill(bulbs, Colors.hex("#FFF3D2"))

        // The mosque's lights in the water, and a ferry going by.
        val streaks = Path()
        for (m in c.minarets) for (y in m.balconies) {
            var yy = quay + 6 * u
            while (yy < ctx.h) {
                val half = (4 + r.nextDouble() * 14) * u
                streaks.rect(m.x - half, yy, m.x + half, yy + 2 * u)
                yy += (10 + r.nextDouble() * 22) * u
            }
        }
        b.fill(streaks, pal.glow, 0.3f)
        val (fb, fw, ff) = PendikArt.vapur(ctx.x(0.24), ctx.y(0.86), 230 * u)
        b.fill(fb, Colors.darken(pal.bgBottom, 0.65f))
        b.fill(ff, Colors.darken(pal.bgBottom, 0.65f))
        b.fill(fw, Colors.lighten(pal.glow, 0.2f), 0.9f)
        if (ctx.options.captions) {
            b.text(TextItem("Pendik'te Ramazan", FontId.LATIN, (30 * u).toFloat(), ctx.cx.toFloat(), ctx.y(0.95).toFloat(), SolidFill(pal.line), alpha = 0.8f, letterSpacing = 0.12f))
        }
        Textures.grainOverlay(b, ctx, 0.25f)
        Common.vignette(b, ctx, 0.35f)
        return b.build()
    }
}

/** A 5×7 bulb alphabet for mahya, with Turkish letters' dots, breve and cedilla on extra rows. */
internal object MahyaFont {
    /** Rows a line of letters takes: accent row, seven letter rows, cedilla row, and a gap. */
    const val LINE_ROWS = 11

    private val GLYPHS: Map<Char, String> = mapOf(
        'A' to ".###.|#...#|#...#|#####|#...#|#...#|#...#",
        'B' to "####.|#...#|#...#|####.|#...#|#...#|####.",
        'C' to ".###.|#...#|#....|#....|#....|#...#|.###.",
        'D' to "####.|#...#|#...#|#...#|#...#|#...#|####.",
        'E' to "#####|#....|#....|####.|#....|#....|#####",
        'F' to "#####|#....|#....|####.|#....|#....|#....",
        'G' to ".###.|#...#|#....|#.###|#...#|#...#|.###.",
        'H' to "#...#|#...#|#...#|#####|#...#|#...#|#...#",
        'I' to ".###.|..#..|..#..|..#..|..#..|..#..|.###.",
        'J' to "..###|...#.|...#.|...#.|...#.|#..#.|.##..",
        'K' to "#...#|#..#.|#.#..|##...|#.#..|#..#.|#...#",
        'L' to "#....|#....|#....|#....|#....|#....|#####",
        'M' to "#...#|##.##|#.#.#|#.#.#|#...#|#...#|#...#",
        'N' to "#...#|#...#|##..#|#.#.#|#..##|#...#|#...#",
        'O' to ".###.|#...#|#...#|#...#|#...#|#...#|.###.",
        'P' to "####.|#...#|#...#|####.|#....|#....|#....",
        'Q' to ".###.|#...#|#...#|#...#|#.#.#|#..#.|.##.#",
        'R' to "####.|#...#|#...#|####.|#.#..|#..#.|#...#",
        'S' to ".####|#....|#....|.###.|....#|....#|####.",
        'T' to "#####|..#..|..#..|..#..|..#..|..#..|..#..",
        'U' to "#...#|#...#|#...#|#...#|#...#|#...#|.###.",
        'V' to "#...#|#...#|#...#|#...#|#...#|.#.#.|..#..",
        'W' to "#...#|#...#|#...#|#.#.#|#.#.#|#.#.#|.#.#.",
        'X' to "#...#|#...#|.#.#.|..#..|.#.#.|#...#|#...#",
        'Y' to "#...#|#...#|.#.#.|..#..|..#..|..#..|..#..",
        'Z' to "#####|....#|...#.|..#..|.#...|#....|#####",
        '0' to ".###.|#...#|#..##|#.#.#|##..#|#...#|.###.",
        '1' to "..#..|.##..|..#..|..#..|..#..|..#..|.###.",
        '2' to ".###.|#...#|....#|...#.|..#..|.#...|#####",
        '3' to "####.|....#|....#|.###.|....#|....#|####.",
        '4' to "...#.|..##.|.#.#.|#..#.|#####|...#.|...#.",
        '5' to "#####|#....|####.|....#|....#|#...#|.###.",
        '6' to "..##.|.#...|#....|####.|#...#|#...#|.###.",
        '7' to "#####|....#|...#.|..#..|.#...|.#...|.#...",
        '8' to ".###.|#...#|#...#|.###.|#...#|#...#|.###.",
        '9' to ".###.|#...#|#...#|.####|....#|...#.|.##..",
        '♡' to ".....|.#.#.|#####|#####|.###.|..#..|.....",
        '♥' to ".....|.#.#.|#####|#####|.###.|..#..|.....",
        '!' to "..#..|..#..|..#..|..#..|..#..|.....|..#..",
        '.' to ".....|.....|.....|.....|.....|.....|..#..",
        '·' to ".....|.....|.....|..#..|.....|.....|.....",
        '-' to ".....|.....|.....|.###.|.....|.....|.....",
        '\'' to "..#..|..#..|.....|.....|.....|.....|.....",
    )

    /** Turkish letters: (base letter, mark above, mark below). */
    private val MARKED: Map<Char, Triple<Char, String?, String?>> = mapOf(
        'Ç' to Triple('C', null, "..#.."),
        'Ş' to Triple('S', null, "..#.."),
        'Ğ' to Triple('G', ".###.", null),
        'İ' to Triple('I', "..#..", null),
        'Ö' to Triple('O', ".#.#.", null),
        'Ü' to Triple('U', ".#.#.", null),
        'Â' to Triple('A', "..#..", null),
        'Î' to Triple('I', "..#..", null),
        'Û' to Triple('U', "..#..", null),
    )

    private const val SPACE_COLS = 4

    private fun advance(ch: Char) = if (ch == ' ' || !known(ch)) SPACE_COLS else 6
    private fun known(ch: Char) = ch in GLYPHS || ch in MARKED

    /** Width of a line in bulb columns. */
    fun width(line: String): Int = (line.sumOf { advance(it) } - 1).coerceAtLeast(1)

    /** The lit bulbs of a line as (column, row); row 0 is the accent row above the letters. */
    fun dots(line: String): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        var col = 0
        for (ch in line) {
            if (known(ch)) {
                val (base, above, below) = MARKED[ch] ?: Triple(ch, null, null)
                val rows = GLYPHS.getValue(base).split('|')
                fun put(row: String, y: Int) = row.forEachIndexed { x, c -> if (c == '#') out += (col + x) to y }
                above?.let { put(it, 0) }
                rows.forEachIndexed { y, row -> put(row, y + 1) }
                below?.let { put(it, 8) }
            }
            col += advance(ch)
        }
        return out
    }

    /**
     * Splits [text] into as few lines as keep the bulbs at least [minPitch]
     * apart across [span] and within [height], at most [maxPitch] apart.
     * Returns the lines and the pitch.
     */
    fun layout(text: String, span: Double, height: Double, minPitch: Double, maxPitch: Double): Pair<List<String>, Double> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }.ifEmpty { listOf(" ") }
        var best: Pair<List<String>, Double>? = null
        for (n in 1..minOf(4, words.size)) {
            val lines = balance(words, n)
            val pitch = minOf(maxPitch, span / (lines.maxOf { width(it) } + 1), height / (n * LINE_ROWS - 2))
            if (best == null || pitch > best.second) best = lines to pitch
            if (pitch >= minPitch) return lines to pitch
        }
        return best!!
    }

    /** [words] in [n] lines with the widest line as narrow as it can be (few words, so every split is tried). */
    private fun balance(words: List<String>, n: Int): List<String> {
        var best: List<String> = listOf(words.joinToString(" "))
        var bestW = Int.MAX_VALUE
        fun go(start: Int, left: Int, acc: List<String>) {
            if (left == 1) {
                val lines = acc + words.subList(start, words.size).joinToString(" ")
                val w = lines.maxOf { width(it) }
                if (w < bestW) { bestW = w; best = lines }
                return
            }
            for (end in start + 1..words.size - left + 1) go(end, left - 1, acc + words.subList(start, end).joinToString(" "))
        }
        if (words.size <= 12) go(0, n, emptyList()) else return words.chunked((words.size + n - 1) / n).map { it.joinToString(" ") }
        return best
    }
}
