package com.noor.wallpapers.prayer

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.IslamicCalendar
import com.ibm.icu.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The same Umm al-Qura calendar the app gets from android.icu, via ICU4J. */
private object UmmAlQura : HijriCalendar {
    override fun of(date: LocalDate): HijriDate {
        val cal = IslamicCalendar(TimeZone.GMT_ZONE)
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis = date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 + 12 * 3600_000L
        return HijriDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
}

class ReligiousDaysTest {
    @Test
    fun matchesDiyanets2025Calendar() {
        // Diyanet İşleri Başkanlığı, Dini Günler 2025.
        val expected = listOf(
            HolyDay.UC_AYLAR to "2025-01-01",
            HolyDay.REGAIB to "2025-01-02",
            HolyDay.MIRAC to "2025-01-26",
            HolyDay.BERAT to "2025-02-13",
            HolyDay.RAMAZAN to "2025-03-01",
            HolyDay.KADIR to "2025-03-26",
            HolyDay.RAMAZAN_AREFE to "2025-03-29",
            HolyDay.RAMAZAN_BAYRAMI to "2025-03-30",
            HolyDay.KURBAN_AREFE to "2025-06-05",
            HolyDay.KURBAN_BAYRAMI to "2025-06-06",
            HolyDay.HICRI_YILBASI to "2025-06-26",
            HolyDay.ASURE to "2025-07-05",
            HolyDay.MEVLID to "2025-09-03",
        ).map { (d, s) -> HolyDayEvent(d, LocalDate.parse(s)) }
        val got = ReligiousDays.between(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 1), UmmAlQura)
        assertEquals(expected, got)
    }

    @Test
    fun bayramCountsOnEachOfItsDays() {
        val second = ReligiousDays.on(LocalDate.of(2025, 6, 8), UmmAlQura)
        assertEquals(listOf(HolyDay.KURBAN_BAYRAMI), second.map { it.day })
        assertEquals(emptyList<HolyDayEvent>(), ReligiousDays.on(LocalDate.of(2025, 6, 10), UmmAlQura))
    }

    @Test
    fun shiftsToAgreeWithDiyanet() {
        val calc = PrayerCalculator(41.0, 29.0, java.time.ZoneId.of("Europe/Istanbul"))
        val date = LocalDate.of(2025, 9, 4)
        val umm = UmmAlQura.of(date)
        // A table that is one day ahead of Umm al-Qura.
        val published = listOf(calc.day(date)!!.copy(source = Source.DIYANET, hijri = UmmAlQura.of(date.plusDays(1))))
        val shifted = ShiftedHijri.matching(UmmAlQura, published)
        assertEquals(UmmAlQura.of(date.plusDays(1)), shifted.of(date))
        assertEquals(umm, ShiftedHijri.matching(UmmAlQura, emptyList()).of(date))
    }
}
