package com.noor.wallpapers.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.photos.Photo
import com.noor.wallpapers.photos.Photos
import com.noor.wallpapers.wallpaper.Prefs
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.NoorLiveWallpaperService
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

/**
 * Keeps one alarm set for the next change, like [PrayerAlarms] does for prayer
 * times. In live mode there is no alarm: the live wallpaper checks the slot
 * itself once a minute while it is on screen.
 */
object RotationAlarms {
    private const val REQUEST = 11
    private const val WORK = "wallpaper-rotation"

    private fun intent(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, RotationReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * What will actually rotate: designs still in the catalogue and real photos
     * still among the chosen ones, at most [RotationSchedule.MAX].
     */
    fun designs(context: Context): List<String> {
        val s = AppSettings(context)
        val visible = Catalog.visible(s.secretUnlocked).map { it.id }.toSet()
        return s.rotationIds.filter { it in visible || Photos.byId(context, it) != null }.take(RotationSchedule.MAX)
    }

    /** A design id as she customised it; null for a photo or an unknown id. */
    fun selectionFor(context: Context, id: String): Selection? =
        Prefs(context).customised(id) ?: Catalog.byId(id)?.let(Selection::of)

    fun active(context: Context) = AppSettings(context).rotationOn && designs(context).size >= 2

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = intent(context)
        am.cancel(pi)
        if (!active(context) || AppSettings(context).rotationLive) return
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

    /**
     * For the live wallpaper in live mode: what to show now, keyed by its
     * encoding so the key changes exactly when the picture should.
     */
    fun liveItem(context: Context): Pair<String, String>? {
        val s = AppSettings(context)
        if (!s.rotationOn || !s.rotationLive) return null
        val ids = designs(context)
        if (ids.size < 2) return null
        val slot = RotationSchedule.slot(ZonedDateTime.now(ZoneId.systemDefault()), s.rotationInterval)
        val id = RotationSchedule.pick(ids, slot) ?: return null
        if (Photo.isPhotoId(id)) return id to id
        val sel = selectionFor(context, id) ?: return null
        return sel.encode() to id
    }

    /** Whether HBSnoor's live wallpaper is the one on the screen now. */
    fun liveWallpaperSet(context: Context): Boolean = runCatching {
        WallpaperManager.getInstance(context).wallpaperInfo?.component ==
            ComponentName(context, NoorLiveWallpaperService::class.java)
    }.getOrDefault(false)

    /** Starts (or restarts with new choices) the rotation and shows its first design at once. */
    fun start(context: Context, ids: List<String>, interval: RotationInterval, target: Target, live: Boolean) {
        val s = AppSettings(context)
        s.rotationIds = ids.take(RotationSchedule.MAX)
        s.rotationInterval = interval
        s.rotationTarget = target
        s.rotationLive = live
        s.rotationOn = true
        // Two schedules fighting over the wallpaper would make neither predictable.
        s.dailyWallpaper = false
        if (live) s.liveFollowsPrayer = false
        Work.ensure(context)
        // Fetch the chosen photos now, while she is online, so later changes work offline.
        Background.executor.execute {
            for (id in ids) Photos.byId(context, id)?.let { p -> runCatching { Photos.wallpaperBitmap(context, p).recycle() } }
        }
        // A static image would replace the live wallpaper, so live mode draws nothing here.
        if (!live) applyNow(context, force = true)
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
        // In live mode the live wallpaper rotates itself; setting a bitmap would replace it.
        if (!RotationAlarms.active(ctx) || s.rotationLive) return Result.success()
        val slot = RotationSchedule.slot(ZonedDateTime.now(ZoneId.systemDefault()), s.rotationInterval)
        val key = "${s.rotationInterval.name}:$slot"
        if (s.rotationLastSlot == key && !inputData.getBoolean(FORCE, false)) return Result.success()
        val id = RotationSchedule.pick(RotationAlarms.designs(ctx), slot) ?: return Result.success()
        val photo = Photos.byId(ctx, id)
        val sel = RotationAlarms.selectionFor(ctx, id)
        if (photo == null && sel == null) return Result.success()
        return try {
            val bmp = if (photo != null) Photos.wallpaper(ctx, photo) else Wallpapers.renderWallpaper(ctx, sel ?: return Result.success())
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
