package com.noor.wallpapers.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val location = Provinces.location(Provinces.byName("İstanbul")!!)
    private val calc = PrayerCalculator(location.latitude!!, location.longitude!!, zone)

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant()

    @Test
    fun usesDiyanetWhereAvailableAndCalculatesElsewhere() {
        val published = calc.day(LocalDate.of(2026, 10, 5))!!.copy(source = Source.DIYANET)
        val s = PrayerSchedule(location, listOf(published), calc::day, zone)
        assertEquals(Source.DIYANET, s.day(LocalDate.of(2026, 10, 5))!!.source)
        assertEquals(Source.CALCULATED, s.day(LocalDate.of(2026, 10, 6))!!.source)
        assertEquals(LocalDate.of(2026, 10, 5), s.diyanetUntil)
    }

    @Test
    fun nextAndCurrentAcrossMidnight() {
        val s = PrayerSchedule(location, emptyList(), calc::day, zone)
        val late = at(2026, 10, 5, 23, 30)
        assertEquals(Prayer.YATSI, s.current(late)!!.prayer)
        val next = s.next(late)!!
        assertEquals(Prayer.IMSAK, next.prayer)
        assertEquals(LocalDate.of(2026, 10, 6), next.day.date)
        val noon = at(2026, 10, 5, 12, 0)
        assertEquals(Prayer.GUNES, s.current(noon)!!.prayer)
        assertEquals(Prayer.OGLE, s.next(noon)!!.prayer)
    }

    @Test
    fun withoutAnyDataThereIsNothingNext() {
        val s = PrayerSchedule(location, emptyList(), null, zone)
        assertEquals(null, s.next(at(2026, 10, 5, 12, 0)))
    }

    @Test
    fun triggersIncludeRemindersForEnabledTimesOnly() {
        val s = PrayerSchedule(location, emptyList(), calc::day, zone)
        val now = at(2026, 10, 5, 12, 0)
        val ogle = s.next(now)!!
        val first = Triggers.next(s, now, 15, setOf(Prayer.OGLE))!!
        assertEquals(Triggers.Kind.REMINDER, first.kind)
        assertEquals(ogle.instant.minusSeconds(15 * 60), first.at)
        val noReminder = Triggers.next(s, now, 15, setOf(Prayer.AKSAM))!!
        assertEquals(Triggers.Kind.ENTRY, noReminder.kind)
        assertEquals(Prayer.OGLE, noReminder.event.prayer)
    }

    @Test
    fun dueCollectsEverythingSinceTheLastAlarm() {
        val s = PrayerSchedule(location, emptyList(), calc::day, zone)
        val due = Triggers.due(s, at(2026, 10, 5, 0, 0), at(2026, 10, 5, 23, 59), 10, Prayer.entries.toSet())
        assertEquals(12, due.size)
        assertTrue(due.zipWithNext().all { (a, b) -> !a.at.isAfter(b.at) })
    }

    @Test
    fun overlayDescribesTheNextTime() {
        val s = PrayerSchedule(location, emptyList(), calc::day, zone)
        val info = OverlayInfo.of(s, at(2026, 10, 5, 12, 0))!!
        assertEquals(Prayer.OGLE, info.next)
        assertEquals(Prayer.GUNES, info.current)
        assertEquals(6, info.today.size)
        assertEquals("İstanbul", info.place)
    }
}
