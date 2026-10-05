package com.noor.wallpapers.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.noor.wallpapers.R

/**
 * The app icons she can choose from. Each is an <activity-alias> in the
 * manifest that opens MainActivity; exactly one is enabled, and that is the
 * icon the launcher shows.
 */
object AppIcons {
    enum class Icon(
        val alias: String,
        val title: String,
        /** Background (centre, edge) and foreground drawable, for the preview in Ayarlar. */
        val bg: Pair<Long, Long>,
        val fg: Int,
    ) {
        ZUMRUT("Zumrut", "Zümrüt", 0xFF14644A to 0xFF02140E, R.drawable.ic_launcher_foreground),
        BETUL("Betul", "Betül", 0xFF1E5562 to 0xFF041217, R.drawable.ic_fg_betul),
        GECE("Gece", "Hilâl", 0xFF1B2A5A to 0xFF050A1C, R.drawable.ic_fg_hilal),
        NUR("Nur", "Nur", 0xFFFFF6DC to 0xFFE3C88E, R.drawable.ic_fg_nur),
        MARMARA("Marmara", "Marmara", 0xFF2F6E8F to 0xFF071A26, R.drawable.ic_fg_marmara),
    }

    private fun component(context: Context, icon: Icon) = ComponentName(context.packageName, "com.noor.wallpapers.icon.${icon.alias}")

    /** The icon the launcher shows now (the manifest's default until she picks another). */
    fun current(context: Context): Icon {
        val pm = context.packageManager
        return Icon.entries.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == Icon.ZUMRUT
                else -> false
            }
        } ?: Icon.ZUMRUT
    }

    /** Switches the launcher icon. The new one is enabled before the old goes, so the app is never without one. */
    fun set(context: Context, icon: Icon) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        for (other in Icon.entries) {
            if (other != icon) {
                pm.setComponentEnabledSetting(component(context, other), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
