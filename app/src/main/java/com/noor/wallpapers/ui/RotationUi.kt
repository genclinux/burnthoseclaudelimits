package com.noor.wallpapers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.RotationInterval
import com.noor.wallpapers.service.RotationSchedule
import com.noor.wallpapers.wallpaper.Target

/** Bottom of the gallery while choosing: how often, where, and start. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RotationBar(count: Int, running: Boolean, onStart: (RotationInterval, Target) -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val s = remember { AppSettings(context) }
    var interval by remember { mutableStateOf(s.rotationInterval) }
    var target by remember { mutableStateOf(s.rotationTarget) }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in RotationInterval.entries) {
                    FilterChip(selected = interval == i, onClick = { interval = i }, label = { Text(i.title) }, colors = chipColors)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((t, label) in listOf(Target.HOME to "Ana ekran", Target.LOCK to "Kilit ekranı", Target.BOTH to "İkisi de")) {
                    FilterChip(selected = target == t, onClick = { target = t }, label = { Text(label) }, colors = chipColors)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (count < 2) "En az 2, en fazla ${RotationSchedule.MAX} tasarım seç" else "$count tasarım sırayla değişecek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (running) TextButton(onClick = onStop) { Text("Durdur") }
                Button(enabled = count >= 2, onClick = { onStart(interval, target) }) { Text(if (running) "Güncelle" else "Başlat") }
            }
        }
    }
}

/** Shown once, the first time the app opens with the rotation in it. */
@Composable
fun RotationIntroDialog(onTry: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni: Duvar kağıdı döngüsü 🔄", color = MaterialTheme.colorScheme.primary) },
        text = {
            Text(
                "Artık koleksiyondan ${RotationSchedule.MAX} taneye kadar duvar kağıdı seçebilirsin; HBSnoor onları " +
                    "sırayla değiştirir: 10 dakikada bir, saatte bir ya da günde bir. Ana ekran, kilit ekranı ya da ikisi " +
                    "için. Galeri'nin üstündeki ↻ düğmesine dokunup seçmeye başla. ✨",
            )
        },
        confirmButton = { Button(onClick = onTry) { Text("Seçmeye başla") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Sonra") } },
    )
}
