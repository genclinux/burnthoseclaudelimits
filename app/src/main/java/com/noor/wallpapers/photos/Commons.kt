package com.noor.wallpapers.photos

import com.noor.wallpapers.prayer.MiniJson
import java.net.URLEncoder

enum class Region(val title: String) {
    PENDIK("Pendik"),
    ISTANBUL("İstanbul"),
    WORLD("Dünyadan"),
}

/** A real place, found on Wikimedia Commons with [query]; [take] photos of it are shown. */
data class Place(val id: String, val title: String, val region: Region, val query: String, val take: Int)

/** A freely licensed photograph from Wikimedia Commons. */
data class Photo(
    val pageId: Long,
    val place: Place,
    val fileTitle: String,
    val width: Int,
    val height: Int,
    val url: String,
    /** A 1920px-wide rendition from Commons' thumbnailer, or null when the original is smaller. */
    val thumbUrl1920: String?,
    val pageUrl: String,
    val artist: String,
    val license: String,
    val licenseUrl: String?,
    /** 2 = featured picture, 1 = quality or valued image, 0 = other. */
    val quality: Int,
) {
    val id: String get() = ID_PREFIX + pageId
    val credit: String get() = "📷 $artist · $license"

    /**
     * The smallest Commons rendition at least [minWidth] wide (standard
     * thumbnail widths, so it is usually already cached), or the original.
     */
    fun urlForWidth(minWidth: Int): String {
        val thumb = thumbUrl1920 ?: return url
        val bucket = THUMB_WIDTHS.firstOrNull { it >= minWidth } ?: return url
        if (bucket >= width) return url
        return thumb.replace("/1920px-", "/${bucket}px-")
    }

    /** Width to download so the photo covers a [w] x [h] screen when centre-cropped. */
    fun widthToCover(w: Int, h: Int): Int {
        val scale = maxOf(w / width.toDouble(), h / height.toDouble())
        return Math.ceil(width * scale).toInt()
    }

    companion object {
        const val ID_PREFIX = "photo:"
        val THUMB_WIDTHS = listOf(250, 500, 960, 1280, 1920, 3840)

        fun isPhotoId(id: String) = id.startsWith(ID_PREFIX)
    }
}

/**
 * Wikimedia Commons search, without any Android in it: the request URL, the
 * response parsed into [Photo]s, and the choice of which ones to show.
 */
object Commons {
    const val API = "https://commons.wikimedia.org/w/api.php"
    const val USER_AGENT = "noorbyHBS/2.1 (https://github.com/genclinux/burnthoseclaudelimits; Android wallpaper app)"

    private const val QUALITY = "Category:Quality images"
    private const val VALUED = "Category:Valued images"
    private const val FEATURED = "Category:Featured pictures on Wikimedia Commons"

    fun searchUrl(place: Place): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val params = linkedMapOf(
            "action" to "query",
            "format" to "json",
            "formatversion" to "2",
            "generator" to "search",
            "gsrnamespace" to "6",
            "gsrlimit" to "40",
            "gsrsearch" to "${place.query} filetype:bitmap",
            "prop" to "imageinfo|categories",
            "iiprop" to "url|size|mime|extmetadata",
            "iiurlwidth" to "1920",
            "iiextmetadatafilter" to "Artist|LicenseShortName|LicenseUrl",
            "clcategories" to "$QUALITY|$VALUED|$FEATURED",
            "cllimit" to "max",
        )
        return API + "?" + params.entries.joinToString("&") { (k, v) -> "$k=${enc(v)}" }
    }

    /** Every usable photo in a search response, best first. */
    fun parse(json: String, place: Place): List<Photo> {
        @Suppress("UNCHECKED_CAST")
        val root = MiniJson.parse(json) as? Map<String, Any?> ?: return emptyList()
        val pages = (root["query"] as? Map<*, *>)?.get("pages") as? List<*> ?: return emptyList()
        return rank(pages.mapNotNull { photo(it as? Map<*, *> ?: return@mapNotNull null, place) })
    }

    private fun photo(page: Map<*, *>, place: Place): Photo? {
        val title = page["title"] as? String ?: return null
        val pageId = (page["pageid"] as? Double)?.toLong() ?: return null
        val info = (page["imageinfo"] as? List<*>)?.firstOrNull() as? Map<*, *> ?: return null
        val width = (info["width"] as? Double)?.toInt() ?: return null
        val height = (info["height"] as? Double)?.toInt() ?: return null
        val url = info["url"] as? String ?: return null
        if (info["mime"] != "image/jpeg") return null
        if (!usableShape(width, height) || unwanted(title)) return null
        val meta = info["extmetadata"] as? Map<*, *> ?: return null
        fun field(name: String) = ((meta[name] as? Map<*, *>)?.get("value") as? String)?.let(::plainText)
        val license = field("LicenseShortName") ?: return null
        if (!freeLicense(license)) return null
        val cats = (page["categories"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.get("title") as? String }.toSet()
        val quality = when {
            FEATURED in cats -> 2
            QUALITY in cats || VALUED in cats -> 1
            else -> 0
        }
        val thumb = (info["thumburl"] as? String)?.takeIf { "/1920px-" in it }
        return Photo(
            pageId = pageId,
            place = place,
            fileTitle = title,
            width = width,
            height = height,
            url = url,
            thumbUrl1920 = thumb,
            pageUrl = info["descriptionurl"] as? String ?: "https://commons.wikimedia.org/wiki/${title.replace(' ', '_')}",
            artist = field("Artist")?.takeIf { it.isNotBlank() }?.take(80) ?: "Wikimedia Commons",
            license = license,
            licenseUrl = field("LicenseUrl"),
            quality = quality,
        )
    }

    /** Big enough for a phone screen and not a sliver of a panorama. */
    fun usableShape(width: Int, height: Int): Boolean {
        if (minOf(width, height) < 1000 || maxOf(width, height) < 1600) return false
        val aspect = width / height.toDouble()
        return aspect in 0.45..2.0
    }

    private val UNWANTED = Regex(
        "\\b(map|harita|plan|logo|diagram|drawing|çizim|stamp|pul|coin|banknote|postcard|kartpostal|sign|tabela|" +
            "station|istasyon|metro|marmaray|bus|otobüs|football|stadium|stadyum|airport|havalimanı|hospital|hastane|" +
            "school|okul|election|seçim|protest)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** File names that are almost never the photograph we want. */
    fun unwanted(title: String) = UNWANTED.containsMatchIn(title)

    /** Licences that allow reuse with credit: CC0, CC BY, CC BY-SA, public domain. No NC or ND. */
    fun freeLicense(name: String): Boolean {
        val n = name.lowercase().trim()
        if (Regex("\\b(nc|nd)\\b").containsMatchIn(n.replace('-', ' '))) return false
        return n.startsWith("cc0") || n.startsWith("cc by") || n.startsWith("cc-by") ||
            n.startsWith("public domain") || n.startsWith("pd")
    }

    /** Featured, then quality; upright photos suit a phone better; then the sharper one. */
    fun rank(photos: List<Photo>): List<Photo> = photos.sortedWith(
        compareByDescending<Photo> { it.quality }
            .thenByDescending { it.height > it.width }
            .thenByDescending { it.width.toLong() * it.height },
    )

    /**
     * The photos to show from per-place results: each place's best [Place.take],
     * a photo found for two places shown once, in the order of [places].
     */
    fun choose(places: List<Place>, found: Map<String, List<Photo>>): List<Photo> {
        val seen = HashSet<Long>()
        return places.flatMap { p -> found[p.id].orEmpty().filter { seen.add(it.pageId) }.take(p.take) }
    }

    /** Commons' Artist field is HTML (links, spans); keep the words. */
    fun plainText(html: String): String = html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
