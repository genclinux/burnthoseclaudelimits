package com.noor.wallpapers.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class PrayerCalcTest {
    private val tz = ZoneId.of("Europe/Istanbul")

    /**
     * Reference: NOAA solar algorithms (Python `astral` 3.2), minutes after local
     * midnight for 18° dawn, sunrise, noon, Shafi asr, sunset and 17° dusk.
     */
    private val reference = listOf(
        Triple("istanbul", LocalDate.of(2026, 1, 15), "409.63 507.13 793.30 941.43 1080.08 1172.18"),
        Triple("istanbul", LocalDate.of(2026, 6, 21), "203.97 332.28 785.80 1026.92 1239.48 1358.38"),
        Triple("istanbul", LocalDate.of(2026, 10, 5), "333.15 424.87 772.65 966.85 1119.53 1205.70"),
        Triple("ankara", LocalDate.of(2026, 1, 15), "392.83 488.68 777.78 928.87 1067.48 1158.00"),
        Triple("ankara", LocalDate.of(2026, 6, 21), "196.77 320.33 770.27 1009.07 1220.38 1335.10"),
        Triple("ankara", LocalDate.of(2026, 10, 5), "318.55 408.78 757.12 952.33 1104.60 1189.38"),
        Triple("van", LocalDate.of(2026, 1, 15), "349.08 442.87 735.73 890.58 1029.15 1117.70"),
        Triple("van", LocalDate.of(2026, 6, 21), "164.77 282.83 728.22 963.88 1173.75 1283.60"),
        Triple("van", LocalDate.of(2026, 10, 5), "277.63 365.98 715.07 911.53 1063.32 1146.37"),
        Triple("izmir", LocalDate.of(2026, 1, 15), "413.90 507.57 800.65 955.73 1094.32 1182.77"),
        Triple("izmir", LocalDate.of(2026, 6, 21), "230.20 348.02 793.13 1028.63 1238.45 1348.05"),
        Triple("izmir", LocalDate.of(2026, 10, 5), "342.65 430.92 779.98 976.47 1128.22 1211.17"),
    )

    private val cities = mapOf(
        "istanbul" to (41.0082 to 28.9784),
        "ankara" to (39.9334 to 32.8597),
        "van" to (38.5012 to 43.3730),
        "izmir" to (38.4237 to 27.1428),
    )

    @Test
    fun solarTimesMatchNoaaWithinAMinute() {
        for ((city, date, expected) in reference) {
            val (lat, lon) = cities.getValue(city)
            val raw = PrayerCalculator(lat, lon, tz).rawHours(date, 3.0)!!
            val want = expected.split(' ').map { it.toDouble() }
            Prayer.entries.forEachIndexed { i, p ->
                // Dawn/dusk differ a little more between models (refraction at depth), so allow 1.5 min there.
                val tol = if (p == Prayer.IMSAK || p == Prayer.YATSI) 1.5 else 1.0
                assertEquals("$city $date $p", want[i], raw.getValue(p) * 60, tol)
            }
        }
    }

    @Test
    fun appliesDiyanetTemkin() {
        val calc = PrayerCalculator(41.0082, 28.9784, tz)
        val date = LocalDate.of(2026, 10, 5)
        val raw = calc.rawHours(date, 3.0)!!
        val day = calc.day(date)!!
        fun minutes(t: LocalTime) = t.toSecondOfDay() / 60.0
        assertEquals(raw.getValue(Prayer.GUNES) * 60 - 7, minutes(day.time(Prayer.GUNES)), 0.5)
        assertEquals(raw.getValue(Prayer.OGLE) * 60 + 5, minutes(day.time(Prayer.OGLE)), 0.5)
        assertEquals(raw.getValue(Prayer.IKINDI) * 60 + 4, minutes(day.time(Prayer.IKINDI)), 0.5)
        assertEquals(raw.getValue(Prayer.AKSAM) * 60 + 7, minutes(day.time(Prayer.AKSAM)), 0.5)
        assertEquals(ZoneOffset.ofHours(3), day.offset)
        assertEquals(Source.CALCULATED, day.source)
    }

    @Test
    fun calibrationLearnsDiyanetsOffsets() {
        val calc = PrayerCalculator(41.0082, 28.9784, tz)
        // Pretend Diyanet's table runs 2 minutes later at Öğle and 1 earlier at İmsak.
        val published = (0 until 10).map { k ->
            val d = calc.day(LocalDate.of(2026, 10, 5).plusDays(k.toLong()))!!
            d.copy(
                source = Source.DIYANET,
                times = d.times.mapValues { (p, t) ->
                    when (p) { Prayer.OGLE -> t.plusMinutes(2); Prayer.IMSAK -> t.minusMinutes(1); else -> t }
                },
            )
        }
        val cal = Calibration.measure(published, calc)
        assertEquals(2, cal.minutes(Prayer.OGLE))
        assertEquals(-1, cal.minutes(Prayer.IMSAK))
        assertEquals(0, cal.minutes(Prayer.AKSAM))
        val again = Calibration.parse(cal.toString())
        Prayer.entries.forEach { assertEquals(cal.minutes(it), again.minutes(it)) }
        val calibrated = PrayerCalculator(41.0082, 28.9784, tz, cal).day(LocalDate.of(2026, 11, 1))!!
        val plain = calc.day(LocalDate.of(2026, 11, 1))!!
        assertEquals(plain.time(Prayer.OGLE).plusMinutes(2), calibrated.time(Prayer.OGLE))
    }

    @Test
    fun highLatitudeSummerStillHasEveryTime() {
        // Oslo at midsummer: the sun never gets 17-18° below the horizon.
        val day = PrayerCalculator(59.91, 10.75, ZoneId.of("Europe/Oslo")).day(LocalDate.of(2026, 6, 21))
        assertNotNull(day)
        assertTrue(day!!.instant(Prayer.IMSAK).isBefore(day.instant(Prayer.GUNES)))
        assertTrue(day.instant(Prayer.AKSAM).isBefore(day.instant(Prayer.YATSI)))
    }

    @Test
    fun polarDayHasNoTimes() {
        assertEquals(null, PrayerCalculator(78.22, 15.65, ZoneId.of("Arctic/Longyearbyen")).day(LocalDate.of(2026, 6, 21)))
    }
}
