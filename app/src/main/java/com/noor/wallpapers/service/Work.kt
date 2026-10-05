package com.noor.wallpapers.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.Category
import com.noor.wallpapers.wallpaper.Prefs
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.Target
import com.noor.wallpapers.wallpaper.Wallpapers
import java.time.LocalDate
import java.time.MonthDay
import java.util.concurrent.TimeUnit

/** Background jobs: keeping Diyanet's table fresh, and the wallpaper of the day. */
object Work {
    private const val REFRESH = "prayer-refresh"
    private const val REFRESH_NOW = "prayer-refresh-now"
    private const val DAILY = "daily-wallpaper"
    private const val DAILY_NOW = "daily-wallpaper-now"

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Idempotent: call on app start, after boot and after settings change. */
    fun ensure(context: Context) {
        val wm = WorkManager.getInstance(context)
        val s = AppSettings(context)
        if (s.location != null) {
            wm.enqueueUniquePeriodicWork(
                REFRESH, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PrayerRefreshWorker>(12, TimeUnit.HOURS).setConstraints(online).build(),
            )
        }
        if (s.dailyWallpaper) {
            wm.enqueueUniquePeriodicWork(
                DAILY, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyWallpaperWorker>(4, TimeUnit.HOURS).build(),
            )
        } else {
            wm.cancelUniqueWork(DAILY)
        }
    }

    /** Fetch now (when online), e.g. right after a place is chosen. */
    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_NOW, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PrayerRefreshWorker>().setConstraints(online).build(),
        )
    }

    /** Change the wallpaper now, e.g. when the daily wallpaper is switched on. */
    fun dailyNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_NOW, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyWallpaperWorker>().setInputData(workDataOf(DailyWallpaperWorker.FORCE to true)).build(),
        )
    }
}

class PrayerRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (AppSettings(ctx).location == null) return Result.success()
        return try {
            PrayerRepository.refresh(ctx)
            PrayerAlarms.reschedule(ctx)
            PrayerWidget.updateAll(ctx)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

/**
 * A new wallpaper every day from favourites, her collection or everything,
 * each with a fresh seed. On her birthday it is always the birthday design.
 */
class DailyWallpaperWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val s = AppSettings(ctx)
        if (!s.dailyWallpaper) return Result.success()
        val today = LocalDate.now()
        if (s.dailyLastDate == today.toString() && !inputData.getBoolean(FORCE, false)) return Result.success()
        val sel = pick(ctx, s, today) ?: return Result.success()
        return try {
            val bmp = Wallpapers.renderWallpaper(ctx, sel)
            val target = Target.entries.firstOrNull { it.name == s.dailyTarget } ?: Target.BOTH
            Wallpapers.apply(ctx, bmp, target)
            bmp.recycle()
            s.dailyLastDate = today.toString()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun pick(ctx: Context, s: AppSettings, today: LocalDate): Selection? {
        val prefs = Prefs(ctx)
        if (s.birthday == MonthDay.from(today)) Catalog.byId(Catalog.BIRTHDAY)?.let { return Selection.of(it) }
        val visible = Catalog.visible(s.secretUnlocked)
        val pool = when (s.dailySource) {
            DailySource.FAVORITES -> visible.filter { it.id in prefs.favorites }
                .ifEmpty { visible.filter { it.category == Category.HANIFE_BETUL } }
            DailySource.HANIFE_BETUL -> visible.filter { it.category == Category.HANIFE_BETUL }
            DailySource.ALL -> visible
        }.filter { it.id != Catalog.BIRTHDAY }
        if (pool.isEmpty()) return null
        val entry = pool[Math.floorMod(today.toEpochDay(), pool.size.toLong()).toInt()]
        val base = prefs.customised(entry.id) ?: Selection.of(entry)
        // A new arrangement each day: moon, lanterns, marbling all move.
        return base.copy(seed = (Math.floorMod(today.toEpochDay() * 7919, 99_991L) + 1).toInt())
    }

    companion object {
        const val FORCE = "force"
    }
}
