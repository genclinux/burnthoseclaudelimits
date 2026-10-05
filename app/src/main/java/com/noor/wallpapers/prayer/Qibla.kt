package com.noor.wallpapers.prayer

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The direction and distance to the Kaaba along the great circle. */
object Qibla {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Degrees clockwise from true north. */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun distanceKm(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dp = p2 - p1
        val dl = Math.toRadians(KAABA_LON - lon)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** "Güneydoğu" etc. for a bearing, as a reading aid next to the number. */
    fun compassName(bearing: Double): String {
        val names = listOf("Kuzey", "Kuzeydoğu", "Doğu", "Güneydoğu", "Güney", "Güneybatı", "Batı", "Kuzeybatı")
        return names[(((bearing % 360) + 360 + 22.5) / 45).toInt() % 8]
    }

    /** Shortest signed turn from [from] to [to], in -180..180 degrees. */
    fun turn(from: Double, to: Double): Double {
        var d = (to - from) % 360
        if (d > 180) d -= 360
        if (d < -180) d += 360
        return d
    }
}
