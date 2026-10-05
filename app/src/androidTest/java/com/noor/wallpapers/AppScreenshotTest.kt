package com.noor.wallpapers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Screenshots of the app itself, as she sees it: the "Yeni" popup an updating
 * user gets, every tab, the real photos and the settings. CI pulls them into
 * the device-previews contact sheets so UI changes can be looked at.
 */
@RunWith(AndroidJUnit4::class)
class AppScreenshotTest {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val context: Context = inst.targetContext
    private val out = File(context.getExternalFilesDir(null), "app-screens").apply { deleteRecursively(); mkdirs() }

    private fun shot(name: String, tab: String?, waitMs: Long = 4000) {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tab != null) intent.putExtra(MainActivity.EXTRA_TAB, tab)
        ActivityScenario.launch<MainActivity>(intent).use {
            Thread.sleep(waitMs)
            inst.waitForIdleSync()
            val bmp = inst.uiAutomation.takeScreenshot() ?: return
            File(out, "$name.png").outputStream().use { s -> bmp.compress(Bitmap.CompressFormat.PNG, 100, s) }
            bmp.recycle()
        }
    }

    @Test
    fun screenshotTheApp() {
        val sp = context.getSharedPreferences("noor", Context.MODE_PRIVATE)
        // Someone updating from 1.2.x: welcomed, and the old rotation popup already seen.
        sp.edit().putBoolean("welcomed", true).putBoolean("rotation_intro_seen", true).remove("seen_features").commit()
        shot("00-yeni", null)

        // Then the screens themselves, with nothing in front of them.
        sp.edit().putStringSet("seen_features", WhatsNew.ALL.map { it.id }.toSet()).commit()
        shot("01-galeri", "galeri")
        shot("02-fotograflar", MainActivity.TAB_PHOTOS, waitMs = 15_000)
        shot("03-vakitler", MainActivity.TAB_PRAYER)
        shot("04-kible", "kible")
        shot("05-zikir", "zikir")
        shot("06-takvim", "takvim")
        shot("07-ayarlar", MainActivity.TAB_SETTINGS)
        assertTrue(out.list()!!.size >= 8)
    }
}
