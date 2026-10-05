package com.noor.wallpapers.art

enum class Category(val title: String, val arabic: String) {
    HANIFE_BETUL("Hanife Betül ♡", "حنيفة بتول"),
    GEOMETRIC("Geometrik", "هندسة"),
    CALLIGRAPHY("Hat", "خط"),
    NIGHT("Gece Camileri", "ليل"),
    RAMADAN("Ramazan ve Bayram", "رمضان"),
    MIHRAB("Mihrap", "محراب"),
    EBRU("Ebru", "ابری"),
    LEVHA("Levha", "لوحة"),
}

/**
 * One wallpaper in the gallery. Every entry can be re-rendered in any [Palette]
 * and with any seed ("shuffle"), so the catalog is a starting point, not a limit.
 */
class Entry(
    val id: String,
    val title: String,
    val category: Category,
    val defaultPalette: Palette,
    val defaultSeed: Int = 1,
    private val builder: (RenderContext) -> Scene,
) {
    fun render(ctx: RenderContext): Scene {
        val scene = builder(ctx)
        val sig = SceneBuilder(ctx.width, ctx.height).apply { Common.signature(this, ctx) }
        return Scene(scene.width, scene.height, scene.items + sig.items)
    }
}

object Catalog {
    private fun pal(id: String) = Palette.byId(id)

    private fun geo(
        id: String, title: String, palette: String, tiling: Tiling, style: PatternStyle,
        edge: Double = 150.0, contact: Double = tiling.defaultContactDegrees,
    ) = Entry(id, title, Category.GEOMETRIC, pal(palette)) { ctx ->
        GeometricArt.scene(ctx, GeometricArt.Params(tiling, style, contact, edge))
    }

    val entries: List<Entry> = buildList {
        // Hanife Betül's own collection, first in the gallery.
        val hb = Category.HANIFE_BETUL
        val betul = Palette.BETUL
        add(Entry("hb-name", "Hanife Betül", hb, betul) { ctx ->
            CalligraphyArt.scene(ctx, CalligraphyArt.Params(Phrases.HANIFE_BETUL, CalligraphyArt.Script.NASKH, Tiling.DODECAGON_HEXAGON_SQUARE))
        })
        add(Entry("hb-hanif", "Hanîf · Rûm 30:30", hb, pal("emerald")) { ctx ->
            CalligraphyArt.scene(ctx, CalligraphyArt.Params(Phrases.HANIF, CalligraphyArt.Script.NASKH, Tiling.OCTAGON_SQUARE, medallion = false))
        })
        add(Entry("hb-tabattal", "Tebettül · Müzzemmil 73:8", hb, betul) { ctx ->
            CalligraphyArt.scene(ctx, CalligraphyArt.Params(Phrases.TABATTAL, CalligraphyArt.Script.NASKH, Tiling.DODECAGON_TRIANGLE, medallion = false))
        })
        add(Entry("hb-stars", "Betül'ün Gecesi", hb, betul, 7) { ctx ->
            NightArt.scene(ctx, NightArt.Params(NightArt.Architecture.OTTOMAN, NightArt.Sky.DUSK, water = true))
        })
        add(Entry("hb-nur", "Nur", hb, betul, 4) { ctx ->
            LanternArt.scene(ctx, LanternArt.Params(Phrases.NUR, CalligraphyArt.Script.NASKH))
        })
        add(Entry("hb-mihrab", "Hanife Betül Mihrabı", hb, betul) { ctx ->
            MihrabArt.scene(ctx, MihrabArt.Params(Tiling.DODECAGON_HEXAGON_SQUARE, PatternStyle.ZELLIGE, Tiling.OCTAGON_SQUARE, Phrases.HANIFE_BETUL))
        })
        add(Entry("hb-rosette", "Betül Gülü", hb, betul) { ctx ->
            GeometricArt.scene(ctx, GeometricArt.Params(Tiling.DODECAGON_TRIANGLE, PatternStyle.STRAPWORK, 72.0, 95.0))
        })

        // Geometric
        add(geo("geo-khatam", "Hâtem", "emerald", Tiling.SQUARE, PatternStyle.STRAPWORK, 230.0))
        add(geo("geo-rub-el-hizb", "Rub-ül Hizb", "lapis", Tiling.OCTAGON_SQUARE, PatternStyle.STRAPWORK, 120.0))
        add(geo("geo-fez", "Fes Çinisi", "iznik", Tiling.OCTAGON_SQUARE, PatternStyle.ZELLIGE, 105.0, 72.0))
        add(geo("geo-shamsa", "Şemse", "amethyst", Tiling.DODECAGON_TRIANGLE, PatternStyle.STRAPWORK, 95.0))
        add(geo("geo-rosette", "On İki Kollu Gül", "alhambra", Tiling.DODECAGON_HEXAGON_SQUARE, PatternStyle.STRAPWORK, 82.0))
        add(geo("geo-hexagram", "Altı Köşeli Yıldız", "isfahan", Tiling.HEXAGON, PatternStyle.STRAPWORK, 120.0))
        add(geo("geo-gold-lines", "Altın Varak", "onyx", Tiling.DODECAGON_HEXAGON_SQUARE, PatternStyle.LINEWORK, 90.0, 66.0))
        add(geo("geo-lapis-lines", "Lâcivert Çizgiler", "lapis", Tiling.OCTAGON_SQUARE, PatternStyle.LINEWORK, 110.0, 70.0))
        add(geo("geo-marrakesh", "Marakeş", "alhambra", Tiling.DODECAGON_TRIANGLE, PatternStyle.ZELLIGE, 88.0, 75.0))
        add(geo("geo-sand", "Çöl Yıldızı", "sand", Tiling.SQUARE, PatternStyle.ZELLIGE, 200.0, 70.0))

        // Calligraphy
        fun cal(
            phrase: Phrase, palette: String, script: CalligraphyArt.Script, backdrop: Tiling,
            medallion: Boolean = true, suffix: String = "",
        ) = Entry(
            "cal-${phrase.id}$suffix", phrase.transliteration.substringBefore(" ·"), Category.CALLIGRAPHY, pal(palette),
        ) { ctx -> CalligraphyArt.scene(ctx, CalligraphyArt.Params(phrase, script, backdrop, medallion = medallion)) }

        add(cal(Phrases.BISMILLAH, "emerald", CalligraphyArt.Script.NASKH, Tiling.OCTAGON_SQUARE, medallion = false))
        add(cal(Phrases.ALLAH, "onyx", CalligraphyArt.Script.NASKH, Tiling.DODECAGON_HEXAGON_SQUARE))
        add(cal(Phrases.MUHAMMAD, "emerald", CalligraphyArt.Script.NASKH, Tiling.DODECAGON_TRIANGLE))
        add(cal(Phrases.SUBHANALLAH, "lapis", CalligraphyArt.Script.RUQAA, Tiling.OCTAGON_SQUARE))
        add(cal(Phrases.ALHAMDULILLAH, "isfahan", CalligraphyArt.Script.RUQAA, Tiling.HEXAGON))
        add(cal(Phrases.ALLAHU_AKBAR, "alhambra", CalligraphyArt.Script.KUFI, Tiling.SQUARE))
        add(cal(Phrases.TAWHID, "amethyst", CalligraphyArt.Script.NASKH, Tiling.DODECAGON_TRIANGLE, medallion = false))
        add(cal(Phrases.MASHALLAH, "iznik", CalligraphyArt.Script.RUQAA, Tiling.OCTAGON_SQUARE))
        add(cal(Phrases.YUSRA, "sand", CalligraphyArt.Script.NASKH, Tiling.DODECAGON_HEXAGON_SQUARE, medallion = false))
        add(cal(Phrases.HASBUNALLAH, "onyx", CalligraphyArt.Script.NASKH, Tiling.OCTAGON_SQUARE, medallion = false))
        add(cal(Phrases.DHIKR, "lapis", CalligraphyArt.Script.NASKH, Tiling.HEXAGON, medallion = false))
        add(cal(Phrases.MAAKUM, "emerald", CalligraphyArt.Script.NASKH, Tiling.DODECAGON_TRIANGLE, medallion = false))
        add(cal(Phrases.ALLAH, "amethyst", CalligraphyArt.Script.KUFI, Tiling.SQUARE, suffix = "-kufi"))

        // Night mosques
        fun night(id: String, title: String, palette: String, a: NightArt.Architecture, sky: NightArt.Sky, water: Boolean, seed: Int) =
            Entry(id, title, Category.NIGHT, pal(palette), seed) { ctx -> NightArt.scene(ctx, NightArt.Params(a, sky, water)) }

        add(night("night-istanbul", "İstanbul Gecesi", "lapis", NightArt.Architecture.OTTOMAN, NightArt.Sky.NIGHT, true, 3))
        add(night("night-isfahan", "İsfahan'da Akşam", "isfahan", NightArt.Architecture.PERSIAN, NightArt.Sky.DUSK, true, 5))
        add(night("night-agra", "Agra'da Şafak", "amethyst", NightArt.Architecture.MOGHUL, NightArt.Sky.DAWN, true, 8))
        add(night("night-desert", "Çöl Camisi", "sand", NightArt.Architecture.OTTOMAN, NightArt.Sky.DUSK, false, 12))
        add(night("night-emerald", "Zümrüt Gece", "emerald", NightArt.Architecture.MOGHUL, NightArt.Sky.NIGHT, false, 2))
        add(night("night-oled", "Gece Yarısı Minareleri", "onyx", NightArt.Architecture.PERSIAN, NightArt.Sky.NIGHT, true, 4))

        // Ramadan & Eid
        fun lantern(id: String, title: String, palette: String, phrase: Phrase, script: CalligraphyArt.Script, seed: Int) =
            Entry(id, title, Category.RAMADAN, pal(palette), seed) { ctx -> LanternArt.scene(ctx, LanternArt.Params(phrase, script)) }

        add(lantern("ramadan-kareem", "Hayırlı Ramazanlar", "lapis", Phrases.RAMADAN, CalligraphyArt.Script.RUQAA, 1))
        add(lantern("ramadan-amethyst", "Ramazan Fenerleri", "amethyst", Phrases.RAMADAN, CalligraphyArt.Script.NASKH, 6))
        add(lantern("eid-mubarak", "Bayram Tebriği", "emerald", Phrases.EID, CalligraphyArt.Script.RUQAA, 3))
        add(lantern("eid-rose", "Bayram Gülü", "isfahan", Phrases.EID, CalligraphyArt.Script.KUFI, 9))

        // Mihrab
        fun mihrab(id: String, title: String, palette: String, wall: Tiling, style: PatternStyle, niche: Tiling) =
            Entry(id, title, Category.MIHRAB, pal(palette)) { ctx -> MihrabArt.scene(ctx, MihrabArt.Params(wall, style, niche)) }

        add(mihrab("mihrab-cordoba", "Kurtuba", "alhambra", Tiling.OCTAGON_SQUARE, PatternStyle.ZELLIGE, Tiling.DODECAGON_TRIANGLE))
        add(mihrab("mihrab-iznik", "İznik Çinileri", "iznik", Tiling.DODECAGON_HEXAGON_SQUARE, PatternStyle.ZELLIGE, Tiling.OCTAGON_SQUARE))
        add(mihrab("mihrab-emerald", "Zümrüt Mihrap", "emerald", Tiling.SQUARE, PatternStyle.STRAPWORK, Tiling.HEXAGON))
        add(mihrab("mihrab-lapis", "Lâcivert Mihrap", "lapis", Tiling.DODECAGON_TRIANGLE, PatternStyle.STRAPWORK, Tiling.DODECAGON_HEXAGON_SQUARE))

        addAll(ebruEntries())
        addAll(LevhaArt.entries())
    }

    private fun ebru(id: String, title: String, palette: String, style: Ebru.Style, seed: Int) =
        Entry(id, title, Category.EBRU, pal(palette), seed) { ctx -> EbruArt.scene(ctx, EbruArt.Params(style)) }

    private fun ebruEntries() = listOf(
        ebru("ebru-battal-klasik", "Battal Ebru", "emerald", Ebru.Style.BATTAL, 3),
        ebru("ebru-gelgit-deniz", "Gelgit · Deniz", "lapis", Ebru.Style.GELGIT, 5),
        ebru("ebru-sal-iznik", "Şal Ebru · İznik", "iznik", Ebru.Style.SAL, 8),
        ebru("ebru-tarakli-toprak", "Taraklı · Toprak", "sand", Ebru.Style.TARAKLI, 11),
        ebru("ebru-bulbul-gul", "Bülbül Yuvası · Gül", "betul", Ebru.Style.BULBUL_YUVASI, 4),
        ebru("ebru-battal-gece", "Battal · Gece", "onyx", Ebru.Style.BATTAL, 21),
        ebru("ebru-sal-isfahan", "Şal · İsfahan", "isfahan", Ebru.Style.SAL, 13),
        ebru("ebru-gelgit-elhamra", "Gelgit · Elhamra", "alhambra", Ebru.Style.GELGIT, 17),
        ebru("ebru-bulbul-lacivert", "Bülbül Yuvası · Lâcivert", "lapis", Ebru.Style.BULBUL_YUVASI, 9),
        ebru("ebru-tarakli-ametist", "Taraklı · Ametist", "amethyst", Ebru.Style.TARAKLI, 6),
        ebru("ebru-battal-betul", "Betül'ün Ebrusu", "betul", Ebru.Style.BATTAL, 12),
        ebru("ebru-sal-zumrut", "Şal · Zümrüt", "emerald", Ebru.Style.SAL, 15),
    )

    fun byId(id: String) = entries.firstOrNull { it.id == id }
}
