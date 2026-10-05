package com.noor.wallpapers.art

import com.noor.wallpapers.prayer.Qibla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendikTest {
    @Test
    fun pendikHasItsOwnTabAndAPieceInTheOthers() {
        val pendik = Catalog.visible(false).filter { it.category == Category.PENDIK }
        assertTrue(pendik.size >= 8)
        for (c in listOf(Category.SULUBOYA, Category.NIGHT, Category.RAMADAN, Category.EBRU, Category.LEVHA, Category.CINI, Category.GEOMETRIC)) {
            assertTrue(c.name, pendik.any { it.inCategory(c) })
        }
        // Showing up in another tab must not move it out of its own.
        assertTrue(pendik.all { it.inCategory(Category.PENDIK) })
        assertTrue(Catalog.entries.none { it.category in it.alsoIn })
    }

    @Test
    fun qiblaFromPendikPointsSouthEast() {
        val bearing = Qibla.bearing(PendikArt.LAT, PendikArt.LON)
        assertEquals(152.0, bearing, 1.5)
    }

    @Test
    fun mahyaSpellsTurkishLetters() {
        // Every letter of the default message is lit, dotted İ and cedilla Ş included.
        val dots = MahyaFont.dots("İŞ")
        assertTrue(dots.any { (x, y) -> x == 2 && y == 0 })      // the dot over İ
        assertTrue(dots.any { (x, y) -> x == 8 && y == 8 })      // the cedilla under Ş
        assertEquals(5 + 1 + 5, MahyaFont.width("AB"))
        for (ch in MahyaArt.DEFAULT_TEXT.filter { it != ' ' }) assertTrue("$ch", MahyaFont.dots("$ch").isNotEmpty())
    }

    @Test
    fun mahyaWrapsWhenTheMessageIsTooLongForOneLine() {
        val (one, _) = MahyaFont.layout("HOŞ GELDİN", 1000.0, 400.0, 10.0, 17.0)
        assertEquals(1, one.size)
        val (two, pitch) = MahyaFont.layout("HOŞ GELDİN YA ŞEHR-İ RAMAZAN", 700.0, 400.0, 10.0, 17.0)
        assertTrue(two.size >= 2)
        assertTrue(pitch >= 10.0)
        assertEquals("HOŞ GELDİN YA ŞEHR-İ RAMAZAN", two.joinToString(" "))
        // Split where the lines come out evenest, not after the first word.
        assertEquals(listOf("HOŞ GELDİN", "RAMAZAN"), MahyaFont.layout("HOŞ GELDİN RAMAZAN", 650.0, 400.0, 10.0, 17.0).first)
    }
}
