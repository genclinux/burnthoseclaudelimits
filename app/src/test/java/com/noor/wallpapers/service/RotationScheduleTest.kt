package com.noor.wallpapers.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RotationScheduleTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun at(zone: ZoneId, y: Int, mo: Int, d: Int, h: Int, mi: Int) = ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi), zone)

    @Test
    fun changesOnTheLocalClock() {
        val now = at(istanbul, 2026, 10, 5, 14, 37)
        assertEquals(at(istanbul, 2026, 10, 5, 14, 40), RotationSchedule.nextChange(now, RotationInterval.TEN_MINUTES))
        assertEquals(at(istanbul, 2026, 10, 5, 15, 0), RotationSchedule.nextChange(now, RotationInterval.HOUR))
        assertEquals(at(istanbul, 2026, 10, 6, 0, 0), RotationSchedule.nextChange(now, RotationInterval.DAY))
    }

    @Test
    fun onTheBoundaryTheNextChangeIsAFullIntervalAway() {
        val now = at(istanbul, 2026, 10, 5, 15, 0)
        assertEquals(at(istanbul, 2026, 10, 5, 16, 0), RotationSchedule.nextChange(now, RotationInterval.HOUR))
    }

    @Test
    fun consecutiveSlotsShowEachDesignInTurn() {
        val ids = listOf("a", "b", "c")
        var t = at(istanbul, 2026, 10, 5, 9, 0)
        val shown = (0 until 6).map {
            val id = RotationSchedule.pick(ids, RotationSchedule.slot(t, RotationInterval.TEN_MINUTES))
            t = RotationSchedule.nextChange(t, RotationInterval.TEN_MINUTES)
            id
        }
        assertEquals(shown.subList(0, 3).toSet(), ids.toSet())
        assertEquals(shown.subList(0, 3), shown.subList(3, 6))
    }

    @Test
    fun dailyFollowsLocalMidnightAcrossDst() {
        val berlin = ZoneId.of("Europe/Berlin")
        // Clocks go forward at 02:00 on 29 March 2026; midnight still exists.
        val now = at(berlin, 2026, 3, 28, 22, 0)
        assertEquals(at(berlin, 2026, 3, 29, 0, 0), RotationSchedule.nextChange(now, RotationInterval.DAY))
        // Inside the gap, the change moves to the first valid instant.
        val gap = at(berlin, 2026, 3, 29, 1, 55)
        assertEquals(at(berlin, 2026, 3, 29, 3, 0), RotationSchedule.nextChange(gap, RotationInterval.TEN_MINUTES))
    }

    @Test
    fun nothingToPickFromAnEmptyList() {
        assertNull(RotationSchedule.pick(emptyList(), 5))
    }
}
