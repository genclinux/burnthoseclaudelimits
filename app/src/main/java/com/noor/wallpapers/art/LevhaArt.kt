package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hat levhası: a calligraphy panel mounted the traditional way. Ink on
 * burnished ahar paper, gold cetvel rules, gilded corner pieces (köşebent),
 * the whole sheet set into an ebru margin. A small red seal carries Hanife
 * Betül's initials, ح ب.
 *
 * On the Onyx palette it becomes a zerendüd levha: gold calligraphy on black paper.
 */
object LevhaArt {
    class Params(
        val phrase: Phrase,
        val script: CalligraphyArt.Script = CalligraphyArt.Script.NASKH,
        val ebru: Ebru.Style = Ebru.Style.BATTAL,
        /** Wide panels suit long verses; tall ones suit a word or two. */
        val wide: Boolean = true,
    )

    private const val INK = 0xFF1B1611.toInt()
    private const val SEPIA = 0xFF5A4630.toInt()
    private const val SEAL_RED = 0xFFA3272A.toInt()

    /** Geometry and inks of the mounted sheet, for whatever is written on it. */
    class Sheet(
        val innerL: Double, val innerT: Double, val innerW: Double, val innerH: Double,
        val gold: Int, val ink: Int, val noteColor: Int, val noteWidth: Double, val night: Boolean,
    )

    fun scene(ctx: RenderContext, p: Params): Scene = panel(ctx, p.ebru, p.wide) { s -> writing(this, ctx, p, s) }

    /**
     * The levha without its writing: ebru margin, aged ahar paper, gold cetvel,
     * köşebent corners and the ح ب seal. [content] writes on the sheet.
     */
    fun panel(ctx: RenderContext, ebru: Ebru.Style, wide: Boolean, content: SceneBuilder.(Sheet) -> Unit): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        val night = pal.id == "onyx"

        // The ebru margin: the palette's pigments, softened toward the paper as margin papers usually are.
        val (pigments, water) = Ebru.pigments(pal)
        val soft = pigments.map { Colors.mix(it, water, 0.42f) }
        b.raster(Ebru.sheet(ebru, soft, water, ctx.seed + 400, 0.0, 0.0, ctx.w, ctx.h, maxPixels = 260_000))
        Textures.grainOverlay(b, ctx, 0.5f)

        // Panel geometry inside the safe area.
        val pw = minOf(ctx.safeW, ctx.safeH) * 0.86
        val ph = if (wide) pw * 0.66 else minOf(pw * 1.18, ctx.safeH * 0.86)
        val left = ctx.cx - pw / 2; val top = ctx.cy - ph / 2
        val right = left + pw; val bottom = top + ph

        // Soft shadow so the sheet sits on the margin.
        for (k in 1..6) {
            val g = k * 5 * u
            b.fill(Path().rect(left - g * 0.4, top + g * 0.2, right + g * 0.6, bottom + g), Colors.BLACK, 0.045f)
        }

        // Paper: slightly irregular edge (cut by hand), aged mottling and a burnished centre.
        val paperColor = if (night) 0xFF17130F.toInt() else 0xFFEFE2C4.toInt()
        val sheet = Brush.toPath(Brush.wobble(Brush.flatten(Path().rect(left, top, right, bottom), 6.0), 1.6 * u, 90 * u, ctx.seed + 3), true)
        b.fill(sheet, paperColor)
        b.fill(
            sheet,
            RadialFill(
                ctx.cx.toFloat(), ctx.cy.toFloat(), (pw * 0.75).toFloat(),
                intArrayOf(Colors.withAlpha(Colors.WHITE, if (night) 0.04f else 0.35f), 0, Colors.withAlpha(SEPIA, if (night) 0.0f else 0.22f)),
                floatArrayOf(0f, 0.6f, 1f),
            ),
        )
        b.items += GroupItem(listOf(agedPaper(ctx, left, top, right, bottom, night)), sheet)

        // Cetvel: an outer coloured hairline, the gold rule, and a black inner hairline.
        val gold = if (night) Colors.hex("#D8B45A") else pal.line.let { Colors.mix(it, Colors.hex("#C9A04A"), 0.5f) }
        fun inset(d: Double) = Path().rect(left + d, top + d, right - d, bottom - d)
        b.stroke(inset(22 * u), pal.accentA, (1.6 * u).toFloat(), 0.85f, round = false)
        val ruleOuter = inset(30 * u); val ruleInner = inset(38 * u)
        val goldBand = Path().apply { append(ruleOuter); append(ruleInner) }.also { it.evenOdd = true }
        b.fill(goldBand, gold)
        Textures.gild(b, ctx, goldBand, gold, 0.85f)
        b.fill(Brush.ink(ruleOuter, 1.1 * u, ctx.seed + 11), if (night) Colors.darken(gold, 0.6f) else INK, 0.8f)
        b.fill(Brush.ink(ruleInner, 1.1 * u, ctx.seed + 12), if (night) Colors.darken(gold, 0.6f) else INK, 0.8f)
        b.fill(Brush.ink(inset(46 * u), 1.4 * u, ctx.seed + 13), if (night) gold else INK, 0.9f)

        // Köşebent: gilded quarter-rosettes in the four corners.
        val cornerR = minOf(pw, ph) * 0.17
        for ((i, corner) in listOf(Vec(left, top), Vec(right, top), Vec(right, bottom), Vec(left, bottom)).withIndex()) {
            val c = corner + Vec(if (i == 0 || i == 3) 46 * u else -46 * u, if (i < 2) 46 * u else -46 * u)
            val start = PI / 2 * i
            kosebent(b, ctx, c, cornerR, start, gold, pal.accentA, pal.accentB, night, ctx.seed + 20 + i)
        }

        // The writing area, and the seal's corner kept clear for small notes at the foot.
        val innerL = left + 70 * u; val innerR = right - 70 * u
        val innerT = top + 70 * u; val innerB = bottom - 70 * u
        val innerW = innerR - innerL; val innerH = innerB - innerT
        val sealX = right - 46 * u - cornerR - 34 * u
        val sealY = bottom - 46 * u - 34 * u
        val noteWidth = minOf(innerW * 0.8, 2 * (sealX - 46 * u - ctx.cx))
        val noteColor = if (night) Colors.darken(gold, 0.15f) else SEPIA
        b.content(Sheet(innerL, innerT, innerW, innerH, gold, if (night) gold else INK, noteColor, noteWidth, night))

        // Seal (mühür) with her initials, stamped slightly crooked near the corner.
        seal(b, ctx, sealX, sealY, 24 * u, ctx.seed + 31)

        // Paper tooth and ink texture over the whole sheet.
        Textures.grainOverlay(b, ctx, if (night) 0.35f else 0.7f, clip = sheet)
        Common.vignette(b, ctx, 0.35f)
        return b.build()
    }

    /** The calligraphy, with its reading and meaning small in sepia at the foot. */
    private fun writing(b: SceneBuilder, ctx: RenderContext, p: Params, s: Sheet) {
        val u = ctx.u
        val (font, text) = when (p.script) {
            CalligraphyArt.Script.NASKH -> FontId.NASKH_BOLD to p.phrase.arabic
            CalligraphyArt.Script.RUQAA -> FontId.RUQAA to p.phrase.bare
            CalligraphyArt.Script.KUFI -> FontId.KUFI to p.phrase.bare
        }
        // Without captions the calligraphy takes the whole sheet.
        val captions = ctx.options.captions
        val textCy = s.innerT + s.innerH * (if (captions) 0.44 else 0.5)
        val ink = s.ink
        b.text(
            TextItem(
                text, font, (s.innerH * 0.6).toFloat(), ctx.cx.toFloat(), textCy.toFloat(),
                LinearFill(
                    0f, (textCy - s.innerH * 0.3).toFloat(), 0f, (textCy + s.innerH * 0.3).toFloat(),
                    intArrayOf(Colors.lighten(ink, if (s.night) 0.25f else 0.08f), ink, Colors.darken(ink, 0.1f)),
                ),
                maxWidth = (s.innerW * 0.84).toFloat(),
                maxHeight = (s.innerH * (if (captions) 0.56 else 0.66)).toFloat(),
                inkCentered = true,
            ),
        )
        if (!captions) return
        b.text(
            TextItem(
                p.phrase.transliteration, FontId.LATIN, (30 * u).toFloat(), ctx.cx.toFloat(), (s.innerT + s.innerH * 0.83).toFloat(),
                SolidFill(s.noteColor), maxWidth = s.noteWidth.toFloat(), alpha = 0.9f, letterSpacing = 0.03f,
            ),
        )
        b.text(
            TextItem(
                p.phrase.meaning, FontId.LATIN, (24 * u).toFloat(), ctx.cx.toFloat(), (s.innerT + s.innerH * 0.92).toFloat(),
                SolidFill(s.noteColor), maxWidth = s.noteWidth.toFloat(), alpha = 0.75f,
            ),
        )
    }

    /** Age: soft foxing and tide marks on the paper, multiplied in. */
    private fun agedPaper(ctx: RenderContext, l: Double, t: Double, r: Double, bt: Double, night: Boolean): Item {
        val w = (r - l); val h = (bt - t)
        val scale = maxOf(1.0, sqrt(w * h / 60_000))
        val tw = (w / scale).toInt(); val th = (h / scale).toInt()
        val n = Noise(ctx.seed + 51)
        val px = IntArray(tw * th)
        for (y in 0 until th) for (x in 0 until tw) {
            val m = n.fbm(x * scale / (w * 0.35), y * scale / (h * 0.35), 4)
            val edge = minOf(x, y, tw - 1 - x, th - 1 - y) / (minOf(tw, th) * 0.12)
            val age = (1 - m) * 0.22 + (1 - minOf(1.0, edge)) * 0.18
            val v = (255 * (1 - age * if (night) 0.3 else 1.0)).toInt().coerceIn(0, 255)
            px[y * tw + x] = Colors.argb(255, v, (v * 0.97).toInt(), (v * 0.9).toInt())
        }
        return RasterItem(tw, th, px, l.toFloat(), t.toFloat(), r.toFloat(), bt.toFloat(), alpha = 0.8f, blend = Blend.MULTIPLY)
    }

    /** A gilded quarter-rosette of rumi-like lobes, inked by hand, with a few coloured dots. */
    private fun kosebent(
        b: SceneBuilder, ctx: RenderContext, c: Vec, r: Double, start: Double,
        gold: Int, accentA: Int, accentB: Int, night: Boolean, seed: Int,
    ) {
        val lobes = 5
        val shape = Path().moveTo(c.x, c.y)
        for (k in 0..lobes) {
            val a0 = start + PI / 2 * k / lobes
            val a1 = start + PI / 2 * (k + 0.5) / lobes
            val p0 = c + Vec.polar(r * 0.78, a0)
            val tip = c + Vec.polar(r, a1)
            val p1 = c + Vec.polar(r * 0.78, start + PI / 2 * (k + 1) / lobes)
            if (k == 0) shape.lineTo(p0.x, p0.y)
            if (k < lobes) shape.quadTo(tip.x + (tip.x - c.x) * 0.05, tip.y + (tip.y - c.y) * 0.05, p1.x, p1.y)
        }
        shape.close()
        b.fill(shape, gold)
        Textures.gild(b, ctx, shape, gold, 0.9f)
        // Inner lobe and a hatayi bud, in colour.
        val inner = Path().moveTo(c.x, c.y)
        for (k in 0..8) {
            val a = start + PI / 2 * k / 8
            val rr = r * (0.45 + 0.08 * cos(a * 8))
            val p = c + Vec.polar(rr, a)
            inner.lineTo(p.x, p.y)
        }
        inner.close()
        b.fill(inner, if (night) Colors.darken(gold, 0.45f) else accentA, 0.85f)
        val bud = c + Vec.polar(r * 0.22, start + PI / 4)
        b.fill(Path().circle(bud.x, bud.y, r * 0.09), if (night) gold else accentB, 0.9f)
        val rnd = ctx.random(seed)
        repeat(5) {
            val a = start + PI / 2 * (it + 0.5) / 5
            val p = c + Vec.polar(r * 0.64, a)
            b.fill(Path().circle(p.x, p.y, r * (0.025 + rnd.nextDouble() * 0.01)), if (night) Colors.lighten(gold, 0.4f) else accentB, 0.9f)
        }
        b.fill(Brush.ink(shape, 1.2 * ctx.u, seed), if (night) Colors.darken(gold, 0.55f) else INK, 0.85f)
        b.fill(Brush.ink(inner, 0.9 * ctx.u, seed + 1), if (night) Colors.darken(gold, 0.55f) else INK, 0.7f)
    }

    /** A small square seal in red with ح ب, slightly rotated and unevenly inked. */
    private fun seal(b: SceneBuilder, ctx: RenderContext, cx: Double, cy: Double, half: Double, seed: Int) {
        val rot = -0.06
        val corners = listOf(Vec(-half, -half), Vec(half, -half), Vec(half, half), Vec(-half, half)).map { it.rotated(rot) + Vec(cx, cy) }
        val shape = Brush.toPath(Brush.wobble(Brush.flatten(Path().polygon(corners), 2.0), half * 0.04, half * 0.6, seed), true)
        b.fill(shape, SEAL_RED, 0.85f, Blend.MULTIPLY)
        b.text(
            TextItem(
                "ح ب", FontId.NASKH_BOLD, (half * 1.2).toFloat(), cx.toFloat(), cy.toFloat(),
                SolidFill(0xFFF3E6CC.toInt()), maxWidth = (half * 1.6).toFloat(), maxHeight = (half * 1.3).toFloat(),
                inkCentered = true, alpha = 0.92f,
            ),
        )
        // Uneven stamp pressure: speckle the seal with paper showing through.
        val rnd = ctx.random(seed)
        val speck = Path()
        repeat(14) {
            val p = Vec(cx, cy) + Vec(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5) * (half * 1.8)
            speck.circle(p.x, p.y, half * (0.02 + rnd.nextDouble() * 0.04))
        }
        b.items += GroupItem(listOf(FillItem(speck, SolidFill(0xFFEFE2C4.toInt()), 0.6f)), shape)
    }

    fun entries(): List<Entry> {
        fun levha(phrase: Phrase, palette: String, script: CalligraphyArt.Script, ebru: Ebru.Style, wide: Boolean, seed: Int, id: String = phrase.id) =
            Entry(
                "levha-$id", phrase.transliteration.substringBefore(" ·"), Category.LEVHA, Palette.byId(palette), seed,
            ) { ctx -> scene(ctx, Params(phrase, script, ebru, wide)) }
        val N = CalligraphyArt.Script.NASKH
        val R = CalligraphyArt.Script.RUQAA
        return listOf(
            levha(Phrases.HANIFE_BETUL, "betul", N, Ebru.Style.BATTAL, wide = true, seed = 12),
            levha(Phrases.BISMILLAH, "emerald", N, Ebru.Style.BATTAL, wide = true, seed = 3),
            levha(Phrases.ALLAH, "lapis", N, Ebru.Style.SAL, wide = false, seed = 5),
            levha(Phrases.MUHAMMAD, "emerald", N, Ebru.Style.GELGIT, wide = false, seed = 7),
            levha(Phrases.TAWHID, "alhambra", N, Ebru.Style.BATTAL, wide = true, seed = 9),
            levha(Phrases.SUBHANALLAH, "iznik", R, Ebru.Style.TARAKLI, wide = true, seed = 4),
            levha(Phrases.ALHAMDULILLAH, "isfahan", R, Ebru.Style.BATTAL, wide = true, seed = 14),
            levha(Phrases.ALLAHU_AKBAR, "sand", N, Ebru.Style.GELGIT, wide = true, seed = 15),
            levha(Phrases.MASHALLAH, "betul", R, Ebru.Style.BULBUL_YUVASI, wide = true, seed = 8),
            levha(Phrases.YUSRA, "amethyst", N, Ebru.Style.SAL, wide = true, seed = 10),
            levha(Phrases.HASBUNALLAH, "onyx", N, Ebru.Style.BATTAL, wide = true, seed = 2),
            levha(Phrases.DHIKR, "lapis", N, Ebru.Style.BATTAL, wide = true, seed = 16),
            levha(Phrases.MAAKUM, "emerald", N, Ebru.Style.TARAKLI, wide = true, seed = 18),
            levha(Phrases.HANIF, "iznik", N, Ebru.Style.BATTAL, wide = true, seed = 19),
            levha(Phrases.TABATTAL, "betul", N, Ebru.Style.GELGIT, wide = true, seed = 20),
            levha(Phrases.ALLAH, "onyx", N, Ebru.Style.BATTAL, wide = false, seed = 22, id = "allah-zerendud"),
        )
    }
}
