package com.noor.wallpapers.art

import kotlin.math.PI

/** A phrase with full vocalisation for Naskh, a bare form for Kufi, its Turkish reading and its meaning. */
class Phrase(
    val id: String,
    val arabic: String,
    val bare: String,
    val transliteration: String,
    val meaning: String,
    /** Optional small line under the meaning. */
    val note: String? = null,
)

object Phrases {
    val BISMILLAH = Phrase(
        "bismillah", "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "بسم الله الرحمن الرحيم",
        "Bismillâhirrahmânirrahîm", "Rahmân ve Rahîm olan Allah'ın adıyla",
    )
    val ALLAH = Phrase("allah", "ٱللَّٰه", "الله", "Allah", "Celle Celâlühû")
    val MUHAMMAD = Phrase("muhammad", "مُحَمَّدٌ", "محمد", "Muhammed", "Sallallâhu aleyhi ve sellem")
    val SUBHANALLAH = Phrase("subhanallah", "سُبْحَانَ ٱللَّٰهِ", "سبحان الله", "Sübhânallah", "Allah her türlü noksanlıktan münezzehtir")
    val ALHAMDULILLAH = Phrase("alhamdulillah", "ٱلْحَمْدُ لِلَّٰهِ", "الحمد لله", "Elhamdülillah", "Hamd Allah'a mahsustur")
    val ALLAHU_AKBAR = Phrase("allahuakbar", "ٱللَّٰهُ أَكْبَرُ", "الله أكبر", "Allahu Ekber", "Allah en büyüktür")
    val TAWHID = Phrase("tawhid", "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", "لا إله إلا الله", "Lâ ilâhe illallah", "Allah'tan başka ilah yoktur")
    val MASHALLAH = Phrase("mashallah", "مَا شَاءَ ٱللَّٰهُ", "ما شاء الله", "Mâşallah", "Allah'ın dilediği olur")
    val YUSRA = Phrase(
        "yusra", "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "إن مع العسر يسرا",
        "İnne meal usri yüsrâ", "Şüphesiz zorlukla beraber bir kolaylık vardır · İnşirah 94:6",
    )
    val HASBUNALLAH = Phrase(
        "hasbunallah", "حَسْبُنَا ٱللَّٰهُ وَنِعْمَ ٱلْوَكِيلُ", "حسبنا الله ونعم الوكيل",
        "Hasbünallâhu ve ni'mel vekîl", "Allah bize yeter, O ne güzel vekildir · Âl-i İmrân 3:173",
    )
    val DHIKR = Phrase(
        "dhikr", "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ", "فاذكروني أذكركم",
        "Fezkürûnî ezkürküm", "Beni anın ki ben de sizi anayım · Bakara 2:152",
    )
    val MAAKUM = Phrase(
        "maakum", "وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ", "وهو معكم أين ما كنتم",
        "Ve hüve meaküm eyne mâ küntüm", "Nerede olursanız olun, O sizinle beraberdir · Hadîd 57:4",
    )
    // Hanife Betül's collection. Hanîf appears in the Qur'an itself (for Ibrahim's faith,
    // and in Rum 30:30). Betül does not: al-Muzzammil 73:8 has tabattal, from the same
    // b-t-l root, so the wallpaper says so rather than presenting it as her name's verse.
    // Wording reviewed against Diyanet / TDV definitions.
    val HANIFE_BETUL = Phrase(
        "hanife-betul", "حَنِيفَة بَتُول", "حنيفة بتول",
        "Hanife Betül", "Hanîf: yalnız Allah'a yönelen · Betül: iffetli, Allah'a gönülden yönelmiş",
    )
    val HANIF = Phrase(
        "hanif", "فَأَقِمْ وَجْهَكَ لِلدِّينِ حَنِيفًا", "فأقم وجهك للدين حنيفا",
        "Fe ekım vecheke lid-dîni hanîfâ", "Yüzünü hanîf olarak dine çevir · Rûm 30:30",
    )
    val TABATTAL = Phrase(
        "tabattal", "وَتَبَتَّلْ إِلَيْهِ تَبْتِيلًا", "وتبتل إليه تبتيلا",
        "Ve tebettel ileyhi tebtîlâ", "Bütün benliğinle O'na yönel · Müzzemmil 73:8",
        note = "Betül ile aynı ب-ت-ل kökünden",
    )
    val NUR = Phrase("nur", "نُور", "نور", "Nur", "YOLUN NUR OLSUN, HANİFE BETÜL")
    // The verse of light, the "noor" in HBSnoor.
    val NUR_VERSE = Phrase(
        "nur-ayeti", "ٱللَّهُ نُورُ ٱلسَّمَٰوَٰتِ وَٱلْأَرْضِ", "الله نور السماوات والأرض",
        "Allâhu nûrus-semâvâti vel-ard", "Allah göklerin ve yerin nurudur · Nûr 24:35",
    )
    val RAMADAN = Phrase("ramadan", "رَمَضَانُ كَرِيمٌ", "رمضان كريم", "Ramazan-ı Kerîm", "HAYIRLI RAMAZANLAR")
    val EID = Phrase("eid", "عِيدٌ مُبَارَكٌ", "عيد مبارك", "Îd Mübârek", "BAYRAMINIZ MÜBAREK OLSUN")
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

        val cx = ctx.cx
        val cy = ctx.cy
        val r = minOf(ctx.safeW * 0.40, ctx.safeH * 0.27)
        val hw = ctx.safeW * 0.44
        val hh = minOf(ctx.safeW * 0.30, ctx.safeH * 0.27)
        if (p.medallion) medallion(b, ctx, cx, cy, r) else frame(b, ctx, cx, cy, hw, hh)

        val (font, text) = when (p.script) {
            Script.NASKH -> FontId.NASKH_BOLD to p.phrase.arabic
            Script.RUQAA -> FontId.RUQAA to p.phrase.bare
            Script.KUFI -> FontId.KUFI to p.phrase.bare
        }
        val maxWidth = if (p.medallion) r * 1.55 else hw * 1.8
        val size = if (p.medallion) 250 * ctx.u else 170 * ctx.u
        b.text(
            TextItem(
                text, font, size.toFloat(), cx.toFloat(), cy.toFloat(),
                LinearFill(
                    0f, (cy - size / 2).toFloat(), 0f, (cy + size / 2).toFloat(),
                    intArrayOf(Colors.lighten(pal.line, 0.35f), pal.line, Colors.darken(pal.line, 0.15f)),
                ),
                maxWidth = maxWidth.toFloat(),
                glowColor = Colors.withAlpha(pal.glow, 0.55f), glowRadius = (18 * ctx.u).toFloat(),
                // Stay inside the medallion's disc / the frame, marks and descenders included.
                maxHeight = (if (p.medallion) r * 0.95 else hh * 1.3).toFloat(),
                inkCentered = true,
            ),
        )

        if (p.showMeaning && ctx.options.captions) {
            val y = cy + (if (p.medallion) r else hh + 130 * ctx.u) + 150 * ctx.u
            b.text(
                TextItem(
                    p.phrase.transliteration, FontId.LATIN, (40 * ctx.u).toFloat(), cx.toFloat(), y.toFloat(),
                    SolidFill(pal.line), maxWidth = (ctx.safeW * 0.84).toFloat(), alpha = 0.95f, letterSpacing = 0.04f,
                ),
            )
            b.text(
                TextItem(
                    p.phrase.meaning, FontId.LATIN, (32 * ctx.u).toFloat(), cx.toFloat(), (y + 62 * ctx.u).toFloat(),
                    SolidFill(Colors.lighten(pal.line, 0.5f)), maxWidth = (ctx.safeW * 0.84).toFloat(), alpha = 0.75f,
                ),
            )
            p.phrase.note?.let { note ->
                b.text(
                    TextItem(
                        note, FontId.LATIN, (26 * ctx.u).toFloat(), cx.toFloat(), (y + 112 * ctx.u).toFloat(),
                        SolidFill(Colors.lighten(pal.line, 0.5f)), maxWidth = (ctx.safeW * 0.84).toFloat(), alpha = 0.55f,
                    ),
                )
            }
        }
        Textures.grainOverlay(b, ctx, 0.35f)
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
    fun frame(b: SceneBuilder, ctx: RenderContext, cx: Double, cy: Double, hw: Double, hh: Double) {
        val pal = ctx.palette
        Common.glow(b, cx, cy, hw * 1.6, pal.glow, 0.15f)
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
