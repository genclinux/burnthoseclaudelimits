package com.noor.wallpapers.service

import android.content.Context
import com.noor.wallpapers.prayer.Calibration
import com.noor.wallpapers.prayer.Diyanet
import com.noor.wallpapers.prayer.HijriCalendar
import com.noor.wallpapers.prayer.Place
import com.noor.wallpapers.prayer.PrayerCalculator
import com.noor.wallpapers.prayer.PrayerLocation
import com.noor.wallpapers.prayer.PrayerSchedule
import com.noor.wallpapers.prayer.Provinces
import com.noor.wallpapers.prayer.ReligiousDays
import com.noor.wallpapers.prayer.ShiftedHijri
import com.noor.wallpapers.prayer.TurkishText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

/** Fetches Diyanet's lists and tables over HTTPS, keeping place lists on disk for a month. */
object DiyanetApi {
    private const val LIST_MAX_AGE_MS = 30L * 24 * 3600 * 1000

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 25_000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "HBSnoor (Android)")
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Sunucu yanıtı: HTTP $code")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun cachedList(context: Context, name: String, url: String, parse: (String) -> List<Place>): List<Place> {
        val file = File(File(context.cacheDir, "diyanet").apply { mkdirs() }, "$name.json")
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < LIST_MAX_AGE_MS
        if (fresh) runCatching { parse(file.readText()) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        return try {
            val json = get(url)
            val list = parse(json)
            if (list.isEmpty()) throw IOException("Liste boş geldi")
            file.writeText(json)
            list
        } catch (e: Exception) {
            // Offline: an old list is better than none.
            if (file.exists()) runCatching { parse(file.readText()) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
            throw e
        }
    }

    /** Countries, with Türkiye first. */
    suspend fun countries(context: Context): List<Place> =
        cachedList(context, "ulkeler", Diyanet.countriesUrl(), Diyanet::parseCountries)
            .sortedWith(compareBy<Place> { !Diyanet.isTurkey(it) }.thenBy(collator()) { it.title })

    suspend fun cities(context: Context, countryId: String): List<Place> =
        cachedList(context, "sehirler-$countryId", Diyanet.citiesUrl(countryId), Diyanet::parseCities)
            .sortedWith(compareBy<Place, String>(collator()) { it.title })

    /** Districts, with the one named after the city (its centre) first. */
    suspend fun districts(context: Context, cityId: String, cityName: String? = null): List<Place> {
        val key = cityName?.let { TurkishText.matchKey(it) }
        return cachedList(context, "ilceler-$cityId", Diyanet.districtsUrl(cityId), Diyanet::parseDistricts)
            .sortedWith(compareBy<Place> { TurkishText.matchKey(it.name) != key }.thenBy(collator()) { it.title })
    }

    private fun collator(): Comparator<String> {
        val c = java.text.Collator.getInstance(TurkishText.TR)
        return Comparator { a, b -> c.compare(a, b) }
    }
}

/**
 * The prayer times the whole app reads: Diyanet's table for the chosen place,
 * cached on disk, with a calibrated calculation for days outside it.
 */
object PrayerRepository {
    private const val TIMES_FILE = "prayer-times.json"

    /** Refresh when fewer than this many days of Diyanet's table remain. */
    private const val MIN_DAYS_AHEAD = 10L

    @Volatile private var memo: Pair<String, PrayerSchedule>? = null

    fun zoneFor(loc: PrayerLocation): ZoneId =
        loc.zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: if (TurkishText.matchKey(loc.country) in setOf("turkiye", "turkey")) ZoneId.of(Provinces.TURKEY_ZONE)
            else ZoneId.systemDefault()

    private fun timesFile(context: Context) = File(context.filesDir, TIMES_FILE)

    /** The current schedule, or null before a place has been chosen. Cheap to call repeatedly. */
    fun schedule(context: Context): PrayerSchedule? {
        val settings = AppSettings(context)
        val loc = settings.location ?: return null
        val file = timesFile(context)
        val key = "$loc|${file.lastModified()}|${settings.calibration}"
        memo?.let { (k, s) -> if (k == key) return s }
        val zone = zoneFor(loc)
        val days = if (loc.hasDiyanet && file.exists()) {
            runCatching { Diyanet.parseTimes(file.readText(), zone) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val calc = if (loc.hasCoordinates) PrayerCalculator(loc.latitude!!, loc.longitude!!, zone, settings.calibration) else null
        val schedule = PrayerSchedule(loc, days, calc?.let { c -> { d: LocalDate -> c.day(d) } }, zone)
        memo = key to schedule
        return schedule
    }

    /** The Hijri calendar, moved by a day where Diyanet's table says it should be. */
    fun hijri(context: Context): HijriCalendar =
        schedule(context)?.let { ShiftedHijri.matching(IcuHijri, it.published) } ?: IcuHijri

    /** Whether a kandil night begins on the evening of [date] (it runs on past midnight). */
    fun isKandilEvening(context: Context, date: LocalDate): Boolean =
        ReligiousDays.on(date, hijri(context)).any { it.day.night && it.date == date }

    /** Sets a new place; its table is fetched by [refresh]. */
    fun setLocation(context: Context, loc: PrayerLocation) {
        val settings = AppSettings(context)
        settings.location = loc
        settings.calibration = Calibration.NONE
        settings.fetchedAt = 0L
        timesFile(context).delete()
        memo = null
        settings.bumpPrayerVersion()
    }

    fun needsRefresh(context: Context): Boolean {
        val s = schedule(context) ?: return false
        if (!s.location.hasDiyanet) return TurkishText.matchKey(s.location.country) in setOf("turkiye", "turkey")
        val until = s.diyanetUntil ?: return true
        return until.isBefore(LocalDate.now(s.zone).plusDays(MIN_DAYS_AHEAD))
    }

    /**
     * Downloads Diyanet's table when it is missing or running out (or always,
     * with [force]). A place chosen offline from the province list is matched
     * to Diyanet's ids first. Returns the number of days now covered.
     */
    suspend fun refresh(context: Context, force: Boolean = false): Int {
        val settings = AppSettings(context)
        var loc = settings.location ?: throw IllegalStateException("Önce bir konum seç")
        if (!force && !needsRefresh(context)) return schedule(context)?.diyanetDays ?: 0
        if (!loc.hasDiyanet) {
            loc = resolveDiyanet(context, loc) ?: return 0
            settings.location = loc
        }
        val zone = zoneFor(loc)
        val json = DiyanetApi.get(Diyanet.timesUrl(loc.districtId!!))
        val days = Diyanet.parseTimes(json, zone)
        if (days.isEmpty()) throw IOException("Diyanet'ten vakit gelmedi")
        withContext(Dispatchers.IO) {
            val file = timesFile(context)
            val tmp = File(file.parentFile, "$TIMES_FILE.tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
        if (loc.hasCoordinates) {
            settings.calibration = Calibration.measure(days, PrayerCalculator(loc.latitude!!, loc.longitude!!, zone))
        }
        settings.fetchedAt = System.currentTimeMillis()
        memo = null
        settings.bumpPrayerVersion()
        return days.size
    }

    /** Finds a Turkish province (and its central district) in Diyanet's lists. */
    private suspend fun resolveDiyanet(context: Context, loc: PrayerLocation): PrayerLocation? {
        val turkey = DiyanetApi.countries(context).firstOrNull(Diyanet::isTurkey) ?: return null
        val cityKey = TurkishText.matchKey(loc.city)
        val city = DiyanetApi.cities(context, turkey.id).firstOrNull {
            TurkishText.matchKey(it.name) == cityKey || Provinces.byName(it.name)?.name == loc.city
        } ?: return null
        val districts = DiyanetApi.districts(context, city.id, city.name)
        val district = districts.firstOrNull { TurkishText.matchKey(it.name) == TurkishText.matchKey(loc.district) }
            ?: districts.firstOrNull() ?: return null
        return loc.copy(
            countryId = turkey.id, country = turkey.title, cityId = city.id, city = city.title,
            districtId = district.id, district = district.title,
        )
    }
}
