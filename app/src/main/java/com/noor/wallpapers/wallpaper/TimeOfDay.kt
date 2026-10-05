package com.noor.wallpapers.wallpaper

import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.prayer.Prayer

/**
 * A design for each part of the day, from İmsak to Yatsı: dawn over Agra,
 * İznik tiles in the morning, the mihrab at noon, her rosette in the
 * afternoon, dusk in İsfahan and night over İstanbul. The prayer screen uses
 * it as its backdrop; the live wallpaper can follow it through the day.
 */
object TimeOfDay {
    private val BY_PERIOD = mapOf(
        Prayer.IMSAK to "night-agra",
        Prayer.GUNES to "geo-fez",
        Prayer.OGLE to "mihrab-iznik",
        Prayer.IKINDI to "hb-rosette",
        Prayer.AKSAM to "night-isfahan",
        Prayer.YATSI to "night-istanbul",
    )

    /** The design for the period that began at [current] (null before any times are known: night). */
    fun selection(current: Prayer?): Selection {
        val id = BY_PERIOD[current ?: Prayer.YATSI] ?: "hb-stars"
        return Selection.of(Catalog.byId(id) ?: Catalog.entries.first())
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
