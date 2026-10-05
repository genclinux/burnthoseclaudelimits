package com.noor.wallpapers

/**
 * Every feature she should hear about, newest first. The app shows the ones
 * she hasn't seen in a popup when it opens.
 *
 * Adding a user-visible feature? Add an entry here, at the top, with the
 * version it ships in (see CLAUDE.md). Never reuse or rename an id: that
 * would show it again, or hide a new one.
 */
object WhatsNew {
    enum class Action(val label: String) {
        PICK_ROTATION("Döngüyü kur"),
        OPEN_PHOTOS("Fotoğraflara bak"),
    }

    data class Feature(
        val id: String,
        val version: String,
        val emoji: String,
        val title: String,
        val body: String,
        val action: Action? = null,
    )

    val ALL: List<Feature> = listOf(
        Feature(
            id = "v2-design",
            version = "2.0.0",
            emoji = "✨",
            title = "HBSnoor 2.0: yeni görünüm",
            body = "Her sekme aynı düzende: başlık solda, ⚙ Ayarlar her sekmenin sağ üstünde. Ayarlar konulara göre " +
                "kartlara ayrıldı, seçenekler aynı biçimde. Döngü artık Galeri'nin altında, adıyla duran bir düğme.",
        ),
        Feature(
            id = "real-mosques",
            version = "1.2.6",
            emoji = "📷",
            title = "Gerçek Camiler",
            body = "Galeri'de yeni bir bölüm: önce Pendik, sonra İstanbul'dan birkaç ve dünyanın dört bir yanından gerçek " +
                "cami fotoğrafları. Duvar kağıdı yapabilir, Döngü'ye ekleyebilirsin.",
            action = Action.OPEN_PHOTOS,
        ),
        Feature(
            id = "live-rotation",
            version = "1.2.5",
            emoji = "🌙",
            title = "Döngü canlı duvar kağıdında",
            body = "Seçtiğin tasarımlar canlı duvar kağıdında da sırayla değişebilir: yumuşak bir geçişle, yıldızlar ve " +
                "vakit paneli üstte kalarak.",
            action = Action.PICK_ROTATION,
        ),
        Feature(
            id = "rotation",
            version = "1.2.4",
            emoji = "🔄",
            title = "Duvar kağıdı döngüsü",
            body = "Koleksiyondan 10 taneye kadar duvar kağıdı seç; 10 dakikada, saatte ya da günde bir sırayla değişsin. " +
                "Ana ekran, kilit ekranı ya da ikisi için.",
            action = Action.PICK_ROTATION,
        ),
    )

    /** The version of the newest entries. */
    val latest: String get() = ALL.first().version

    /**
     * What to show, newest first. Someone who has used the app before sees
     * everything they missed; a first install, which has just had the
     * welcome, sees only what is new in this version.
     */
    fun toShow(seen: Set<String>, firstInstall: Boolean): List<Feature> =
        ALL.filter { it.id !in seen && (!firstInstall || it.version == latest) }

    /** Ids to remember as seen once [shown] has been on screen: on a first install, the older ones too. */
    fun seenAfter(seen: Set<String>, shown: List<Feature>, firstInstall: Boolean): Set<String> =
        seen + shown.map { it.id } + if (firstInstall) ALL.map { it.id } else emptyList()
}
