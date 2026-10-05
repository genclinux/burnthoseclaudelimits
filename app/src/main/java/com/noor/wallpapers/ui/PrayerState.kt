package com.noor.wallpapers.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.noor.wallpapers.prayer.PrayerSchedule
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.PrayerRepository
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Increments whenever an app setting changes (any, or only those in [keys]),
 * so screens re-read what they show.
 */
@Composable
fun rememberSettingsVersion(keys: Set<String>? = null): Int {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val settings = AppSettings(context)
        // SharedPreferences holds listeners weakly; this one lives as long as the effect.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (keys == null || (key != null && key in keys)) version++ }
        settings.register(listener)
        onDispose { settings.unregister(listener) }
    }
    return version
}

/** The current prayer schedule, re-read when the place or the table changes. */
@Composable
fun rememberSchedule(): PrayerSchedule? {
    val context = LocalContext.current
    val v = rememberSettingsVersion()
    return remember(v) { PrayerRepository.schedule(context) }
}

/** The time, ticking every [stepMs] on the boundary. */
@Composable
fun rememberNow(stepMs: Long = 1000L): Instant {
    val now by produceState(Instant.now(), stepMs) {
        while (true) {
            delay(stepMs - System.currentTimeMillis() % stepMs)
            value = Instant.now()
        }
    }
    return now
}

/** The bundled Amiri, for Arabic set in the app's own screens. */
@Composable
fun rememberAmiri(): FontFamily {
    val context = LocalContext.current
    return remember { amiri(context) }
}

private fun amiri(context: Context) = FontFamily(Font("fonts/Amiri-Bold.ttf", context.assets))
