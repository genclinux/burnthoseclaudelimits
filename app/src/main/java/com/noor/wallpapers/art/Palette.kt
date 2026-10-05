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
        val ALL = listOf(
            Palette(
                "emerald", "Emerald & Gold",
                hex("#0D4633"), hex("#02140E"), hex("#D9B54A"),
                hex("#1C7A58"), hex("#0B4F3A"), hex("#F2D27A"), hex("#FFE9A8"),
            ),
            Palette(
                "lapis", "Midnight Lapis",
                hex("#14275A"), hex("#040A1E"), hex("#E2C275"),
                hex("#2A4FA8"), hex("#16306E"), hex("#9DBBFF"), hex("#FFE2A0"),
            ),
            Palette(
                "iznik", "Iznik Turquoise",
                hex("#0E5A66"), hex("#04242A"), hex("#F4EEDC"),
                hex("#1C9AA0"), hex("#B83A2E"), hex("#3C7BC4"), hex("#E8FFFB"),
            ),
            Palette(
                "sand", "Desert Sand",
                hex("#B98C5F"), hex("#4E3220"), hex("#FFF2D6"),
                hex("#9A4F2B"), hex("#6E5236"), hex("#F6D7A7"), hex("#FFF0C8"),
            ),
            Palette(
                "isfahan", "Isfahan Rose",
                hex("#431A38"), hex("#140611"), hex("#F0C987"),
                hex("#B0466E"), hex("#2A7F86"), hex("#F7A1B5"), hex("#FFD9C2"),
            ),
            Palette(
                "alhambra", "Alhambra Terracotta",
                hex("#5E2215"), hex("#200804"), hex("#E9C46A"),
                hex("#B5462A"), hex("#2A6F5F"), hex("#F4A261"), hex("#FFE0B0"),
            ),
            Palette(
                "onyx", "Onyx & Pearl",
                hex("#161616"), hex("#000000"), hex("#E8E2D0"),
                hex("#2A2A2A"), hex("#121212"), hex("#FFFFFF"), hex("#FFF6E0"),
            ),
            Palette(
                "amethyst", "Royal Amethyst",
                hex("#2E1456"), hex("#0B0319"), hex("#E6C76E"),
                hex("#5B2A9A"), hex("#3B1A6E"), hex("#C9A7FF"), hex("#FFE7B0"),
            ),
        )

        fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}
