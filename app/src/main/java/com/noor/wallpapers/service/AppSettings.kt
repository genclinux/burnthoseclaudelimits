package com.noor.wallpapers.service

import android.content.Context
import android.content.SharedPreferences
import com.noor.wallpapers.prayer.Calibration
import com.noor.wallpapers.prayer.OverlayPosition
import com.noor.wallpapers.prayer.OverlayStyle
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.PrayerLocation
import java.time.MonthDay

/**
 * Settings for the features beyond wallpapers. Shares the "noor" preferences
 * file with [com.noor.wallpapers.wallpaper.Prefs], so the live wallpaper's
 * listener hears about prayer and overlay changes too.
 */
class AppSettings(context: Context) {
    private val sp: SharedPreferences = context.applicationContext.getSharedPreferences("noor", Context.MODE_PRIVATE)

    // Location ----------------------------------------------------------------

    var location: PrayerLocation?
        get() {
            val city = sp.getString("loc_city", null) ?: return null
            return PrayerLocation(
                countryId = sp.getString("loc_country_id", null),
                country = sp.getString("loc_country", null) ?: "Türkiye",
                cityId = sp.getString("loc_city_id", null),
                city = city,
                districtId = sp.getString("loc_district_id", null),
                district = sp.getString("loc_district", null) ?: city,
                latitude = sp.getString("loc_lat", null)?.toDoubleOrNull(),
                longitude = sp.getString("loc_lon", null)?.toDoubleOrNull(),
                zoneId = sp.getString("loc_zone", null),
            )
        }
        set(v) {
            sp.edit().apply {
                if (v == null) {
                    listOf("loc_country_id", "loc_country", "loc_city_id", "loc_city", "loc_district_id", "loc_district", "loc_lat", "loc_lon", "loc_zone")
                        .forEach { remove(it) }
                } else {
                    putString("loc_country_id", v.countryId)
                    putString("loc_country", v.country)
                    putString("loc_city_id", v.cityId)
                    putString("loc_city", v.city)
                    putString("loc_district_id", v.districtId)
                    putString("loc_district", v.district)
                    putString("loc_lat", v.latitude?.toString())
                    putString("loc_lon", v.longitude?.toString())
                    putString("loc_zone", v.zoneId)
                }
            }.apply()
        }

    var calibration: Calibration
        get() = Calibration.parse(sp.getString("prayer_calibration", null))
        set(v) = sp.edit().putString("prayer_calibration", v.toString()).apply()

    var fetchedAt: Long
        get() = sp.getLong("prayer_fetched_at", 0L)
        set(v) = sp.edit().putLong("prayer_fetched_at", v).apply()

    /** Bumped whenever the times change, so the live wallpaper and screens reload. */
    var prayerVersion: Int
        get() = sp.getInt(KEY_PRAYER_VERSION, 0)
        set(v) = sp.edit().putInt(KEY_PRAYER_VERSION, v).apply()

    fun bumpPrayerVersion() { prayerVersion = prayerVersion + 1 }

    // Notifications -------------------------------------------------------------

    var notificationsOn: Boolean
        get() = sp.getBoolean("notify_on", false)
        set(v) = sp.edit().putBoolean("notify_on", v).apply()

    /** Times that notify. Güneş is off by default: it ends the morning prayer rather than starting one. */
    var notifyPrayers: Set<Prayer>
        get() = sp.getStringSet("notify_prayers", null)
            ?.mapNotNull { n -> Prayer.entries.firstOrNull { it.name == n } }?.toSet()
            ?: Prayer.entries.filter { it.isPrayer }.toSet()
        set(v) = sp.edit().putStringSet("notify_prayers", v.map { it.name }.toSet()).apply()

    /** Minutes before a time for a reminder; 0 = none. */
    var reminderMinutes: Int
        get() = sp.getInt("remind_minutes", 0)
        set(v) = sp.edit().putInt("remind_minutes", v).apply()

    var holyDayNotifications: Boolean
        get() = sp.getBoolean("holy_day_notify", true)
        set(v) = sp.edit().putBoolean("holy_day_notify", v).apply()

    var lastTriggerAt: Long
        get() = sp.getLong("last_trigger_at", 0L)
        set(v) = sp.edit().putLong("last_trigger_at", v).apply()

    // Live wallpaper --------------------------------------------------------------

    /** null = no prayer panel on the live wallpaper. */
    var overlayPosition: OverlayPosition?
        get() = sp.getString(KEY_OVERLAY_POSITION, OverlayPosition.TOP.name)
            .let { n -> OverlayPosition.entries.firstOrNull { it.name == n } }
        set(v) = sp.edit().putString(KEY_OVERLAY_POSITION, v?.name ?: "OFF").apply()

    var overlayStyle: OverlayStyle
        get() = sp.getString(KEY_OVERLAY_STYLE, null)
            .let { n -> OverlayStyle.entries.firstOrNull { it.name == n } } ?: OverlayStyle.COMPACT
        set(v) = sp.edit().putString(KEY_OVERLAY_STYLE, v.name).apply()

    /** The live wallpaper changes design with the time of day: dawn, day, dusk, night. */
    var liveFollowsPrayer: Boolean
        get() = sp.getBoolean(KEY_LIVE_FOLLOWS, false)
        set(v) = sp.edit().putBoolean(KEY_LIVE_FOLLOWS, v).apply()

    // Daily wallpaper --------------------------------------------------------------

    var dailyWallpaper: Boolean
        get() = sp.getBoolean("daily_on", false)
        set(v) = sp.edit().putBoolean("daily_on", v).apply()

    var dailySource: DailySource
        get() = sp.getString("daily_source", null).let { n -> DailySource.entries.firstOrNull { it.name == n } } ?: DailySource.FAVORITES
        set(v) = sp.edit().putString("daily_source", v.name).apply()

    var dailyTarget: String
        get() = sp.getString("daily_target", "BOTH") ?: "BOTH"
        set(v) = sp.edit().putString("daily_target", v).apply()

    var dailyLastDate: String?
        get() = sp.getString("daily_last", null)
        set(v) = sp.edit().putString("daily_last", v).apply()

    // Widget and app look ----------------------------------------------------------

    /** Encoded Selection drawn behind the prayer widget. */
    var widgetSelection: String?
        get() = sp.getString("widget_selection", null)
        set(v) = sp.edit().putString("widget_selection", v).apply()

    /** Palette id the app's own colours follow; null = the original emerald and gold. */
    var themePalette: String?
        get() = sp.getString(KEY_THEME, null)
        set(v) = sp.edit().putString(KEY_THEME, v).apply()

    /** Saved custom palettes (palette ids, which carry their colours). */
    var customPalettes: List<String>
        get() = sp.getString("custom_palettes", null)?.split(';')?.filter { it.isNotBlank() } ?: emptyList()
        set(v) = sp.edit().putString("custom_palettes", v.joinToString(";")).apply()

    // Hanife Betül -------------------------------------------------------------------

    var birthday: MonthDay?
        get() = sp.getString("birthday", null)?.let { runCatching { MonthDay.parse(it) }.getOrNull() }
        set(v) = sp.edit().putString("birthday", v?.toString()).apply()

    /** Surprises she has found (see HanifeBetul.Surprise). */
    var foundSurprises: Set<String>
        get() = sp.getStringSet("found_surprises", emptySet())!!.toSet()
        set(v) = sp.edit().putStringSet("found_surprises", v).apply()

    /** The hidden Lâle · Hilâl · Allah design, unlocked from the tesbih. */
    var secretUnlocked: Boolean
        get() = sp.getBoolean(KEY_SECRET, false)
        set(v) = sp.edit().putBoolean(KEY_SECRET, v).apply()

    // Tesbih -----------------------------------------------------------------------

    var tesbihPreset: String
        get() = sp.getString("tesbih_preset", "tesbihat") ?: "tesbihat"
        set(v) = sp.edit().putString("tesbih_preset", v).apply()

    var tesbihCount: Int
        get() = sp.getInt("tesbih_count", 0)
        set(v) = sp.edit().putInt("tesbih_count", v).apply()

    /** For the tesbihat sequence: which of its phrases is being counted. */
    var tesbihStep: Int
        get() = sp.getInt("tesbih_step", 0)
        set(v) = sp.edit().putInt("tesbih_step", v).apply()

    var tesbihTotal: Long
        get() = sp.getLong("tesbih_total", 0L)
        set(v) = sp.edit().putLong("tesbih_total", v).apply()

    var tesbihHaptics: Boolean
        get() = sp.getBoolean("tesbih_haptics", true)
        set(v) = sp.edit().putBoolean("tesbih_haptics", v).apply()

    fun register(l: SharedPreferences.OnSharedPreferenceChangeListener) = sp.registerOnSharedPreferenceChangeListener(l)
    fun unregister(l: SharedPreferences.OnSharedPreferenceChangeListener) = sp.unregisterOnSharedPreferenceChangeListener(l)

    companion object {
        const val KEY_PRAYER_VERSION = "prayer_version"
        const val KEY_OVERLAY_POSITION = "overlay_position"
        const val KEY_OVERLAY_STYLE = "overlay_style"
        const val KEY_LIVE_FOLLOWS = "live_follows_prayer"
        const val KEY_THEME = "theme_palette"
        const val KEY_SECRET = "secret_unlocked"

        /** Keys the live wallpaper redraws for. */
        val LIVE_KEYS = setOf(KEY_PRAYER_VERSION, KEY_OVERLAY_POSITION, KEY_OVERLAY_STYLE, KEY_LIVE_FOLLOWS, "birthday")
    }
}

enum class DailySource(val title: String) {
    FAVORITES("Favorilerim"),
    HANIFE_BETUL("Hanife Betül koleksiyonu"),
    ALL("Tüm tasarımlar"),
}
