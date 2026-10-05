package com.noor.wallpapers.prayer

import java.time.DayOfWeek
import java.time.LocalDate

/** Converts a Gregorian day to the Hijri calendar. The app uses ICU's Umm al-Qura; tests use ICU4J. */
fun interface HijriCalendar {
    fun of(date: LocalDate): HijriDate
}

/**
 * A Hijri calendar moved by whole days. Diyanet's calendar and Umm al-Qura
 * occasionally start a month a day apart, so the app compares them on the days
 * Diyanet has published and shifts to agree.
 */
class ShiftedHijri(private val base: HijriCalendar, val shiftDays: Long) : HijriCalendar {
    override fun of(date: LocalDate): HijriDate = base.of(date.plusDays(shiftDays))

    companion object {
        fun matching(base: HijriCalendar, published: List<DayTimes>): HijriCalendar {
            val known = published.mapNotNull { d -> d.hijri?.let { d.date to it } }
            if (known.isEmpty()) return base
            val best = (-2L..2L).maxBy { s -> known.count { (date, h) -> base.of(date.plusDays(s)) == h } }
            return if (best == 0L) base else ShiftedHijri(base, best)
        }
    }
}

/**
 * Diyanet's religious days. A kandil ([night]) is the night before its Hijri
 * date: it begins at Akşam on the Gregorian day listed, as in Diyanet's calendar.
 */
enum class HolyDay(val title: String, val night: Boolean, val days: Int = 1) {
    UC_AYLAR("Üç Ayların Başlangıcı", false),
    REGAIB("Regaib Kandili", true),
    MIRAC("Miraç Kandili", true),
    BERAT("Berat Kandili", true),
    RAMAZAN("Ramazan Başlangıcı", false),
    KADIR("Kadir Gecesi", true),
    RAMAZAN_AREFE("Ramazan Bayramı Arifesi", false),
    RAMAZAN_BAYRAMI("Ramazan Bayramı", false, days = 3),
    KURBAN_AREFE("Kurban Bayramı Arifesi", false),
    KURBAN_BAYRAMI("Kurban Bayramı", false, days = 4),
    HICRI_YILBASI("Hicri Yılbaşı", false),
    ASURE("Aşure Günü", false),
    MEVLID("Mevlid Kandili", true),
}

data class HolyDayEvent(val day: HolyDay, val date: LocalDate) {
    val lastDate: LocalDate get() = date.plusDays(day.days - 1L)
}

object ReligiousDays {
    /** Every religious day from [from] to [to] inclusive, in date order. */
    fun between(from: LocalDate, to: LocalDate, cal: HijriCalendar): List<HolyDayEvent> {
        val out = ArrayList<HolyDayEvent>()
        var d = from
        val end = to.plusDays(1)
        while (!d.isAfter(end)) {
            val h = cal.of(d)
            val eve = d.minusDays(1)
            when {
                h.month == 7 && h.day == 1 -> out += HolyDayEvent(HolyDay.UC_AYLAR, d)
                h.month == 7 && h.day == 27 -> out += HolyDayEvent(HolyDay.MIRAC, eve)
                h.month == 8 && h.day == 15 -> out += HolyDayEvent(HolyDay.BERAT, eve)
                h.month == 9 && h.day == 1 -> out += HolyDayEvent(HolyDay.RAMAZAN, d)
                h.month == 9 && h.day == 27 -> out += HolyDayEvent(HolyDay.KADIR, eve)
                h.month == 10 && h.day == 1 -> {
                    out += HolyDayEvent(HolyDay.RAMAZAN_AREFE, eve)
                    out += HolyDayEvent(HolyDay.RAMAZAN_BAYRAMI, d)
                }
                h.month == 12 && h.day == 9 -> out += HolyDayEvent(HolyDay.KURBAN_AREFE, d)
                h.month == 12 && h.day == 10 -> out += HolyDayEvent(HolyDay.KURBAN_BAYRAMI, d)
                h.month == 1 && h.day == 1 -> out += HolyDayEvent(HolyDay.HICRI_YILBASI, d)
                h.month == 1 && h.day == 10 -> out += HolyDayEvent(HolyDay.ASURE, d)
                h.month == 3 && h.day == 12 -> out += HolyDayEvent(HolyDay.MEVLID, eve)
            }
            // Regaib: the night before the first Friday of Recep.
            if (h.month == 7 && h.day <= 7 && d.dayOfWeek == DayOfWeek.FRIDAY) out += HolyDayEvent(HolyDay.REGAIB, eve)
            d = d.plusDays(1)
        }
        return out.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .sortedWith(compareBy<HolyDayEvent> { it.date }.thenBy { it.day.ordinal })
    }

    /** The religious days that fall on [date] (a Bayram counts on each of its days). */
    fun on(date: LocalDate, cal: HijriCalendar): List<HolyDayEvent> =
        between(date.minusDays(4), date, cal).filter { !date.isBefore(it.date) && !date.isAfter(it.lastDate) }
}
