package com.noor.wallpapers.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.wallpaper.Prefs
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.Target
import com.noor.wallpapers.wallpaper.Wallpapers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

enum class RotationInterval(val minutes: Long, val title: String) {
    TEN_MINUTES(10, "10 dakikada bir"),
    HOUR(60, "Saatte bir"),
    DAY(24 * 60, "Günde bir"),
}

/**
 * When the rotation changes design, without any Android in it. Time is cut
 * into slots on the local clock (10:00, 10:10…; every hour; every midnight),
 * and slot n shows design n mod count. A missed alarm only delays the change:
 * whichever slot it is when the work runs decides what is shown.
 */
object RotationSchedule {
    const val MAX = 10

    private fun localMinutes(t: LocalDateTime) = t.toEpochSecond(ZoneOffset.UTC) / 60

    fun slot(now: ZonedDateTime, interval: RotationInterval): Long =
        Math.floorDiv(localMinutes(now.toLocalDateTime()), interval.minutes)

    /** Start of the next slot, as a wall-clock time in [now]'s zone. */
    fun nextChange(now: ZonedDateTime, interval: RotationInterval): ZonedDateTime {
        val minutes = (slot(now, interval) + 1) * interval.minutes
        val local = LocalDateTime.ofEpochSecond(minutes * 60, 0, ZoneOffset.UTC)
        // Inside a DST gap this moves forward to the first valid instant.
        return ZonedDateTime.of(local, now.zone)
    }

    fun pick(ids: List<String>, slot: Long): String? =
        if (ids.isEmpty()) null else ids[Math.floorMod(slot, ids.size.toLong()).toInt()]
}

/** Keeps one alarm set for the next change, like [PrayerAlarms] does for prayer times. */
object RotationAlarms {
    private const val REQUEST = 11
    private const val WORK = "wallpaper-rotation"

    private fun intent(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, RotationReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The designs that will actually rotate: still in the catalogue, at most [RotationSchedule.MAX]. */
    fun designs(context: Context): List<String> {
        val s = AppSettings(context)
        val visible = Catalog.visible(s.secretUnlocked).map { it.id }.toSet()
        return s.rotationIds.filter { it in visible }.take(RotationSchedule.MAX)
    }

    fun active(context: Context) = AppSettings(context).rotationOn && designs(context).size >= 2

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = intent(context)
        am.cancel(pi)
        if (!active(context)) return
        val at = RotationSchedule.nextChange(ZonedDateTime.now(ZoneId.systemDefault()), AppSettings(context).rotationInterval)
            .toInstant().toEpochMilli()
        try {
            if (PrayerAlarms.canExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                return
            }
        } catch (_: SecurityException) {
            // Permission withdrawn a moment ago; an inexact alarm is fine for wallpapers.
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Render the design for this slot. [force] re-applies it even if it is already showing. */
    fun applyNow(context: Context, force: Boolean = false) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK, if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RotationWorker>().setInputData(workDataOf(RotationWorker.FORCE to force)).build(),
        )
    }

    /** Starts (or restarts with new choices) the rotation and shows its first design at once. */
    fun start(context: Context, ids: List<String>, interval: RotationInterval, target: Target) {
        val s = AppSettings(context)
        s.rotationIds = ids.take(RotationSchedule.MAX)
        s.rotationInterval = interval
        s.rotationTarget = target
        s.rotationOn = true
        // Two schedules fighting over the wallpaper would make neither predictable.
        s.dailyWallpaper = false
        Work.ensure(context)
        applyNow(context, force = true)
        Background.executor.execute { reschedule(context) }
    }

    fun stop(context: Context) {
        AppSettings(context).rotationOn = false
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
        Work.ensure(context)
        Background.executor.execute { reschedule(context) }
    }
}

class RotationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Background.run(this) {
            RotationAlarms.applyNow(app)
            RotationAlarms.reschedule(app)
        }
    }
}

/** Puts the design for the current slot on the screen, as she customised it. */
class RotationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val s = AppSettings(ctx)
        if (!RotationAlarms.active(ctx)) return Result.success()
        val slot = RotationSchedule.slot(ZonedDateTime.now(ZoneId.systemDefault()), s.rotationInterval)
        val key = "${s.rotationInterval.name}:$slot"
        if (s.rotationLastSlot == key && !inputData.getBoolean(FORCE, false)) return Result.success()
        val id = RotationSchedule.pick(RotationAlarms.designs(ctx), slot) ?: return Result.success()
        val entry = Catalog.byId(id) ?: return Result.success()
        val sel = Prefs(ctx).customised(id) ?: Selection.of(entry)
        return try {
            val bmp = Wallpapers.renderWallpaper(ctx, sel)
            Wallpapers.apply(ctx, bmp, s.rotationTarget)
            bmp.recycle()
            s.rotationLastSlot = key
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        } finally {
            // Keeps the chain going even if an alarm was lost.
            RotationAlarms.reschedule(ctx)
        }
    }

    companion object {
        const val FORCE = "force"
    }
}
