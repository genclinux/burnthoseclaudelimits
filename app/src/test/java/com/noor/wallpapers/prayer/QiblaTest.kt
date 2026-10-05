package com.noor.wallpapers.prayer

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaTest {
    @Test
    fun knownBearings() {
        // Published qibla directions (degrees from true north).
        assertEquals(151.6, Qibla.bearing(41.0082, 28.9784), 0.3) // İstanbul
        assertEquals(118.99, Qibla.bearing(51.5074, -0.1278), 0.3) // London
        assertEquals(58.48, Qibla.bearing(40.7128, -74.0060), 0.3) // New York
    }

    @Test
    fun distance() {
        assertEquals(2400.0, Qibla.distanceKm(41.0082, 28.9784), 30.0) // İstanbul to Mecca, about 2,400 km
        assertEquals(0.0, Qibla.distanceKm(Qibla.KAABA_LAT, Qibla.KAABA_LON), 1e-6)
    }

    @Test
    fun namesAndTurns() {
        assertEquals("Güneydoğu", Qibla.compassName(151.6))
        assertEquals("Kuzey", Qibla.compassName(359.0))
        assertEquals(-20.0, Qibla.turn(10.0, 350.0), 1e-9)
        assertEquals(20.0, Qibla.turn(350.0, 10.0), 1e-9)
    }

    @Test
    fun provinces() {
        assertEquals(81, Provinces.ALL.size)
        assertEquals((1..81).toList(), Provinces.ALL.map { it.plate }.sorted())
        assertEquals("İstanbul", Provinces.nearest(41.05, 29.05).name) // Üsküdar
        assertEquals("Hatay", Provinces.byName("ANTAKYA")?.name)
        assertEquals("Şanlıurfa", Provinces.byName("SANLIURFA")?.name)
        Provinces.ALL.forEach { assert(Provinces.isInTurkey(it.latitude, it.longitude)) { it.name } }
    }
}
