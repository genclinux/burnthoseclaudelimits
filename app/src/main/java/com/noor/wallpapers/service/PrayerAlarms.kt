package com.noor.wallpapers.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.noor.wallpapers.prayer.HijriDate
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.ReligiousDays
import com.noor.wallpapers.prayer.Triggers
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** One background thread for receivers' work (rendering the widget, reading the cache). */
internal object Background {
    val executor: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "noor-background") }

    /** Runs [work] off the main thread while keeping the broadcast alive. */
    fun run(receiver: BroadcastReceiver, work: () -> Unit) {
        val pending = receiver.goAsync()
        executor.execute {
            try {
                work()
            } catch (_: Exception) {
                // A failed widget render or reschedule must never crash the process from a receiver.
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Keeps one alarm set: the next prayer time or reminder. When it fires it
 * notifies (if asked to), redraws the widget and sets the following one, so
 * the chain carries on across days without any periodic polling.
 */
object PrayerAlarms {
    private const val REQUEST = 7

    /** Notifications older than this when the alarm finally runs (deep doze, reboot) are skipped. */
    private val STALE = Duration.ofMinutes(15)

    fun canExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun intent(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, PrayerAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Sets the alarm for the next trigger; call after anything that changes times or settings. */
    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = intent(context)
        am.cancel(pi)
        val schedule = PrayerRepository.schedule(context) ?: return
        val s = AppSettings(context)
        val next = Triggers.next(schedule, Instant.now(), s.reminderMinutes, s.notifyPrayers) ?: return
        val at = next.at.toEpochMilli()
        try {
            if (s.notificationsOn && canExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                return
            }
        } catch (_: SecurityException) {
            // The exact-alarm permission was withdrawn a moment ago; fall through to an inexact one.
        }
        // Without exact alarms (or with notifications off, when only the widget needs it) a little lateness is fine.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Runs on a background thread when the alarm fires. */
    internal fun handle(context: Context) {
        val s = AppSettings(context)
        val now = Instant.now()
        val schedule = PrayerRepository.schedule(context)
        if (schedule != null) {
            val since = s.lastTriggerAt.takeIf { it > 0 }?.let(Instant::ofEpochMilli) ?: now.minus(STALE)
            val due = Triggers.due(schedule, since, now.plusSeconds(1), s.reminderMinutes, s.notifyPrayers)
            val hijri = PrayerRepository.hijri(context)
            for (t in due) {
                if (Duration.between(t.at, now) > STALE) continue
                val p = t.event.prayer
                when (t.kind) {
                    Triggers.Kind.REMINDER ->
                        if (s.notificationsOn && p in s.notifyPrayers) Notifications.reminder(context, t.event, s.reminderMinutes)
                    Triggers.Kind.ENTRY -> {
                        val date = t.event.day.date
                        val ramadan = (t.event.day.hijri ?: hijri.of(date)).month == HijriDate.RAMAZAN
                        if (s.notificationsOn && p in s.notifyPrayers) {
                            Notifications.prayer(context, t.event, schedule.location.label, ramadan)
                        }
                        greetings(context, s, p, date, hijri)
                    }
                }
            }
            s.lastTriggerAt = now.toEpochMilli()
        }
        PrayerWidget.updateAll(context)
        reschedule(context)
    }

    /** Kandil nights begin at Akşam; Bayrams and other days are greeted at sunrise. Her birthday too. */
    private fun greetings(context: Context, s: AppSettings, p: Prayer, date: LocalDate, hijri: com.noor.wallpapers.prayer.HijriCalendar) {
        if (!s.notificationsOn || !s.holyDayNotifications) return
        val events = ReligiousDays.on(date, hijri).filter { it.date == date }
        when (p) {
            Prayer.AKSAM -> events.filter { it.day.night }.forEach { Notifications.holyDay(context, it) }
            Prayer.GUNES -> {
                events.filter { !it.day.night }.forEach { Notifications.holyDay(context, it) }
                if (s.birthday == MonthDay.from(date)) Notifications.birthday(context)
            }
            else -> Unit
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Background.run(this) { PrayerAlarms.handle(app) }
    }
}

/** Boot, app update, clock or time-zone change, exact-alarm permission: set everything up again. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Background.run(this) {
            PrayerAlarms.reschedule(app)
            RotationAlarms.reschedule(app)
            // The clock may have jumped into another slot.
            if (RotationAlarms.active(app)) RotationAlarms.applyNow(app)
            PrayerWidget.updateAll(app)
            Work.ensure(app)
        }
    }
}
