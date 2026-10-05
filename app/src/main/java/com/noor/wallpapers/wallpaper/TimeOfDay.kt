package com.noor.wallpapers.wallpaper

import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.prayer.Prayer

/**
 * A design for each part of the day, from İmsak to Yatsı: an İstanbul dawn in
 * watercolour, İznik tiles through the morning and noon, her rosette in the
 * afternoon, the Golden Horn at sunset and a crescent night, and on a kandil
 * evening the mosque with its minarets lit. The prayer screen uses it as its
 * backdrop; the live wallpaper can follow it through the day.
 */
object TimeOfDay {
    private val BY_PERIOD = mapOf(
        Prayer.IMSAK to "suluboya-safak",
        Prayer.GUNES to "cini-vazo",
        Prayer.OGLE to "cini-lale",
        Prayer.IKINDI to "hb-rosette",
        Prayer.AKSAM to "suluboya-gun-batimi",
        Prayer.YATSI to "suluboya-hilal",
    )
    private const val KANDIL = "suluboya-kandil"

    /**
     * The design for the period that began at [current] (null before any times
     * are known: night). On a [kandil] night the evening is lit for it.
     */
    fun selection(current: Prayer?, kandil: Boolean = false): Selection {
        val p = current ?: Prayer.YATSI
        val id = if (kandil && (p == Prayer.AKSAM || p == Prayer.YATSI)) KANDIL else BY_PERIOD[p]
        return Selection.of(Catalog.byId(id ?: "") ?: Catalog.byId("hb-stars") ?: Catalog.entries.first())
    }

    fun title(current: Prayer?): String = when (current) {
        Prayer.IMSAK -> "Seher"
        Prayer.GUNES -> "Kuşluk"
        Prayer.OGLE -> "Öğle"
        Prayer.IKINDI -> "İkindi"
        Prayer.AKSAM -> "Akşam"
        Prayer.YATSI, null -> "Gece"
    }
}
