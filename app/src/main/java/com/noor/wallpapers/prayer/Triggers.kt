package com.noor.wallpapers.prayer

import java.time.Instant

/**
 * When the app has to wake up: at the start of every time (the widget and
 * wallpaper move on, and enabled times notify) and, optionally, a few minutes
 * before the enabled ones.
 */
object Triggers {
    enum class Kind { REMINDER, ENTRY }

    data class Trigger(val kind: Kind, val event: PrayerEvent, val at: Instant)

    private fun all(schedule: PrayerSchedule, around: Instant, reminderMinutes: Int, remind: Set<Prayer>): List<Trigger> =
        schedule.events(around, daysAhead = 2).flatMap { e ->
            val entry = Trigger(Kind.ENTRY, e, e.instant)
            if (reminderMinutes > 0 && e.prayer in remind) {
                listOf(Trigger(Kind.REMINDER, e, e.instant.minusSeconds(reminderMinutes * 60L)), entry)
            } else {
                listOf(entry)
            }
        }.sortedWith(compareBy<Trigger> { it.at }.thenBy { it.kind.ordinal })

    /** The first trigger strictly after [now]. */
    fun next(schedule: PrayerSchedule, now: Instant, reminderMinutes: Int, remind: Set<Prayer>): Trigger? =
        all(schedule, now, reminderMinutes, remind).firstOrNull { it.at.isAfter(now) }

    /**
     * Triggers in (after, until]: everything due since the last alarm, so two
     * that share a minute are both handled by one wake-up.
     */
    fun due(schedule: PrayerSchedule, after: Instant, until: Instant, reminderMinutes: Int, remind: Set<Prayer>): List<Trigger> =
        all(schedule, until, reminderMinutes, remind).filter { it.at.isAfter(after) && !it.at.isAfter(until) }
}
