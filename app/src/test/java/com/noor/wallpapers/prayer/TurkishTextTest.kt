package com.noor.wallpapers.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class TurkishTextTest {
    @Test
    fun titleCasesDiyanetNames() {
        assertEquals("Kadıköy", TurkishText.titleCase("KADIKÖY"))
        assertEquals("İstanbul", TurkishText.titleCase("İSTANBUL"))
        assertEquals("Afyonkarahisar", TurkishText.titleCase("AFYONKARAHİSAR"))
        assertEquals("Bandırma (Balıkesir)", TurkishText.titleCase("BANDIRMA (BALIKESİR)"))
        assertEquals("A.B.D.", TurkishText.titleCase("A.B.D."))
    }

    @Test
    fun matchKeysIgnoreCaseAndDiacritics() {
        assertEquals(TurkishText.matchKey("ŞANLIURFA"), TurkishText.matchKey("Sanliurfa"))
        assertEquals(TurkishText.matchKey("İzmir"), TurkishText.matchKey("IZMIR"))
        assertEquals("kahramanmaras", TurkishText.matchKey("KAHRAMANMARAŞ"))
    }

    @Test
    fun countdownRoundsUpToTheMinute() {
        assertEquals("1 sa 23 dk", TurkishText.countdown(Duration.ofSeconds(3600 + 22 * 60 + 10)))
        assertEquals("2 sa", TurkishText.countdown(Duration.ofHours(2)))
        assertEquals("1 dk", TurkishText.countdown(Duration.ofSeconds(20)))
        assertEquals("1:02:03", TurkishText.clock(Duration.ofSeconds(3723)))
        assertEquals("02:03", TurkishText.clock(Duration.ofSeconds(123)))
    }

    @Test
    fun locativeSuffixFollowsHowTheTimeIsRead() {
        fun at(h: Int, m: Int) = TurkishText.at(java.time.LocalTime.of(h, m))
        assertEquals("13:12'de", at(13, 12))
        assertEquals("13:10'da", at(13, 10))
        assertEquals("18:40'ta", at(18, 40))
        assertEquals("05:43'te", at(5, 43))
        assertEquals("19:06'da", at(19, 6))
        assertEquals("12:00'de", at(12, 0))
        assertEquals("13:00'te", at(13, 0))
        assertEquals("20:00'de", at(20, 0))
        assertEquals("16:09'da", at(16, 9))
    }

    @Test
    fun formatsDates() {
        assertEquals("5 Ekim 2026 Pazartesi", TurkishText.longDate(LocalDate.of(2026, 10, 5)))
    }
}
