package com.noor.wallpapers

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.time.temporal.ChronoUnit

/** Little facts about her days so far, for the "Doğduğun gün" surprise. Plain Kotlin, so it can be tested. */
data class BirthFacts(
    /** Today is her [dayNumber]th day in the world (the day she was born is the 1st). */
    val dayNumber: Long,
    val years: Int,
    val fridays: Long,
    /** 0 on her birthday. */
    val daysToBirthday: Long,
) {
    companion object {
        fun of(birth: LocalDate, today: LocalDate): BirthFacts {
            val days = ChronoUnit.DAYS.between(birth, today)
            // Fridays from her birthday up to and including today.
            val firstFriday = birth.plusDays(((DayOfWeek.FRIDAY.value - birth.dayOfWeek.value + 7) % 7).toLong())
            val fridays = if (firstFriday.isAfter(today)) 0 else ChronoUnit.DAYS.between(firstFriday, today) / 7 + 1
            val md = MonthDay.from(birth)
            // 29 February falls back to the 28th in other years.
            var next = md.atYear(today.year)
            if (next.isBefore(today)) next = md.atYear(today.year + 1)
            return BirthFacts(
                dayNumber = days + 1,
                years = Period.between(birth, today).years,
                fridays = fridays,
                daysToBirthday = ChronoUnit.DAYS.between(today, next),
            )
        }

        /** Ramadans she has lived through, given Hijri (year, month) of her birth and of today. */
        fun ramadans(birthYear: Int, birthMonth: Int, todayYear: Int, todayMonth: Int): Int =
            (todayYear - birthYear) + (if (todayMonth >= 9) 1 else 0) - (if (birthMonth > 9) 1 else 0)
    }
}
