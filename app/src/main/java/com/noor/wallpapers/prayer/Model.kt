package com.noor.wallpapers.prayer

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/*
 * Prayer times, as Diyanet publishes them: six times a day, in local time.
 * Everything in this package is plain Kotlin (java.time only), so the same code
 * runs in the app, the live wallpaper, the widget and the desktop JVM tests.
 */

enum class Prayer(val title: String) {
    IMSAK("İmsak"),
    GUNES("Güneş"),
    OGLE("Öğle"),
    IKINDI("İkindi"),
    AKSAM("Akşam"),
    YATSI("Yatsı");

    /** The five daily prayers; Güneş (sunrise) marks the end of the morning prayer, not a prayer of its own. */
    val isPrayer get() = this != GUNES
}

/** A Hijri date; [month] is 1 (Muharrem) to 12 (Zilhicce). */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName get() = TurkishText.HIJRI_MONTHS[(month - 1).coerceIn(0, 11)]
    override fun toString() = "$day $monthName $year"

    companion object {
        const val RAMAZAN = 9
    }
}

enum class Source { DIYANET, CALCULATED }

/** One day of times at one place. [offset] is that place's UTC offset on that day. */
data class DayTimes(
    val date: LocalDate,
    val times: Map<Prayer, LocalTime>,
    val offset: ZoneOffset,
    val source: Source,
    /** Diyanet's own Hijri date for the day, when it came from Diyanet. */
    val hijri: HijriDate? = null,
    /** "Kıble saati": when the sun stands in the direction of the Kaaba. */
    val qiblaTime: LocalTime? = null,
) {
    fun time(p: Prayer): LocalTime = times.getValue(p)

    fun instant(p: Prayer): Instant {
        // Far north in summer Yatsı can fall after midnight, on the next calendar day.
        val nextDay = p == Prayer.YATSI && time(p).isBefore(time(Prayer.AKSAM))
        return (if (nextDay) date.plusDays(1) else date).atTime(time(p)).toInstant(offset)
    }
}

data class PrayerEvent(val prayer: Prayer, val day: DayTimes) {
    val instant: Instant get() = day.instant(prayer)
    val time: LocalTime get() = day.time(prayer)
}

/** Where the times are for. Ids are Diyanet's; a place picked offline has none and is calculated. */
data class PrayerLocation(
    val countryId: String?,
    val country: String,
    val cityId: String?,
    val city: String,
    val districtId: String?,
    val district: String,
    val latitude: Double?,
    val longitude: Double?,
    /** IANA zone when known (Europe/Istanbul for Türkiye); otherwise taken from Diyanet's GMT offset. */
    val zoneId: String? = null,
) {
    val hasDiyanet get() = districtId != null
    val hasCoordinates get() = latitude != null && longitude != null

    /** "Kadıköy, İstanbul", or just "İstanbul" for a province centre. */
    val label: String
        get() = if (district.isBlank() || TurkishText.matchKey(district) == TurkishText.matchKey(city)) city
        else "$district, $city"
}

/**
 * The times for a place: Diyanet's published days where we have them, and a
 * calculation (calibrated against Diyanet where possible) for any other day.
 */
class PrayerSchedule(
    val location: PrayerLocation,
    diyanetDays: List<DayTimes>,
    private val calculator: ((LocalDate) -> DayTimes?)?,
    val zone: ZoneId,
) {
    private val diyanet: Map<LocalDate, DayTimes> = diyanetDays.associateBy { it.date }

    /** Diyanet's published days, in date order. */
    val published: List<DayTimes> = diyanet.values.sortedBy { it.date }

    /** The last day Diyanet's data covers, or null if we have none. */
    val diyanetUntil: LocalDate? = diyanet.keys.maxOrNull()
    val diyanetDays: Int get() = diyanet.size

    fun day(date: LocalDate): DayTimes? = diyanet[date] ?: calculator?.invoke(date)

    fun today(now: Instant): LocalDate = now.atZone(zone).toLocalDate()

    /** Every time from the day before [now] through [daysAhead] days after, in order. */
    fun events(now: Instant, daysAhead: Int = 2): List<PrayerEvent> {
        val today = today(now)
        return (-1..daysAhead).flatMap { k ->
            val d = day(today.plusDays(k.toLong())) ?: return@flatMap emptyList()
            Prayer.entries.map { PrayerEvent(it, d) }
        }.sortedBy { it.instant }
    }

    fun next(now: Instant): PrayerEvent? = events(now).firstOrNull { it.instant.isAfter(now) }

    /** The time whose period we are in (the latest one at or before [now]). */
    fun current(now: Instant): PrayerEvent? = events(now).lastOrNull { !it.instant.isAfter(now) }

    fun untilNext(now: Instant): Duration? = next(now)?.let { Duration.between(now, it.instant) }
}
