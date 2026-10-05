package com.noor.wallpapers.art

import com.noor.wallpapers.art.Colors.hex

/**
 * A colour scheme every design can be rendered in.
 *
 * [line] is the strapwork / gold-leaf colour, [accentA] and [accentB] fill the
 * stars and the spaces between them, [accentC] is a highlight and [glow] is
 * used for lamplight, windows and the moon.
 */
data class Palette(
    val id: String,
    val name: String,
    val bgTop: Int,
    val bgBottom: Int,
    val line: Int,
    val accentA: Int,
    val accentB: Int,
    val accentC: Int,
    val glow: Int,
) {
    companion object {
        /** Made for Hanife Betül: rose gold on a deep teal night, dusty rose and sage. */
        val BETUL = Palette(
            "betul", "Hanife Betül",
            hex("#123A44"), hex("#041217"), hex("#E8B4A0"),
            hex("#C97B84"), hex("#4F7F72"), hex("#F4C7C3"), hex("#FFE4D6"),
        )

        val ALL = listOf(
            BETUL,
            Palette(
                "emerald", "Zümrüt ve Altın",
                hex("#0D4633"), hex("#02140E"), hex("#D9B54A"),
                hex("#1C7A58"), hex("#0B4F3A"), hex("#F2D27A"), hex("#FFE9A8"),
            ),
            Palette(
                "lapis", "Gece Lâciverdi",
                hex("#14275A"), hex("#040A1E"), hex("#E2C275"),
                hex("#2A4FA8"), hex("#16306E"), hex("#9DBBFF"), hex("#FFE2A0"),
            ),
            Palette(
                "iznik", "İznik Turkuazı",
                hex("#0E5A66"), hex("#04242A"), hex("#F4EEDC"),
                hex("#1C9AA0"), hex("#B83A2E"), hex("#3C7BC4"), hex("#E8FFFB"),
            ),
            Palette(
                "sand", "Çöl Kumu",
                hex("#B98C5F"), hex("#4E3220"), hex("#FFF2D6"),
                hex("#9A4F2B"), hex("#6E5236"), hex("#F6D7A7"), hex("#FFF0C8"),
            ),
            Palette(
                "isfahan", "İsfahan Gülü",
                hex("#431A38"), hex("#140611"), hex("#F0C987"),
                hex("#B0466E"), hex("#2A7F86"), hex("#F7A1B5"), hex("#FFD9C2"),
            ),
            Palette(
                "alhambra", "Elhamra Kiremidi",
                hex("#5E2215"), hex("#200804"), hex("#E9C46A"),
                hex("#B5462A"), hex("#2A6F5F"), hex("#F4A261"), hex("#FFE0B0"),
            ),
            Palette(
                "onyx", "Oniks ve İnci",
                hex("#161616"), hex("#000000"), hex("#E8E2D0"),
                hex("#2A2A2A"), hex("#121212"), hex("#FFFFFF"), hex("#FFF6E0"),
            ),
            Palette(
                "amethyst", "Saray Ametisti",
                hex("#2E1456"), hex("#0B0319"), hex("#E6C76E"),
                hex("#5B2A9A"), hex("#3B1A6E"), hex("#C9A7FF"), hex("#FFE7B0"),
            ),
            /** Evening over the Marmara from Pendik's shore: sea blue, island dusk and ferry-light gold. */
            Palette(
                "marmara", "Marmara Akşamı",
                hex("#1C4E6B"), hex("#071A26"), hex("#F2C9A0"),
                hex("#2F7FA6"), hex("#E07A5F"), hex("#F4D6A0"), hex("#FFE3C2"),
            ),
        )

        /** Built-in palettes by id; ids starting "c-" are her own and carry their colours. */
        fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: PaletteMaker.parse(id) ?: ALL[0]
    }

    val isCustom get() = id.startsWith(PaletteMaker.PREFIX)
}

/**
 * Builds a full palette from the three colours she picks: a background, an
 * ornament (the gold of the strapwork and calligraphy) and an accent. The
 * other tones are derived so every design reads well. The colours live in the
 * id ("c-123A44-E8B4A0-C97B84"), so a custom palette needs no storage to
 * render, and thumbnails cache correctly per colour choice.
 */
object PaletteMaker {
    const val PREFIX = "c-"
    const val NAME = "Senin paletin"

    fun make(background: Int, ornament: Int, accent: Int): Palette {
        val bg = Colors.withAlpha(background, 1f)
        val line = Colors.withAlpha(ornament, 1f)
        val acc = Colors.withAlpha(accent, 1f)
        // Light backgrounds still need a dark foot for the gradient and silhouettes.
        val bottom = Colors.darken(bg, if (Colors.luminance(bg) > 0.45) 0.55f else 0.72f)
        return Palette(
            id = PREFIX + listOf(bg, line, acc).joinToString("-") { hex6(it) },
            name = NAME,
            bgTop = bg,
            bgBottom = bottom,
            line = line,
            accentA = acc,
            accentB = Colors.mix(acc, bg, 0.55f),
            accentC = Colors.lighten(Colors.mix(acc, line, 0.35f), 0.25f),
            glow = Colors.lighten(line, 0.55f),
        )
    }

    fun parse(id: String): Palette? {
        if (!id.startsWith(PREFIX)) return null
        val parts = id.removePrefix(PREFIX).split('-')
        if (parts.size != 3 || parts.any { it.length != 6 }) return null
        val c = parts.map { it.toLongOrNull(16)?.toInt() ?: return null }
        return make(c[0], c[1], c[2])
    }

    /** The three colours a palette was made from (or the nearest for a built-in one), for editing. */
    fun seeds(p: Palette): Triple<Int, Int, Int> = Triple(p.bgTop, p.line, p.accentA)

    /** A pleasing random combination: a deep background, a pale metallic ornament, and an accent across the wheel. */
    fun random(rnd: kotlin.random.Random): Palette {
        val h = rnd.nextDouble() * 360
        val bg = Colors.hsv(h, 0.45 + rnd.nextDouble() * 0.35, 0.18 + rnd.nextDouble() * 0.17)
        val ornamentHue = listOf(42.0, 30.0, 15.0, 50.0, h).random(rnd)
        val ornament = Colors.hsv(ornamentHue, 0.25 + rnd.nextDouble() * 0.35, 0.86 + rnd.nextDouble() * 0.12)
        val accent = Colors.hsv((h + listOf(150.0, 180.0, 210.0, 120.0, 30.0).random(rnd)) % 360, 0.45 + rnd.nextDouble() * 0.3, 0.5 + rnd.nextDouble() * 0.3)
        return make(bg, ornament, accent)
    }

    private fun hex6(c: Int) = (c and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
}
