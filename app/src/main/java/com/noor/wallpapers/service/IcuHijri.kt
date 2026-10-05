package com.noor.wallpapers.service

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import com.noor.wallpapers.prayer.HijriCalendar
import com.noor.wallpapers.prayer.HijriDate
import java.time.LocalDate
import java.time.ZoneOffset

/** The Umm al-Qura Hijri calendar from Android's ICU. */
object IcuHijri : HijriCalendar {
    override fun of(date: LocalDate): HijriDate {
        val cal = IslamicCalendar(TimeZone.getTimeZone("UTC"))
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        // Noon UTC, well inside the day whatever the calendar's day boundary.
        cal.timeInMillis = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000 + 12 * 3600_000L
        return HijriDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
}
