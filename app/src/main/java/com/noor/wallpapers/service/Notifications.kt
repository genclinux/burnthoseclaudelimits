package com.noor.wallpapers.service

import android.Manifest
import android.annotation.SuppressLint
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

    // permitted() checks POST_NOTIFICATIONS first; lint can't see through it.
    @SuppressLint("MissingPermission")
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

    /**
     * The time has come. The collapsed notification is just the time and
     * place; expanded, it carries a verse or hadith about that prayer.
     * [ramadan] changes İmsak and Akşam into sahur and iftar.
     */
    fun prayer(context: Context, event: PrayerEvent, place: String, ramadan: Boolean) {
        val time = TurkishText.hhmm(event.time)
        val friday = event.prayer == Prayer.OGLE && event.day.date.dayOfWeek == java.time.DayOfWeek.FRIDAY
        val (title, line) = when (event.prayer) {
            Prayer.IMSAK -> if (ramadan) "İmsak vakti · sahur sona erdi" to "Allah orucunu kabul etsin." else "Sabah namazının vakti girdi" to null
            Prayer.GUNES -> "Güneş doğdu" to "Sabah namazının vakti çıktı."
            Prayer.OGLE -> if (friday) "Cuma vakti" to "Hayırlı Cumalar." else "Öğle vakti girdi" to null
            Prayer.IKINDI -> "İkindi vakti girdi" to null
            Prayer.AKSAM -> if (ramadan) "İftar vakti" to "Allah kabul etsin. Hayırlı iftarlar." else "Akşam vakti girdi" to null
            Prayer.YATSI -> "Yatsı vakti girdi" to null
        }
        val text = "$time · $place"
        val note = if (line == null) HanifeBetul.prayerNote(event.prayer) else null
        val big = when {
            line != null -> "$line\n$text"
            note != null -> "$text\n\n${note.text}\n— ${note.source}"
            else -> text
        }
        post(context, ID_PRAYER, CH_PRAYER, title, if (line != null) "$line · $text" else text, big)
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
