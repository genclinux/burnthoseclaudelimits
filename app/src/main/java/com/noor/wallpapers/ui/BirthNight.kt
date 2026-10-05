package com.noor.wallpapers.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.noor.wallpapers.BirthFacts
import com.noor.wallpapers.art.BirthNightArt
import com.noor.wallpapers.art.MoonPhase
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.service.PrayerRepository
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Easter egg: "Doğduğun gün". Tapping 12 September in Takvim (or opening the
 * app on her birthday) tells her about the day she was born and unlocks the
 * night sky of that evening as a wallpaper.
 */
@Composable
fun BirthNightSheet(onOpenWallpaper: () -> Unit, onMessage: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val birth = BirthNightArt.BIRTH_DATE
    val today = remember { LocalDate.now() }
    val facts = remember(today) { BirthFacts.of(birth, today) }
    val hijri = remember { PrayerRepository.hijri(context) }
    val hBirth = remember(hijri) { hijri.of(birth) }
    val hToday = remember(hijri, today) { hijri.of(today) }
    val ramadans = BirthFacts.ramadans(hBirth.year, hBirth.month, hToday.year, hToday.month)
    val moon = (MoonPhase.illumination(BirthNightArt.EVENING) * 100).roundToInt()
    LaunchedEffect(Unit) { HanifeBetul.find(context, HanifeBetul.Surprise.BIRTH_NIGHT)?.let(onMessage) }

    NoorSheet(
        title = "${birth.dayOfMonth} ${TurkishText.MONTHS[birth.monthValue - 1]} ${birth.year}, ${TurkishText.weekday(birth.dayOfWeek)}",
        subtitle = "Doğduğun gün · $hBirth",
        onDismiss = onDismiss,
    ) {
        Text(
            if (facts.daysToBirthday == 0L) "İyi ki doğdun, ${HanifeBetul.NAME} 🎂" else "Bu gün dünyaya geldin 🌙",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        NoorCard {
            Fact("🌕", "O gece ay %$moon doluydu", "Ertesi akşam dolunaydı; gökyüzü senin için aydınlıktı.")
            Fact("☀️", "Bugün dünyadaki ${"%,d".format(TurkishText.TR, facts.dayNumber)}. günün", "${facts.years} yıl önce, bir ${TurkishText.weekday(birth.dayOfWeek).lowercase(TurkishText.TR)} günü.")
            Fact("🕌", "${"%,d".format(TurkishText.TR, facts.fridays)} cuma", "O günden beri yaşadığın cumalar.")
            Fact("🌙", "$ramadans Ramazan", "Doğduğundan beri karşıladığın Ramazanlar.")
            Fact(
                "🎂",
                if (facts.daysToBirthday == 0L) "Bugün doğum günün" else "Doğum gününe ${facts.daysToBirthday} gün",
                "12 Eylül",
            )
        }
        Button(onClick = onOpenWallpaper, modifier = Modifier.fillMaxWidth()) { Text("Doğduğun gecenin gökyüzü") }
        Footnote("Bu sayfa gizliydi; o gecenin gökyüzü artık Galeri'de, Hanife Betül koleksiyonunda.")
    }
}

@Composable
private fun Fact(emoji: String, title: String, subtitle: String) {
    OptionRow(title, emoji = emoji, subtitle = subtitle)
}
