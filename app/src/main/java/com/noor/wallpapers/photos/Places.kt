package com.noor.wallpapers.photos

/**
 * The real places in "Gerçek Camiler": Pendik first and most, a handful of
 * İstanbul's great mosques, then Islamic architecture around the world.
 * Each is a Commons search; the photos are chosen in [Commons.rank].
 */
object Places {
    private fun pendik(id: String, title: String, query: String, take: Int) = Place(id, title, Region.PENDIK, query, take)
    private fun istanbul(id: String, title: String, query: String) = Place(id, title, Region.ISTANBUL, query, 1)
    private fun world(id: String, title: String, query: String) = Place(id, title, Region.WORLD, query, 2)

    val ALL: List<Place> = listOf(
        pendik("pendik-camileri", "Pendik'in camileri", "Pendik (camii OR cami OR mosque OR minaret)", 10),
        pendik("pendik-sahil", "Pendik sahili", "Pendik (sahil OR marina OR coast OR shore OR sunset OR iskele OR sea)", 6),
        pendik("pendik-kurtkoy", "Kurtköy ve Kaynarca", "(Kurtköy OR Kaynarca) (camii OR cami OR mosque)", 2),

        istanbul("camlica", "Büyük Çamlıca Camii", "\"Çamlıca Mosque\""),
        istanbul("suleymaniye", "Süleymaniye Camii", "\"Süleymaniye Mosque\""),
        istanbul("sultanahmet", "Sultanahmet Camii", "\"Sultan Ahmed Mosque\""),
        istanbul("ayasofya", "Ayasofya-i Kebîr Camii", "\"Hagia Sophia\" Istanbul"),
        istanbul("ortakoy", "Ortaköy Camii", "\"Ortaköy Mosque\""),
        istanbul("rustempasa", "Rüstem Paşa Camii", "\"Rüstem Pasha Mosque\""),
        istanbul("mihrimah", "Mihrimah Sultan Camii", "\"Mihrimah Sultan Mosque\""),

        world("haram", "Mescid-i Haram · Mekke", "\"Masjid al-Haram\""),
        world("nebevi", "Mescid-i Nebevî · Medine", "\"Prophet's Mosque\" Medina"),
        world("kubbetussahra", "Kubbetü's-Sahra · Kudüs", "\"Dome of the Rock\""),
        world("aksa", "Mescid-i Aksa · Kudüs", "\"Al-Aqsa Mosque\""),
        world("selimiye", "Selimiye Camii · Edirne", "\"Selimiye Mosque\" Edirne"),
        world("bursa", "Ulu Cami · Bursa", "\"Grand Mosque of Bursa\""),
        world("divrigi", "Divriği Ulu Camii · Sivas", "Divriği \"Great Mosque\""),
        world("sheikhzayed", "Şeyh Zayed Camii · Abu Dabi", "\"Sheikh Zayed Grand Mosque\""),
        world("sultanqaboos", "Sultan Kabus Camii · Maskat", "\"Sultan Qaboos Grand Mosque\""),
        world("hasan2", "II. Hasan Camii · Kazablanka", "\"Hassan II Mosque\""),
        world("kayrevan", "Ukbe bin Nâfi Camii · Kayrevan", "\"Great Mosque of Kairouan\""),
        world("kurtuba", "Kurtuba Ulu Camii · Endülüs", "Córdoba Mezquita"),
        world("elhamra", "Elhamra · Gırnata", "Alhambra Granada"),
        world("emevi", "Emevî Camii · Şam", "\"Umayyad Mosque\""),
        world("ibntolun", "İbn Tolun Camii · Kahire", "\"Mosque of Ibn Tulun\""),
        world("isfahan", "İmam Camii · İsfahan", "\"Shah Mosque\" Isfahan"),
        world("nasirulmulk", "Nasırülmülk Camii · Şiraz", "\"Nasir al-Mulk Mosque\""),
        world("registan", "Registan · Semerkant", "Registan Samarkand"),
        world("badshahi", "Bâdşâhî Camii · Lahor", "\"Badshahi Mosque\""),
        world("faisal", "Faysal Camii · İslamabad", "\"Faisal Mosque\""),
        world("tacmahal", "Tâc Mahal · Agra", "\"Taj Mahal\""),
        world("cenne", "Cenne Ulu Camii · Mali", "\"Great Mosque of Djenné\""),
        world("brunei", "Ömer Ali Seyfeddin Camii · Brunei", "\"Omar Ali Saifuddien Mosque\""),
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id }
}
