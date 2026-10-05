package com.noor.wallpapers.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Prayer times worked out from the sun's position, with Diyanet's method:
 * İmsak when the sun is 18° below the horizon, Yatsı at 17°, İkindi when a
 * shadow equals its object plus its noon shadow, and Diyanet's temkin
 * (safety margins): Güneş −7, Öğle +5, İkindi +4, Akşam +7 minutes.
 *
 * Diyanet's published tables are always preferred. This covers the gaps: no
 * internet yet, a place Diyanet doesn't list, or days past the cached month.
 * Diyanet also allows for each city's extent, so a point calculation can be a
 * minute or two off; [Calibration] measures and removes that difference once
 * Diyanet's own numbers for the place have been seen.
 *
 * The solar model is the one from PrayTimes.org (accurate to well under a minute
 * at Turkish latitudes), iterated twice.
 */
class PrayerCalculator(
    private val latitude: Double,
    private val longitude: Double,
    private val zone: ZoneId,
    private val calibration: Calibration = Calibration.NONE,
) {
    fun day(date: LocalDate): DayTimes? {
        val offset = zone.rules.getOffset(date.atTime(12, 0))
        val tz = offset.totalSeconds / 3600.0
        val raw = rawHours(date, tz) ?: return null
        val times = LinkedHashMap<Prayer, LocalTime>()
        for (p in Prayer.entries) {
            val minutes = raw.getValue(p) * 60 + TEMKIN.getValue(p) + calibration.minutes(p)
            times[p] = toLocalTime(minutes)
        }
        return DayTimes(date, times, offset, Source.CALCULATED)
    }

    /** Local clock hours (before temkin), or null where the sun never rises or sets. */
    internal fun rawHours(date: LocalDate, timeZone: Double): Map<Prayer, Double>? {
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - longitude / (15 * 24)
        // Initial guesses in hours; each pass refines them with the sun's position at that hour.
        var t = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0)
        repeat(2) {
            val p = t.map { it / 24 }
            t = doubleArrayOf(
                sunAngleTime(jDate, IMSAK_ANGLE, p[0], ccw = true),
                sunAngleTime(jDate, RISE_SET_ANGLE, p[1], ccw = true),
                midDay(jDate, p[2]),
                asrTime(jDate, 1.0, p[3]),
                sunAngleTime(jDate, RISE_SET_ANGLE, p[4], ccw = false),
                sunAngleTime(jDate, YATSI_ANGLE, p[5], ccw = false),
            )
        }
        if (t[1].isNaN() || t[4].isNaN() || t[3].isNaN()) return null
        // Where twilight never gets that deep (far north in summer), use the angle-based night portion.
        val night = 24 - (t[4] - t[1])
        if (t[0].isNaN()) t[0] = t[1] - IMSAK_ANGLE / 60 * night
        if (t[5].isNaN()) t[5] = t[4] + YATSI_ANGLE / 60 * night
        val shift = timeZone - longitude / 15
        return mapOf(
            Prayer.IMSAK to t[0] + shift,
            Prayer.GUNES to t[1] + shift,
            Prayer.OGLE to t[2] + shift,
            Prayer.IKINDI to t[3] + shift,
            Prayer.AKSAM to t[4] + shift,
            Prayer.YATSI to t[5] + shift,
        )
    }

    private fun midDay(jDate: Double, time: Double): Double = fixHour(12 - sun(jDate + time).equation)

    private fun sunAngleTime(jDate: Double, angle: Double, time: Double, ccw: Boolean): Double {
        val decl = sun(jDate + time).declination
        val noon = midDay(jDate, time)
        val cosT = (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
        if (cosT < -1 || cosT > 1) return Double.NaN
        val t = darccos(cosT) / 15
        return noon + if (ccw) -t else t
    }

    private fun asrTime(jDate: Double, factor: Double, time: Double): Double {
        val decl = sun(jDate + time).declination
        val angle = -darccot(factor + dtan(abs(latitude - decl)))
        return sunAngleTime(jDate, angle, time, ccw = false)
    }

    private class Sun(val declination: Double, val equation: Double)

    private fun sun(jd: Double): Sun {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
        val eqt = q / 15 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return Sun(decl, eqt)
    }

    companion object {
        const val IMSAK_ANGLE = 18.0
        const val YATSI_ANGLE = 17.0
        private const val RISE_SET_ANGLE = 0.833

        /** Diyanet's temkin, in minutes. */
        val TEMKIN = mapOf(
            Prayer.IMSAK to 0, Prayer.GUNES to -7, Prayer.OGLE to 5,
            Prayer.IKINDI to 4, Prayer.AKSAM to 7, Prayer.YATSI to 0,
        )

        fun julian(year: Int, month: Int, day: Int): Double {
            var y = year
            var m = month
            if (m <= 2) { y -= 1; m += 12 }
            val a = floor(y / 100.0)
            val b = 2 - a + floor(a / 4)
            return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
        }

        private fun toLocalTime(minutes: Double): LocalTime {
            val m = Math.floorMod(minutes.roundToInt(), 24 * 60)
            return LocalTime.of(m / 60, m % 60)
        }

        private fun dsin(d: Double) = sin(Math.toRadians(d))
        private fun dcos(d: Double) = cos(Math.toRadians(d))
        private fun dtan(d: Double) = tan(Math.toRadians(d))
        private fun darcsin(x: Double) = Math.toDegrees(asin(x))
        private fun darccos(x: Double) = Math.toDegrees(acos(x))
        private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
        private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
        private fun fixAngle(a: Double) = a - 360 * floor(a / 360)
        private fun fixHour(a: Double) = a - 24 * floor(a / 24)
    }
}

/**
 * Per-time corrections, in minutes, that make the calculation agree with
 * Diyanet's published table for this place.
 */
class Calibration(private val offsets: Map<Prayer, Int>) {
    fun minutes(p: Prayer): Int = offsets[p] ?: 0
    val isEmpty get() = offsets.values.all { it == 0 }
    override fun toString() = Prayer.entries.joinToString(",") { "${it.name}=${minutes(it)}" }

    companion object {
        val NONE = Calibration(emptyMap())

        /** Larger gaps mean the calculation is for the wrong place, not a matter of margins. */
        private const val LIMIT = 30

        /**
         * The median difference between Diyanet's [published] days and an
         * uncalibrated [calculator] for the same place.
         */
        fun measure(published: List<DayTimes>, calculator: PrayerCalculator): Calibration {
            val diffs = Prayer.entries.associateWith { ArrayList<Int>() }
            for (d in published) {
                val c = calculator.day(d.date) ?: continue
                for (p in Prayer.entries) {
                    var diff = d.time(p).toSecondOfDay() / 60 - c.time(p).toSecondOfDay() / 60
                    if (diff > 12 * 60) diff -= 24 * 60
                    if (diff < -12 * 60) diff += 24 * 60
                    diffs.getValue(p) += diff
                }
            }
            val out = diffs.mapValues { (_, v) ->
                if (v.isEmpty()) 0 else v.sorted()[v.size / 2].takeIf { abs(it) <= LIMIT } ?: 0
            }
            return Calibration(out)
        }

        fun parse(s: String?): Calibration {
            if (s.isNullOrBlank()) return NONE
            val map = s.split(',').mapNotNull { part ->
                val (k, v) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                val p = Prayer.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
                p to (v.toIntOrNull() ?: return@mapNotNull null)
            }.toMap()
            return Calibration(map)
        }
    }
}
