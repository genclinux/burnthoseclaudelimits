package com.noor.wallpapers.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class DiyanetTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun day(date: String, imsak: String, yatsi: String = "20:09", extra: String = "") = """
        {"Aksam":"18:47","AyinSekliURL":"https://namazvakti.diyanet.gov.tr/images/r7.gif","Gunes":"07:04",
         "GunesBatis":"18:40","GunesDogus":"07:11","HicriTarihKisa":"23.4.1448","HicriTarihUzun":"23 Rebiülahir 1448",
         "Ikindi":"16:06","Imsak":"$imsak","KibleSaati":"12:10","MiladiTarihKisa":"$date",
         "MiladiTarihUzun":"x","Ogle":"13:00","Yatsi":"$yatsi"$extra}
    """.trimIndent()

    @Test
    fun parsesDaysInOrder() {
        val json = "[${day("06.10.2026", "05:44")}, ${day("05.10.2026", "05:43", extra = ",\"GreenwichOrtalamaZamani\":3.0")}]"
        val days = Diyanet.parseTimes(json, istanbul)
        assertEquals(2, days.size)
        val d = days[0]
        assertEquals(LocalDate.of(2026, 10, 5), d.date)
        assertEquals(LocalTime.of(5, 43), d.time(Prayer.IMSAK))
        assertEquals(LocalTime.of(13, 0), d.time(Prayer.OGLE))
        assertEquals(LocalTime.of(20, 9), d.time(Prayer.YATSI))
        assertEquals(ZoneOffset.ofHours(3), d.offset)
        assertEquals(HijriDate(1448, 4, 23), d.hijri)
        assertEquals(LocalTime.of(12, 10), d.qiblaTime)
        assertEquals(Source.DIYANET, d.source)
        assertEquals("23 Rebiülahir 1448", d.hijri.toString())
    }

    @Test
    fun skipsBrokenDays() {
        val json = "[${day("05.10.2026", "25:99")}, ${day("bad", "05:43")}, ${day("07.10.2026", "05:45")}]"
        val days = Diyanet.parseTimes(json, istanbul)
        assertEquals(listOf(LocalDate.of(2026, 10, 7)), days.map { it.date })
    }

    @Test
    fun yatsiAfterMidnightFallsOnTheNextDay() {
        val d = Diyanet.parseTimes("[${day("21.06.2026", "02:10", yatsi = "00:40")}]", ZoneId.of("Europe/Oslo")).single()
        assertEquals(LocalDate.of(2026, 6, 22), d.instant(Prayer.YATSI).atZone(ZoneId.of("Europe/Oslo")).toLocalDate())
    }

    @Test
    fun parsesPlaceLists() {
        val cities = Diyanet.parseCities("""[{"SehirAdi":"İSTANBUL","SehirAdiEn":"ISTANBUL","SehirID":"539"},{"SehirAdi":"ŞANLIURFA","SehirAdiEn":"SANLIURFA","SehirID":577}]""")
        assertEquals(listOf(Place("539", "İSTANBUL"), Place("577", "ŞANLIURFA")), cities)
        assertEquals("Şanlıurfa", cities[1].title)
        val countries = Diyanet.parseCountries("""[{"UlkeAdi":"TÜRKİYE","UlkeAdiEn":"TURKEY","UlkeID":"2"}]""")
        assertTrue(Diyanet.isTurkey(countries.single()))
    }

    @Test
    fun timeParsingIsLenient() {
        assertEquals(LocalTime.of(5, 7), Diyanet.time("5:07"))
        assertEquals(LocalTime.of(5, 7), Diyanet.time("05:07:00"))
        assertNull(Diyanet.time("24:00"))
        assertNull(Diyanet.time(""))
        assertNull(Diyanet.hijri("31.13.1448"))
    }
}
