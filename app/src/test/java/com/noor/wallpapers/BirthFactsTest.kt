package com.noor.wallpapers

import com.noor.wallpapers.art.BirthNightArt
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.MoonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class BirthFactsTest {
    private val birth = LocalDate.of(2000, 9, 12)

    @Test
    fun theDayShewasBorn() {
        assertEquals(java.time.DayOfWeek.TUESDAY, birth.dayOfWeek)
        // The first full moon after was the evening of 13 September 2000; that night it was nearly full.
        val illum = MoonPhase.illumination(BirthNightArt.EVENING)
        assertTrue("was $illum", illum in 0.97..1.0)
        assertTrue(MoonPhase.waxing(BirthNightArt.EVENING))
        // And a known new moon reads as new.
        val newMoon = LocalDateTime.of(2026, 10, 10, 15, 50).toInstant(ZoneOffset.UTC)
        assertTrue(MoonPhase.illumination(newMoon) < 0.02)
    }

    @Test
    fun counts() {
        val f = BirthFacts.of(birth, LocalDate.of(2026, 10, 5))
        assertEquals(9520L, f.dayNumber)
        assertEquals(26, f.years)
        assertEquals(342L, f.daysToBirthday)
        // 15 Sep 2000 was the first Friday; 2 Oct 2026 the last before 5 Oct.
        assertEquals((java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2000, 9, 15), LocalDate.of(2026, 10, 2)) / 7) + 1, f.fridays)

        val onTheDay = BirthFacts.of(birth, LocalDate.of(2027, 9, 12))
        assertEquals(0L, onTheDay.daysToBirthday)
        assertEquals(27, onTheDay.years)
        assertEquals(1L, BirthFacts.of(birth, birth).dayNumber)
        assertEquals(0L, BirthFacts.of(birth, birth.plusDays(1)).fridays)
    }

    @Test
    fun ramadans() {
        // Born in Cemâziyelâhir 1421 (month 6); by Rebîülâhir 1448 (month 4): Ramadans 1421…1447.
        assertEquals(27, BirthFacts.ramadans(1421, 6, 1448, 4))
        assertEquals(28, BirthFacts.ramadans(1421, 6, 1448, 9))
    }

    @Test
    fun theBirthNightIsHiddenUntilFound() {
        assertFalse(Catalog.visible(true).any { it.id == Catalog.BIRTH_NIGHT })
        assertTrue(Catalog.visible(false, birthNight = true).any { it.id == Catalog.BIRTH_NIGHT })
        assertFalse(Catalog.visible(false, birthNight = true).any { it.id == Catalog.SECRET })
    }
}
