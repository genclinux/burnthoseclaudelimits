package com.noor.wallpapers.prayer

import kotlin.math.cos
import kotlin.math.sqrt

/** One of Türkiye's 81 provinces, at its city centre. */
data class Province(val plate: Int, val name: String, val latitude: Double, val longitude: Double)

/**
 * Türkiye's provinces with the coordinates of each city centre. They let the
 * app work before it has ever been online (times are calculated), point the
 * qibla compass without a location permission, and turn a GPS fix into the
 * right entry in Diyanet's lists.
 */
object Provinces {
    const val TURKEY_ZONE = "Europe/Istanbul"

    val ALL = listOf(
        Province(1, "Adana", 37.0000, 35.3213),
        Province(2, "Adıyaman", 37.7648, 38.2786),
        Province(3, "Afyonkarahisar", 38.7507, 30.5567),
        Province(4, "Ağrı", 39.7191, 43.0503),
        Province(5, "Amasya", 40.6499, 35.8353),
        Province(6, "Ankara", 39.9334, 32.8597),
        Province(7, "Antalya", 36.8969, 30.7133),
        Province(8, "Artvin", 41.1828, 41.8183),
        Province(9, "Aydın", 37.8560, 27.8416),
        Province(10, "Balıkesir", 39.6484, 27.8826),
        Province(11, "Bilecik", 40.1426, 29.9793),
        Province(12, "Bingöl", 38.8847, 40.4939),
        Province(13, "Bitlis", 38.4006, 42.1095),
        Province(14, "Bolu", 40.7350, 31.6061),
        Province(15, "Burdur", 37.7203, 30.2908),
        Province(16, "Bursa", 40.1885, 29.0610),
        Province(17, "Çanakkale", 40.1553, 26.4142),
        Province(18, "Çankırı", 40.6013, 33.6134),
        Province(19, "Çorum", 40.5506, 34.9556),
        Province(20, "Denizli", 37.7765, 29.0864),
        Province(21, "Diyarbakır", 37.9144, 40.2306),
        Province(22, "Edirne", 41.6818, 26.5623),
        Province(23, "Elazığ", 38.6810, 39.2264),
        Province(24, "Erzincan", 39.7464, 39.4914),
        Province(25, "Erzurum", 39.9055, 41.2658),
        Province(26, "Eskişehir", 39.7767, 30.5206),
        Province(27, "Gaziantep", 37.0662, 37.3833),
        Province(28, "Giresun", 40.9128, 38.3895),
        Province(29, "Gümüşhane", 40.4386, 39.5086),
        Province(30, "Hakkari", 37.5744, 43.7408),
        Province(31, "Hatay", 36.2021, 36.1600),
        Province(32, "Isparta", 37.7648, 30.5566),
        Province(33, "Mersin", 36.8121, 34.6415),
        Province(34, "İstanbul", 41.0082, 28.9784),
        Province(35, "İzmir", 38.4237, 27.1428),
        Province(36, "Kars", 40.6085, 43.0975),
        Province(37, "Kastamonu", 41.3781, 33.7754),
        Province(38, "Kayseri", 38.7312, 35.4787),
        Province(39, "Kırklareli", 41.7351, 27.2252),
        Province(40, "Kırşehir", 39.1425, 34.1709),
        Province(41, "Kocaeli", 40.7654, 29.9408),
        Province(42, "Konya", 37.8746, 32.4932),
        Province(43, "Kütahya", 39.4200, 29.9833),
        Province(44, "Malatya", 38.3552, 38.3095),
        Province(45, "Manisa", 38.6191, 27.4289),
        Province(46, "Kahramanmaraş", 37.5858, 36.9371),
        Province(47, "Mardin", 37.3212, 40.7245),
        Province(48, "Muğla", 37.2153, 28.3636),
        Province(49, "Muş", 38.7461, 41.4910),
        Province(50, "Nevşehir", 38.6247, 34.7142),
        Province(51, "Niğde", 37.9698, 34.6766),
        Province(52, "Ordu", 40.9862, 37.8797),
        Province(53, "Rize", 41.0201, 40.5234),
        Province(54, "Sakarya", 40.7731, 30.3948),
        Province(55, "Samsun", 41.2928, 36.3313),
        Province(56, "Siirt", 37.9274, 41.9420),
        Province(57, "Sinop", 42.0264, 35.1551),
        Province(58, "Sivas", 39.7477, 37.0179),
        Province(59, "Tekirdağ", 40.9781, 27.5117),
        Province(60, "Tokat", 40.3139, 36.5544),
        Province(61, "Trabzon", 41.0015, 39.7178),
        Province(62, "Tunceli", 39.1080, 39.5480),
        Province(63, "Şanlıurfa", 37.1591, 38.7969),
        Province(64, "Uşak", 38.6742, 29.4058),
        Province(65, "Van", 38.5012, 43.3730),
        Province(66, "Yozgat", 39.8200, 34.8044),
        Province(67, "Zonguldak", 41.4564, 31.7987),
        Province(68, "Aksaray", 38.3687, 34.0370),
        Province(69, "Bayburt", 40.2552, 40.2249),
        Province(70, "Karaman", 37.1811, 33.2150),
        Province(71, "Kırıkkale", 39.8468, 33.5153),
        Province(72, "Batman", 37.8812, 41.1351),
        Province(73, "Şırnak", 37.5180, 42.4600),
        Province(74, "Bartın", 41.6358, 32.3375),
        Province(75, "Ardahan", 41.1105, 42.7022),
        Province(76, "Iğdır", 39.9237, 44.0450),
        Province(77, "Yalova", 40.6550, 29.2769),
        Province(78, "Karabük", 41.2000, 32.6333),
        Province(79, "Kilis", 36.7184, 37.1212),
        Province(80, "Osmaniye", 37.0746, 36.2464),
        Province(81, "Düzce", 40.8438, 31.1565),
    )

    /** Alphabetical in Turkish order (Ç after C, Ş after S). */
    val SORTED: List<Province> by lazy {
        val collator = java.text.Collator.getInstance(TurkishText.TR)
        ALL.sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    fun byName(name: String): Province? {
        val key = TurkishText.matchKey(name)
        // Diyanet has listed Hatay as "Antakya" and Kocaeli as "İzmit" in places.
        val alias = mapOf("antakya" to "hatay", "izmit" to "kocaeli", "adapazari" to "sakarya", "icel" to "mersin")[key] ?: key
        return ALL.firstOrNull { TurkishText.matchKey(it.name) == alias }
    }

    /** The province whose centre is closest to the point (fine for picking a province, not for borders). */
    fun nearest(lat: Double, lon: Double): Province = ALL.minBy { p ->
        val dLat = p.latitude - lat
        val dLon = (p.longitude - lon) * cos(Math.toRadians(lat))
        sqrt(dLat * dLat + dLon * dLon)
    }

    /** Roughly inside Türkiye's borders: decides whether a GPS fix should use the province table. */
    fun isInTurkey(lat: Double, lon: Double) = lat in 35.8..42.2 && lon in 25.6..44.9

    /**
     * Pendik, İstanbul: the app's home until she picks another place. Diyanet's
     * ids are looked up by name on the first fetch.
     */
    val PENDIK = PrayerLocation(
        countryId = null, country = "Türkiye", cityId = null, city = "İstanbul",
        districtId = null, district = "Pendik", latitude = 40.8769, longitude = 29.2346, zoneId = TURKEY_ZONE,
    )

    fun location(p: Province) = PrayerLocation(
        countryId = null, country = "Türkiye", cityId = null, city = p.name,
        districtId = null, district = p.name, latitude = p.latitude, longitude = p.longitude, zoneId = TURKEY_ZONE,
    )
}
