package com.noor.wallpapers.ui

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Everything personal in the app. Nur was made for Hanife Betül, and these
 * easter eggs are meant to be found. The art has its own: an "HB" constellation
 * in every night sky, a tiny H·B star at the foot of every wallpaper, her own
 * collection and palette, and a shooting star in the live wallpaper.
 */
object HanifeBetul {
    const val NAME = "Hanife Betül"

    /** Greeting for the gallery header: holy days first, then Friday, then the time of day. */
    fun greeting(now: LocalDateTime = LocalDateTime.now()): String {
        holyDayGreeting()?.let { return it }
        if (now.dayOfWeek == DayOfWeek.FRIDAY) return "Hayırlı Cumalar, $NAME 🕌"
        return when (now.hour) {
            in 5..10 -> "Hayırlı sabahlar, $NAME ☀️"
            in 11..16 -> "İyi günler, $NAME 🌿"
            in 17..21 -> "Hayırlı akşamlar, $NAME 🌙"
            else -> "Hayırlı geceler, $NAME ✨"
        }
    }

    private fun holyDayGreeting(): String? = try {
        val cal = IslamicCalendar().apply { calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA }
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        when {
            month == IslamicCalendar.RAMADAN -> "Hayırlı Ramazanlar, $NAME 🏮"
            month == IslamicCalendar.SHAWWAL && day <= 3 -> "Bayramın mübarek olsun, $NAME 🌙"
            month == IslamicCalendar.DHU_AL_HIJJAH && day in 10..13 -> "Kurban Bayramın mübarek olsun, $NAME ✨"
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    /** Shown after a wallpaper is set; one picked at random each time. */
    val appliedMessages = listOf(
        "Hayırlı olsun, $NAME ✨",
        "Ekranın artık nur gibi 🌙",
        "Maşallah, çok yakıştı!",
        "Gözün aydın, yeni duvar kağıdın hazır 💚",
        "Bu ekran sana yakışır, $NAME 🌸",
    )

    /** Every seventh shuffle. */
    const val SHUFFLE_MESSAGE = "Maşallah! Bu tam sana göre, $NAME ✨"

    const val EMPTY_FAVOURITES =
        "Henüz favorin yok, $NAME. Beğendiğin bir duvar kağıdındaki kalbe dokun, burada seni beklesin. 💚"
}

@Composable
fun WelcomeDialog(onOpenCollection: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Hoş geldin, ${HanifeBetul.NAME} 🌙",
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Text(
                "Nur senin için yapıldı. İçinde sana özel bir koleksiyon, senin adını taşıyan bir renk " +
                    "paleti ve uygulamanın her köşesine saklanmış (pek de gizli olmayan) sürprizler var. " +
                    "Bakalım hepsini bulabilecek misin? ✨",
            )
        },
        confirmButton = { Button(onClick = onOpenCollection) { Text("Koleksiyonumu göster") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Sonra") } },
    )
}

/** Opened from the ✦ button or by tapping the "Nur" title five times. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicationSheet(onOpenCollection: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("حَنِيفَة بَتُول", fontSize = 40.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "Bu uygulama ${HanifeBetul.NAME} için yapıldı.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            NameMeaning(
                "Hanife · حنيفة",
                "Hanîf: Hakk'a yönelen, dosdoğru inanan. Kur'an'da Hz. İbrahim böyle anılır.",
                "فَأَقِمْ وَجْهَكَ لِلدِّينِ حَنِيفًا",
                "Yüzünü hanîf olarak dine çevir. (Rûm 30:30)",
            )
            NameMeaning(
                "Betül · بتول",
                "Kendini bütünüyle Allah'a adayan. Hz. Meryem ve Hz. Fatıma'nın lakabıdır.",
                "وَتَبَتَّلْ إِلَيْهِ تَبْتِيلًا",
                "Bütün benliğinle O'na yönel. (Müzzemmil 73:8)",
            )
            Text(
                "İpuçları: her duvar kağıdının altındaki minik H·B yıldızına, gece göklerindeki " +
                    "takımyıldızına ve canlı duvar kağıdında kayan yıldıza bak. Bir de dilek tut. 🌠",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onOpenCollection) { Text("${HanifeBetul.NAME} koleksiyonu ♡") }
        }
    }
}

@Composable
private fun NameMeaning(title: String, meaning: String, verse: String, verseMeaning: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(meaning, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Text(verse, fontSize = 24.sp, textAlign = TextAlign.Center)
        Text(
            verseMeaning,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
