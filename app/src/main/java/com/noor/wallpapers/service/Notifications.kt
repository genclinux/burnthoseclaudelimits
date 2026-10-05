package com.noor.wallpapers.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.noor.wallpapers.MainActivity
import com.noor.wallpapers.R
import com.noor.wallpapers.prayer.HolyDayEvent
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.PrayerEvent
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.ui.HanifeBetul

/** Prayer-time, reminder and religious-day notifications, in Turkish and for Hanife Betül. */
object Notifications {
    private const val CH_PRAYER = "vakit"
    private const val CH_REMINDER = "hatirlatma"
    private const val CH_DAYS = "dini_gunler"
    private const val ID_PRAYER = 100
    private const val ID_REMINDER = 200
    private const val ID_DAYS = 300

    private val GOLD = 0xFFD9B54A.toInt()

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PRAYER, "Vakit girdiğinde", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Her namaz vakti girdiğinde bir bildirim. Sesi buradan değiştirebilirsin."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Vakitten önce hatırlatma", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Vakit girmeden birkaç dakika önce haber verir."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DAYS, "Kandiller ve bayramlar", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Kandil gecelerinde ve bayram sabahlarında bir tebrik."
            },
        )
    }

    fun permitted(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_PRAYER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun post(context: Context, id: Int, channel: String, title: String, text: String, big: String? = null) {
        if (!permitted(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_noor)
            .setColor(GOLD)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big ?: text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (channel == CH_PRAYER) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and the post.
        }
    }

    /** The time has come. [ramadan] changes İmsak and Akşam into sahur and iftar. */
    fun prayer(context: Context, event: PrayerEvent, place: String, ramadan: Boolean) {
        val time = TurkishText.hhmm(event.time)
        val name = HanifeBetul.NAME
        val (title, text) = when (event.prayer) {
            Prayer.IMSAK -> if (ramadan) "İmsak vakti · sahur sona erdi" to "Allah orucunu kabul etsin, $name 🌙"
            else "Sabah namazının vakti girdi" to "Güne namazla başla, $name ☀️"
            Prayer.GUNES -> "Güneş doğdu" to "Sabah namazının vakti çıktı · $time"
            Prayer.OGLE -> if (event.day.date.dayOfWeek == java.time.DayOfWeek.FRIDAY) "Cuma vakti 🕌" to "Hayırlı Cumalar, $name"
            else "Öğle vakti girdi 🕌" to HanifeBetul.prayerNote(event.prayer)
            Prayer.IKINDI -> "İkindi vakti girdi" to HanifeBetul.prayerNote(event.prayer)
            Prayer.AKSAM -> if (ramadan) "İftar vakti! 🌙" to "Allah kabul etsin, $name. Hayırlı iftarlar."
            else "Akşam vakti girdi" to HanifeBetul.prayerNote(event.prayer)
            Prayer.YATSI -> "Yatsı vakti girdi ✨" to HanifeBetul.prayerNote(event.prayer)
        }
        post(context, ID_PRAYER, CH_PRAYER, title, "$text · $time · $place")
    }

    fun reminder(context: Context, event: PrayerEvent, minutes: Int) {
        val time = TurkishText.hhmm(event.time)
        val title = if (event.prayer == Prayer.GUNES) "Güneşin doğmasına $minutes dk" else "${event.prayer.title} vaktine $minutes dk"
        val text = if (event.prayer == Prayer.GUNES) "Sabah namazı için son dakikalar · $time"
        else "${event.prayer.title} vakti ${TurkishText.at(event.time)} giriyor"
        post(context, ID_REMINDER, CH_REMINDER, title, text)
    }

    fun holyDay(context: Context, e: HolyDayEvent) {
        val (title, text) = HanifeBetul.holyDayMessage(e)
        post(context, ID_DAYS, CH_DAYS, title, text)
    }

    fun birthday(context: Context) {
        val (title, text) = HanifeBetul.birthdayMessage()
        post(context, ID_DAYS + 1, CH_DAYS, title, text)
    }

    /** From settings, so she can hear what it sounds like. */
    fun test(context: Context) {
        post(context, ID_PRAYER, CH_PRAYER, "Bildirimler hazır ✨", "Vakit girince sana böyle haber vereceğim, ${HanifeBetul.NAME}.")
    }
}
