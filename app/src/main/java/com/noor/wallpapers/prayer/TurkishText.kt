package com.noor.wallpapers.prayer

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Turkish names, casing and number formats used across the prayer features. */
object TurkishText {
    val TR: Locale = Locale.forLanguageTag("tr")

    /** Diyanet's spelling of the Hijri months. */
    val HIJRI_MONTHS = listOf(
        "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir",
        "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce",
    )

    val MONTHS = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )

    fun weekday(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "Pazartesi"
        DayOfWeek.TUESDAY -> "Salı"
        DayOfWeek.WEDNESDAY -> "Çarşamba"
        DayOfWeek.THURSDAY -> "Perşembe"
        DayOfWeek.FRIDAY -> "Cuma"
        DayOfWeek.SATURDAY -> "Cumartesi"
        DayOfWeek.SUNDAY -> "Pazar"
    }

    /** "5 Ekim 2026 Pazartesi" */
    fun longDate(d: LocalDate) = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year} ${weekday(d.dayOfWeek)}"

    /** "5 Ekim" */
    fun dayMonth(d: LocalDate) = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"

    /** "05:07" */
    fun hhmm(t: LocalTime) = String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)

    /**
     * "1 sa 23 dk", "23 dk", "1 dk": for captions, notifications and the wallpaper.
     * Rounds up, like a clock app: 22 min 10 s to go reads "23 dk".
     */
    fun countdown(d: Duration): String {
        val minutes = (d.seconds.coerceAtLeast(0) + 59) / 60
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "$h sa $m dk"
            h > 0 -> "$h sa"
            else -> "$m dk"
        }
    }

    /** "1:23:45" or "23:45", for a ticking display. */
    fun clock(d: Duration): String {
        val s = d.seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%02d:%02d", m, sec)
    }

    /**
     * "13:12'de", "13:10'da", "12:00'de", "18:40'ta": the time with the locative
     * suffix that matches how it is read aloud.
     */
    fun at(t: LocalTime): String {
        val n = if (t.minute != 0) t.minute else t.hour
        val word = when {
            n % 10 != 0 -> UNITS[n % 10]
            n == 0 -> "sıfır"
            else -> TENS[n / 10 % 10]
        }
        val back = word.last { it in "aeıioöuü" } in "aıou"
        val hard = word.last() in "çfhkpsşt"
        val suffix = (if (hard) "t" else "d") + (if (back) "a" else "e")
        return "${hhmm(t)}'$suffix"
    }

    private val UNITS = listOf("", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
    private val TENS = listOf("", "on", "yirmi", "otuz", "kırk", "elli")

    /** "bugün", "yarın", "12 gün sonra". */
    fun daysFromNow(days: Long): String = when (days) {
        0L -> "bugün"
        1L -> "yarın"
        else -> "$days gün sonra"
    }

    /**
     * A key for comparing place names however they are written: Diyanet sends
     * "ŞANLIURFA", a geocoder "Şanlıurfa" or "Sanliurfa".
     */
    fun matchKey(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            val c = when (ch) {
                'İ', 'I', 'ı', 'i', 'Î', 'î' -> 'i'
                'Ş', 'ş' -> 's'
                'Ğ', 'ğ' -> 'g'
                'Ü', 'ü', 'Û', 'û' -> 'u'
                'Ö', 'ö' -> 'o'
                'Ç', 'ç' -> 'c'
                'Â', 'â' -> 'a'
                else -> ch.lowercaseChar()
            }
            if (c in 'a'..'z' || c in '0'..'9') sb.append(c)
        }
        return sb.toString()
    }

    /** "KADIKÖY" -> "Kadıköy", "AFYONKARAHİSAR" -> "Afyonkarahisar"; abbreviations like "A.B.D." stay as they are. */
    fun titleCase(s: String): String = s.trim().split(Regex("\\s+")).joinToString(" ") { word ->
        if ('.' in word) return@joinToString word
        val lower = word.lowercase(TR)
        val first = lower.indexOfFirst { it.isLetter() }
        if (first < 0) lower else lower.substring(0, first) + lower.substring(first, first + 1).uppercase(TR) + lower.substring(first + 1)
    }
}
