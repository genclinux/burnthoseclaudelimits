package com.noor.wallpapers.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.WhatsNew
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.RotationInterval
import com.noor.wallpapers.service.RotationSchedule
import com.noor.wallpapers.wallpaper.Target

/** Bottom of the gallery while choosing for Döngü: how often, where, and start. */
@Composable
fun RotationBar(count: Int, running: Boolean, onStart: (RotationInterval, Target, Boolean) -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val s = remember { AppSettings(context) }
    var interval by remember { mutableStateOf(s.rotationInterval) }
    // Where: a Target name, or LIVE for the live wallpaper (which moves through them itself).
    var where by remember { mutableStateOf(if (s.rotationLive) LIVE else s.rotationTarget.name) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Noor.Gutter, vertical = 14.dp),
        ) {
            FieldLabel("Ne sıklıkla")
            ChoiceChips(RotationInterval.entries.map { it to it.title }, interval) { interval = it }
            FieldLabel("Nerede")
            ChoiceChips(
                listOf(Target.HOME.name to "Ana ekran", Target.LOCK.name to "Kilit ekranı", Target.BOTH.name to "İkisi de", LIVE to "Canlı duvar kağıdı ✨"),
                where,
            ) { where = it }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when {
                        count < 2 -> "En az 2, en fazla ${RotationSchedule.MAX} tane seç"
                        where == LIVE -> "$count tane, yumuşak geçişle değişecek"
                        else -> "$count tane sırayla değişecek"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (running) TextButton(onClick = onStop) { Text("Durdur") }
                Button(enabled = count >= 2, onClick = {
                    val live = where == LIVE
                    onStart(interval, if (live) s.rotationTarget else Target.valueOf(where), live)
                }) { Text(if (running) "Güncelle" else "Başlat") }
            }
        }
    }
}

private const val LIVE = "LIVE"

/**
 * "Yeni": the features she hasn't seen, one page each, newest first. A page
 * can offer to try its feature at once.
 */
@Composable
fun WhatsNewDialog(features: List<WhatsNew.Feature>, onAction: (WhatsNew.Action) -> Unit, onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val f = features[page]
    val last = page == features.lastIndex
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            // The emoji pops in for each page.
            AnimatedContent(f.emoji, transitionSpec = { scaleIn(Motion.spring()) + fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade()) }, label = "emoji") { e ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                ) { Text(e, fontSize = 30.sp) }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (features.size > 1) "Yeni · ${page + 1}/${features.size}" else "Yeni",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(f.title, textAlign = TextAlign.Center)
            }
        },
        // Pages slide in from the right, like turning a page.
        text = {
            AnimatedContent(
                page,
                transitionSpec = {
                    (slideInHorizontally(Motion.spring()) { it / 3 } + fadeIn(Motion.fade())) togetherWith
                        (slideOutHorizontally(Motion.spring()) { -it / 3 } + fadeOut(Motion.fade())) using SizeTransform(clip = false)
                },
                label = "page",
            ) { p -> Text(features[p].body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        },
        confirmButton = {
            val action = f.action
            if (action != null) {
                Button(onClick = { onDone(); onAction(action) }) { Text(action.label) }
            } else {
                Button(onClick = { if (last) onDone() else page++ }) { Text(if (last) "Tamam" else "Sonraki") }
            }
        },
        dismissButton = {
            // With an action on the page, the other way on is still there.
            if (f.action != null) {
                TextButton(onClick = { if (last) onDone() else page++ }) { Text(if (last) "Kapat" else "Sonraki") }
            } else if (!last) {
                TextButton(onClick = onDone) { Text("Kapat") }
            }
        },
    )
}
