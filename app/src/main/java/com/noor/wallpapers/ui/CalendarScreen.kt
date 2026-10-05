package com.noor.wallpapers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.prayer.HijriCalendar
import com.noor.wallpapers.prayer.HolyDayEvent
import com.noor.wallpapers.prayer.ReligiousDays
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.PrayerRepository
import java.time.LocalDate
import java.time.MonthDay
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Takvim: today's Hijri date, a month with Hijri days beneath, and Diyanet's religious days for a year. */
@Composable
fun CalendarScreen(onOpenSettings: () -> Unit, onBirthday: () -> Unit = {}) {
    val context = LocalContext.current
    val v = rememberSettingsVersion()
    val hijri = remember(v) { PrayerRepository.hijri(context) }
    val today = LocalDate.now()
    val todayHijri = remember(today, hijri) { hijri.of(today) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val year = remember(today, hijri) { ReligiousDays.between(today, today.plusYears(1), hijri) }
    val amiri = rememberAmiri()
    val birthday = remember(v) { AppSettings(context).birthday }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        ScreenHeader("Takvim", subtitle = "Hicrî takvim ve dini günler", onSettings = onOpenSettings)
        Column(Modifier.widthIn(max = Noor.MaxWidth).padding(horizontal = Noor.Gutter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${todayHijri.day} ${todayHijri.monthName} ${todayHijri.year}",
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
            )
            Text(ARABIC_MONTHS[todayHijri.month - 1], fontFamily = amiri, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
            Text(TurkishText.longDate(today), color = MaterialTheme.colorScheme.onSurfaceVariant)

            year.firstOrNull()?.let { e ->
                Spacer(Modifier.height(16.dp))
                val days = ChronoUnit.DAYS.between(today, e.date)
                InfoCard(
                    "Sıradaki: ${e.day.title}",
                    "${TurkishText.longDate(e.date)}${if (e.day.night) " gecesi" else ""} · ${TurkishText.daysFromNow(days)}",
                )
            }

            Spacer(Modifier.height(16.dp))
            MonthGrid(
                month, today, hijri, year, birthday,
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                // Easter egg: her birthday opens "Doğduğun gün".
                onDay = { d -> if (birthday != null && MonthDay.from(d) == birthday) onBirthday() },
            )

            Spacer(Modifier.height(20.dp))
            SectionTitle("Dini günler", Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            NoorCard { for (e in year) HolyDayRow(e, today) }
            Spacer(Modifier.height(12.dp))
            Footnote(
                "Kandiller, adı geçen günün akşamından başlayan gecedir. Tarihler Hicri takvimden hesaplanır ve Diyanet'in " +
                    "vakit tablosundaki Hicri tarihle eşleştirilir; ay başlangıçlarında bir gün fark olabilir.",
            )
        }
    }
}

private val ARABIC_MONTHS = listOf(
    "مُحَرَّم", "صَفَر", "رَبِيع ٱلْأَوَّل", "رَبِيع ٱلْآخِر", "جُمَادَىٰ ٱلْأُولَىٰ", "جُمَادَىٰ ٱلْآخِرَة",
    "رَجَب", "شَعْبَان", "رَمَضَان", "شَوَّال", "ذُو ٱلْقَعْدَة", "ذُو ٱلْحِجَّة",
)

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    hijri: HijriCalendar,
    events: List<HolyDayEvent>,
    birthday: MonthDay?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1 // Monday first, as in Türkiye.
    val days = month.lengthOfMonth()
    val special = remember(month, events) {
        events.flatMap { e -> (0 until e.day.days).map { e.date.plusDays(it.toLong()) } }.toSet()
    }
    val hijriOfFirst = remember(month, hijri) { hijri.of(first) }
    val hijriOfLast = remember(month, hijri) { hijri.of(month.atEndOfMonth()) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Noor.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki ay") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${TurkishText.MONTHS[month.monthValue - 1]} ${month.year}", fontWeight = FontWeight.SemiBold)
                Text(
                    if (hijriOfFirst.month == hijriOfLast.month) "${hijriOfFirst.monthName} ${hijriOfFirst.year}"
                    else "${hijriOfFirst.monthName} – ${hijriOfLast.monthName} ${hijriOfLast.year}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki ay") }
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")) {
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val cells = lead + days
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val index = r * 7 + c - lead
                    Box(Modifier.weight(1f).aspectRatio(0.8f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (index in 0 until days) {
                            val date = month.atDay(index + 1)
                            val isToday = date == today
                            val isSpecial = date in special
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                                    .then(if (isSpecial) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                                    .clickable { onDay(date) },
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    lineHeight = 18.sp,
                                    color = if (date.dayOfWeek.value == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                // Her birthday carries a tiny heart instead of the Hijri day: the only hint.
                                val hers = birthday != null && MonthDay.from(date) == birthday
                                Text(
                                    if (hers) "♡" else "${hijri.of(date).day}",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    color = if (hers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolyDayRow(e: HolyDayEvent, today: LocalDate) {
    val days = ChronoUnit.DAYS.between(today, e.date)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(
                if (e.day.night) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            ),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.day.title + if (e.day.days > 1) " (${e.day.days} gün)" else "", fontWeight = FontWeight.Medium)
            Text(
                TurkishText.longDate(e.date) + if (e.day.night) " · gecesi" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(TurkishText.daysFromNow(days), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}
