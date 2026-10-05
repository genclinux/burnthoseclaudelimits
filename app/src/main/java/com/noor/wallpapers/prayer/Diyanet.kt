package com.noor.wallpapers.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** A country, city or district in Diyanet's place lists. */
data class Place(val id: String, val name: String) {
    /** Diyanet writes names in capitals; this is how they read in a sentence. */
    val title: String get() = TurkishText.titleCase(name)
}

/**
 * Diyanet İşleri Başkanlığı's prayer times, through the public ezanvakti
 * service, which republishes Diyanet's official tables (the same numbers as
 * namazvakitleri.diyanet.gov.tr) as JSON:
 *
 *   /ulkeler                  countries      [{UlkeAdi, UlkeAdiEn, UlkeID}]
 *   /sehirler/{UlkeID}        cities         [{SehirAdi, SehirAdiEn, SehirID}]
 *   /ilceler/{SehirID}        districts      [{IlceAdi, IlceAdiEn, IlceID}]
 *   /vakitler/{IlceID}        about 30 days  [{MiladiTarihKisa, Imsak, Gunes, Ogle, Ikindi, Aksam, Yatsi,
 *                                              HicriTarihKisa, KibleSaati, GreenwichOrtalamaZamani, ...}]
 *
 * Parsing is lenient: a day with a missing or malformed field is skipped, not fatal.
 */
object Diyanet {
    const val BASE_URL = "https://ezanvakti.emushaf.net"

    fun countriesUrl() = "$BASE_URL/ulkeler"
    fun citiesUrl(countryId: String) = "$BASE_URL/sehirler/$countryId"
    fun districtsUrl(cityId: String) = "$BASE_URL/ilceler/$cityId"
    fun timesUrl(districtId: String) = "$BASE_URL/vakitler/$districtId"

    fun parseCountries(json: String) = parsePlaces(json, "UlkeAdi", "UlkeID")
    fun parseCities(json: String) = parsePlaces(json, "SehirAdi", "SehirID")
    fun parseDistricts(json: String) = parsePlaces(json, "IlceAdi", "IlceID")

    private fun parsePlaces(json: String, nameKey: String, idKey: String): List<Place> {
        val list = MiniJson.parse(json) as? List<*> ?: throw IllegalArgumentException("Expected a list")
        return list.mapNotNull { o ->
            val m = o as? Map<*, *> ?: return@mapNotNull null
            val id = m.text(idKey) ?: return@mapNotNull null
            val name = m.text(nameKey) ?: m.text(nameKey + "En") ?: return@mapNotNull null
            Place(id, name)
        }
    }

    private val DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /**
     * Parses /vakitler. Each day's UTC offset comes from GreenwichOrtalamaZamani
     * when present, otherwise from [zone] (Europe/Istanbul for Türkiye).
     */
    fun parseTimes(json: String, zone: ZoneId): List<DayTimes> {
        val list = MiniJson.parse(json) as? List<*> ?: throw IllegalArgumentException("Expected a list")
        return list.mapNotNull { o -> (o as? Map<*, *>)?.let { day(it, zone) } }
            .distinctBy { it.date }
            .sortedBy { it.date }
    }

    private fun day(m: Map<*, *>, zone: ZoneId): DayTimes? {
        val date = date(m) ?: return null
        val times = LinkedHashMap<Prayer, LocalTime>()
        for ((p, key) in FIELDS) times[p] = time(m.text(key)) ?: return null
        // İmsak to Akşam must run in order, or the record is garbage. (Yatsı may pass
        // midnight in high-latitude summers; DayTimes handles that.)
        if (times.values.take(5).zipWithNext().any { (a, b) -> !a.isBefore(b) }) return null
        val offset = (m["GreenwichOrtalamaZamani"] as? Double)
            ?.takeIf { it in -14.0..14.0 }
            ?.let { ZoneOffset.ofTotalSeconds((it * 3600).toInt()) }
            ?: zone.rules.getOffset(date.atTime(12, 0))
        return DayTimes(
            date = date,
            times = times,
            offset = offset,
            source = Source.DIYANET,
            hijri = hijri(m.text("HicriTarihKisa")),
            qiblaTime = time(m.text("KibleSaati")),
        )
    }

    private val FIELDS = listOf(
        Prayer.IMSAK to "Imsak", Prayer.GUNES to "Gunes", Prayer.OGLE to "Ogle",
        Prayer.IKINDI to "Ikindi", Prayer.AKSAM to "Aksam", Prayer.YATSI to "Yatsi",
    )

    private fun date(m: Map<*, *>): LocalDate? {
        for (key in listOf("MiladiTarihKisa", "MiladiTarihKisaIso8601")) {
            val s = m.text(key) ?: continue
            runCatching { return LocalDate.parse(s, DMY) }
        }
        // "2026-10-05T00:00:00.0000000+03:00"
        m.text("MiladiTarihUzunIso8601")?.takeIf { it.length >= 10 }?.let { s ->
            runCatching { return LocalDate.parse(s.substring(0, 10)) }
        }
        return null
    }

    /** "05:07", tolerating "5:07" and a trailing ":00". */
    internal fun time(s: String?): LocalTime? {
        val parts = s?.trim()?.split(':') ?: return null
        if (parts.size < 2) return null
        val h = parts[0].trim().toIntOrNull() ?: return null
        val min = parts[1].trim().take(2).toIntOrNull() ?: return null
        if (h !in 0..23 || min !in 0..59) return null
        return LocalTime.of(h, min)
    }

    /** "23.4.1448" (day.month.year) */
    internal fun hijri(s: String?): HijriDate? {
        val p = s?.split('.')?.mapNotNull { it.trim().toIntOrNull() } ?: return null
        if (p.size != 3 || p[1] !in 1..12 || p[0] !in 1..30) return null
        return HijriDate(p[2], p[1], p[0])
    }

    /** Diyanet lists Türkiye under this name; matched rather than trusting a fixed id. */
    fun isTurkey(p: Place) = TurkishText.matchKey(p.name).let { it == "turkiye" || it == "turkey" }
}
