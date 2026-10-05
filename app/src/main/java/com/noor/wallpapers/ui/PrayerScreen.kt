package com.noor.wallpapers.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.prayer.DayTimes
import com.noor.wallpapers.prayer.HolyDayEvent
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.PrayerSchedule
import com.noor.wallpapers.prayer.ReligiousDays
import com.noor.wallpapers.prayer.Source
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.PrayerAlarms
import com.noor.wallpapers.service.PrayerRepository
import com.noor.wallpapers.service.PrayerWidget
import com.noor.wallpapers.wallpaper.TimeOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Tabular figures, so ticking digits don't jiggle. */
const val Tabular = "tnum"

/**
 * Vakitler: Diyanet's times for her place, the next one counting down, and a
 * backdrop that follows the time of day (dawn, noon, dusk, night).
 */
@Composable
fun PrayerScreen(
    onPickLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val schedule = rememberSchedule()
    val now = rememberNow()
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var monthly by remember { mutableStateOf(false) }

    fun refresh(force: Boolean) {
        if (refreshing) return
        scope.launch {
            refreshing = true
            try {
                val days = PrayerRepository.refresh(context, force)
                withContext(Dispatchers.Default) {
                    PrayerAlarms.reschedule(context)
                    PrayerWidget.updateAll(context)
                }
                if (force) onMessage(if (days > 0) "Diyanet'ten $days günlük vakit alındı ✓" else "Vakitler güncel")
            } catch (e: Exception) {
                if (force) onMessage("Diyanet'e ulaşılamadı: ${e.message ?: "bağlantı yok"}. Hesaplanan vakitler gösteriliyor.")
            } finally {
                refreshing = false
            }
        }
    }

    // Fetch on open when the table is missing or running low.
    LaunchedEffect(schedule?.location) {
        if (schedule != null && PrayerRepository.needsRefresh(context)) refresh(force = false)
    }

    val currentEvent = schedule?.current(now)
    val current = currentEvent?.prayer
    // A kandil night is dated by the evening it began, even after midnight.
    val periodDate = currentEvent?.day?.date
    val kandil = remember(periodDate) { periodDate != null && PrayerRepository.isKandilEvening(context, periodDate) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Backdrop(current, kandil)
        if (schedule == null) {
            NoLocation(onPickLocation)
        } else {
            Content(schedule, now, refreshing, settings, onPickLocation, onOpenSettings, { refresh(true) }, { monthly = true }, onMessage)
        }
    }
    if (monthly && schedule != null) MonthlySheet(schedule, now) { monthly = false }
}

/** The design for this part of the day, softly behind everything, fading as it changes. */
@Composable
private fun Backdrop(current: Prayer?, kandil: Boolean) {
    val context = LocalContext.current
    val viewport = rememberViewport()
    val sel = remember(current, kandil) { TimeOfDay.selection(current, kandil) }
    val size = IntSize(viewport.width / 3, viewport.height / 3)
    val bmp by produceState<ImageBitmap?>(Thumbnails.cached(sel, size)?.asImageBitmap(), sel, size) {
        value = Thumbnails.load(context, sel, size).asImageBitmap()
    }
    AnimatedContent(bmp, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "backdrop") { b ->
        if (b != null) Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.35f),
                0.35f to Color.Black.copy(alpha = 0.55f),
                1f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            ),
        ),
    )
}

@Composable
private fun NoLocation(onPickLocation: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
    ) {
        Text("Namaz Vakitleri", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(
            "Diyanet İşleri Başkanlığı'nın resmî vakitleri için konumunu seç, ${HanifeBetul.NAME}. " +
                "İnternet yokken de vakitler Diyanet'in yöntemiyle hesaplanır.",
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.9f),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPickLocation) {
            Icon(Icons.Filled.LocationOn, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Konumumu seç")
        }
    }
}

@Composable
private fun Content(
    schedule: PrayerSchedule,
    now: Instant,
    refreshing: Boolean,
    settings: AppSettings,
    onPickLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onMonthly: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val todayDate = schedule.today(now)
    val today = schedule.day(todayDate)
    val next = schedule.next(now)
    val current = schedule.current(now)
    val hijriCal = remember(schedule) { PrayerRepository.hijri(context) }
    val hijri = today?.hijri ?: remember(todayDate, hijriCal) { hijriCal.of(todayDate) }
    val greeting = remember(todayDate, now.atZone(ZoneId.systemDefault()).hour) {
        HanifeBetul.greeting(LocalDateTime.now(), settings.birthday, hijriCal)
    }
    val upcoming = remember(todayDate, hijriCal) { ReligiousDays.between(todayDate, todayDate.plusDays(30), hijriCal) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Column(Modifier.widthIn(max = 560.dp)) {
            // Place and settings.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onPickLocation)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(schedule.location.label, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("  ▾", color = Color.White.copy(alpha = 0.6f))
                }
                if (refreshing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Yenile", tint = Color.White) }
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Ayarlar", tint = Color.White) }
            }
            Text(greeting, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(28.dp))
            // The next time, counting down.
            if (next != null) {
                val left = Duration.between(now, next.instant)
                Text("Sıradaki vakit", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    next.prayer.title,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    TurkishText.clock(left),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = Tabular),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "${TurkishText.hhmm(next.time)} · ${TurkishText.countdown(left)} kaldı",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "${TurkishText.longDate(todayDate)}  ·  $hijri",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            if (today != null) TimesCard(today, current?.prayer.takeIf { current?.day?.date == todayDate }, now)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = onMonthly, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("İmsakiye")
                }
                if (!settings.notificationsOn) {
                    FilledTonalButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Bildirimler")
                    }
                }
            }

            today?.qiblaTime?.let { q ->
                Spacer(Modifier.height(12.dp))
                InfoCard(
                    "Kıble saati ${TurkishText.hhmm(q)}",
                    "Bu saatte güneş tam kıble yönünde: güneşe dönen kıbleye dönmüş olur.",
                )
            }

            upcoming.firstOrNull()?.let { e ->
                Spacer(Modifier.height(12.dp))
                HolyDayCard(e, todayDate)
            }

            Spacer(Modifier.height(12.dp))
            NoteCard(onMessage)

            Spacer(Modifier.height(16.dp))
            SourceLine(schedule, today, settings)
        }
    }
}

@Composable
private fun TimesCard(day: DayTimes, current: Prayer?, now: Instant) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp),
    ) {
        for (p in Prayer.entries) {
            val lit = p == current
            val past = !lit && day.instant(p).isBefore(now)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (lit) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                Text(
                    p.title,
                    modifier = Modifier.weight(1f),
                    color = when {
                        lit -> MaterialTheme.colorScheme.primary
                        past -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White
                    },
                    fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 18.sp,
                )
                if (lit) Text("şimdi  ", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(
                    TurkishText.hhmm(day.time(p)),
                    color = if (lit) MaterialTheme.colorScheme.primary else if (past) Color.White.copy(alpha = 0.5f) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = Tabular),
                )
            }
        }
    }
}

@Composable
fun InfoCard(title: String, text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HolyDayCard(e: HolyDayEvent, today: LocalDate) {
    val days = ChronoUnit.DAYS.between(today, e.date)
    val title = when {
        days == 0L && e.day.night -> "Bu gece ${e.day.title} 🌙"
        days == 0L -> "Bugün ${e.day.title} ✨"
        days == 1L && e.day.night -> "Yarın gece ${e.day.title} 🌙"
        else -> "${e.day.title} · ${TurkishText.daysFromNow(days)}"
    }
    InfoCard(
        title,
        "${TurkishText.longDate(e.date)}${if (e.day.night) " (gecesi)" else ""}",
    )
}

@Composable
private fun NoteCard(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val note = remember { HanifeBetul.noteOfTheDay() }
    InfoCard(
        "Günün notu ✦",
        note.text + (note.source?.let { "\n— $it" } ?: ""),
        onClick = { HanifeBetul.find(context, HanifeBetul.Surprise.NOTE)?.let(onMessage) },
    )
}

@Composable
private fun SourceLine(schedule: PrayerSchedule, today: DayTimes?, settings: AppSettings) {
    val text = when {
        today?.source == Source.DIYANET -> {
            val updated = settings.fetchedAt.takeIf { it > 0 }?.let {
                TurkishText.dayMonth(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
            }
            "Kaynak: Diyanet İşleri Başkanlığı" + (updated?.let { " · $it güncellendi" } ?: "") +
                (schedule.diyanetUntil?.let { " · ${TurkishText.dayMonth(it)} tarihine kadar" } ?: "")
        }
        schedule.location.hasDiyanet ->
            "Diyanet'in tablosu henüz alınamadı; vakitler Diyanet yöntemiyle hesaplandı. İnternete bağlanınca güncellenir."
        else -> "Vakitler Diyanet yöntemiyle hesaplandı (İmsak 18°, Yatsı 17°, temkinli)."
    }
    Text(text, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** İmsakiye: every day Diyanet has published (or a month calculated), today first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlySheet(schedule: PrayerSchedule, now: Instant, onDismiss: () -> Unit) {
    val today = schedule.today(now)
    val until = schedule.diyanetUntil?.takeIf { !it.isBefore(today) } ?: today.plusDays(29)
    val days = remember(schedule, today) {
        generateSequence(today) { it.plusDays(1) }.takeWhile { !it.isAfter(until) }.mapNotNull { schedule.day(it) }.toList()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text(
            "İmsakiye · ${schedule.location.label}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("", Modifier.weight(1.5f))
            for (p in Prayer.entries) {
                Text(p.title, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
            }
        }
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            items(days, key = { it.date.toEpochDay() }) { d ->
                val isToday = d.date == today
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                ) {
                    Column(Modifier.weight(1.5f)) {
                        Text(
                            "${d.date.dayOfMonth} ${TurkishText.MONTHS[d.date.monthValue - 1].take(3)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            TurkishText.weekday(d.date.dayOfWeek).take(3) + (d.hijri?.let { " · ${it.day} ${it.monthName.take(3)}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    for (p in Prayer.entries) {
                        Text(
                            TurkishText.hhmm(d.time(p)),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = Tabular),
                            textAlign = TextAlign.Center,
                            color = if (d.source == Source.DIYANET) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
