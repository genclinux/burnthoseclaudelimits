package com.noor.wallpapers.art

import kotlin.math.PI

/** A phrase with full vocalisation for Naskh, a bare form for Kufi, and its meaning. */
class Phrase(
    val id: String,
    val arabic: String,
    val bare: String,
    val transliteration: String,
    val meaning: String,
)

object Phrases {
    val BISMILLAH = Phrase(
        "bismillah", "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "بسم الله الرحمن الرحيم",
        "Bismillāhir-Raḥmānir-Raḥīm", "In the name of God, the Most Gracious, the Most Merciful",
    )
    val ALLAH = Phrase("allah", "ٱللَّٰه", "الله", "Allāh", "God")
    val MUHAMMAD = Phrase("muhammad", "مُحَمَّدٌ", "محمد", "Muḥammad", "Peace and blessings be upon him")
    val SUBHANALLAH = Phrase("subhanallah", "سُبْحَانَ ٱللَّٰهِ", "سبحان الله", "Subḥān Allāh", "Glory be to God")
    val ALHAMDULILLAH = Phrase("alhamdulillah", "ٱلْحَمْدُ لِلَّٰهِ", "الحمد لله", "Al-ḥamdu lillāh", "All praise is due to God")
    val ALLAHU_AKBAR = Phrase("allahuakbar", "ٱللَّٰهُ أَكْبَرُ", "الله أكبر", "Allāhu akbar", "God is the Greatest")
    val TAWHID = Phrase("tawhid", "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", "لا إله إلا الله", "Lā ilāha illā Allāh", "There is no god but God")
    val MASHALLAH = Phrase("mashallah", "مَا شَاءَ ٱللَّٰهُ", "ما شاء الله", "Mā shā’ Allāh", "What God has willed")
    val YUSRA = Phrase(
        "yusra", "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "إن مع العسر يسرا",
        "Inna ma‘al-‘usri yusrā", "Indeed, with hardship comes ease · 94:6",
    )
    val HASBUNALLAH = Phrase(
        "hasbunallah", "حَسْبُنَا ٱللَّٰهُ وَنِعْمَ ٱلْوَكِيلُ", "حسبنا الله ونعم الوكيل",
        "Ḥasbunallāhu wa ni‘mal-wakīl", "God is sufficient for us, and He is the best Guardian · 3:173",
    )
    val DHIKR = Phrase(
        "dhikr", "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ", "فاذكروني أذكركم",
        "Fadhkurūnī adhkurkum", "So remember Me; I will remember you · 2:152",
    )
    val MAAKUM = Phrase(
        "maakum", "وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ", "وهو معكم أين ما كنتم",
        "Wa huwa ma‘akum ayna mā kuntum", "And He is with you wherever you are · 57:4",
    )
    val RAMADAN = Phrase("ramadan", "رَمَضَانُ كَرِيمٌ", "رمضان كريم", "Ramaḍān Karīm", "Ramadan Kareem")
    val EID = Phrase("eid", "عِيدٌ مُبَارَكٌ", "عيد مبارك", "‘Īd Mubārak", "Eid Mubarak")
}

object CalligraphyArt {
    enum class Script { NASKH, RUQAA, KUFI }

    class Params(
        val phrase: Phrase,
        val script: Script,
        val backdrop: Tiling = Tiling.OCTAGON_SQUARE,
        val showMeaning: Boolean = true,
        /** Large phrases (one or two words) get a medallion; long verses get a frame. */
        val medallion: Boolean = true,
    )

    fun scene(ctx: RenderContext, p: Params): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        Common.background(b, ctx)
        val backdrop = GeometricArt.Params(p.backdrop, PatternStyle.LINEWORK, edge = 92.0)
        GeometricArt.drawPattern(b, ctx, backdrop, GeometricArt.tiles(ctx, backdrop), alpha = 0.35f)
        Common.vignette(b, ctx, 0.65f)

        val cx = ctx.w / 2
        val cy = ctx.h * 0.5
        if (p.medallion) medallion(b, ctx, cx, cy, ctx.w * 0.40) else frame(b, ctx, cx, cy)

        val (font, text) = when (p.script) {
            Script.NASKH -> FontId.NASKH_BOLD to p.phrase.arabic
            Script.RUQAA -> FontId.RUQAA to p.phrase.bare
            Script.KUFI -> FontId.KUFI to p.phrase.bare
        }
        val maxWidth = if (p.medallion) ctx.w * 0.62 else ctx.w * 0.80
        val size = if (p.medallion) 250 * ctx.u else 170 * ctx.u
        b.text(
            TextItem(
                text, font, size.toFloat(), cx.toFloat(), (cy + if (p.script == Script.NASKH) 0.0 else -10 * ctx.u).toFloat(),
                LinearFill(
                    0f, (cy - size / 2).toFloat(), 0f, (cy + size / 2).toFloat(),
                    intArrayOf(Colors.lighten(pal.line, 0.35f), pal.line, Colors.darken(pal.line, 0.15f)),
                ),
                maxWidth = maxWidth.toFloat(),
                glowColor = Colors.withAlpha(pal.glow, 0.55f), glowRadius = (18 * ctx.u).toFloat(),
            ),
        )

        if (p.showMeaning) {
            val y = cy + ctx.w * 0.40 + 150 * ctx.u
            b.text(
                TextItem(
                    p.phrase.transliteration, FontId.LATIN, (40 * ctx.u).toFloat(), cx.toFloat(), y.toFloat(),
                    SolidFill(pal.line), maxWidth = (ctx.w * 0.84).toFloat(), alpha = 0.95f, letterSpacing = 0.04f,
                ),
            )
            b.text(
                TextItem(
                    p.phrase.meaning, FontId.LATIN, (32 * ctx.u).toFloat(), cx.toFloat(), (y + 62 * ctx.u).toFloat(),
                    SolidFill(Colors.lighten(pal.line, 0.5f)), maxWidth = (ctx.w * 0.84).toFloat(), alpha = 0.75f,
                ),
            )
        }
        Common.topShade(b, ctx)
        return b.build()
    }

    /** Circular shamsa medallion: a 16-point star border, rings and a dark field for the text. */
    fun medallion(b: SceneBuilder, ctx: RenderContext, cx: Double, cy: Double, r: Double) {
        val pal = ctx.palette
        Common.glow(b, cx, cy, r * 1.6, pal.glow, 0.18f)
        b.fill(Common.star(cx, cy, r * 1.12, r * 0.94, 16, -PI / 2), pal.line, 0.9f)
        b.fill(Common.star(cx, cy, r * 1.08, r * 0.92, 16, -PI / 2), Colors.darken(pal.bgBottom, 0.1f))
        b.fill(Common.star(cx, cy, r * 1.0, r * 0.90, 16, -PI / 2 + PI / 16), pal.accentA, 0.75f)
        b.fill(Path().circle(cx, cy, r * 0.88), pal.line)
        b.fill(
            Path().circle(cx, cy, r * 0.855),
            RadialFill(
                cx.toFloat(), cy.toFloat(), (r * 0.855).toFloat(),
                intArrayOf(Colors.mix(pal.bgTop, pal.accentA, 0.25f), Colors.darken(pal.bgBottom, 0.15f)),
            ),
        )
        b.stroke(Path().circle(cx, cy, r * 0.80), pal.line, (3 * ctx.u).toFloat(), 0.7f)
        // Small rub el hizb at the four quarters of the ring.
        for (i in 0 until 4) {
            val a = -PI / 2 + i * PI / 2
            val p = Vec(cx, cy) + Vec.polar(r * 0.98, a)
            b.fill(Common.rubElHizb(p.x, p.y, r * 0.075), pal.glow)
        }
    }

    /** A tall rounded frame with corner stars, for long verses. */
    fun frame(b: SceneBuilder, ctx: RenderContext, cx: Double, cy: Double) {
        val pal = ctx.palette
        val hw = ctx.w * 0.44
        val hh = ctx.w * 0.30
        Common.glow(b, cx, cy, ctx.w * 0.7, pal.glow, 0.15f)
        fun box(inset: Double) = Path().rect(cx - hw + inset, cy - hh + inset, cx + hw - inset, cy + hh - inset)
        b.fill(box(0.0), Colors.darken(pal.bgBottom, 0.25f), 0.75f)
        b.stroke(box(0.0), pal.line, (6 * ctx.u).toFloat())
        b.stroke(box(18 * ctx.u), pal.line, (2 * ctx.u).toFloat(), 0.7f)
        for (sx in listOf(-1.0, 1.0)) for (sy in listOf(-1.0, 1.0)) {
            b.fill(Common.rubElHizb(cx + sx * hw, cy + sy * hh, 34 * ctx.u), pal.line)
            b.fill(Path().circle(cx + sx * hw, cy + sy * hh, 12 * ctx.u), pal.accentA)
        }
        for (sy in listOf(-1.0, 1.0)) {
            b.fill(Common.star(cx, cy + sy * hh, 40 * ctx.u, 18 * ctx.u, 8), pal.line)
        }
    }
}
