package com.noor.wallpapers.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.art.Phrases
import com.noor.wallpapers.service.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A dhikr: its Arabic, how it is read, what it means, and how many make a round (0 = free count). */
class Dhikr(val id: String, val title: String, val arabic: String, val reading: String, val meaning: String, val target: Int)

object Dhikrs {
    val SUBHANALLAH = Dhikr("subhanallah", "Sübhânallah", Phrases.SUBHANALLAH.arabic, "Sübhânallah", Phrases.SUBHANALLAH.meaning, 33)
    val ELHAMDULILLAH = Dhikr("elhamdulillah", "Elhamdülillah", Phrases.ALHAMDULILLAH.arabic, "Elhamdülillah", Phrases.ALHAMDULILLAH.meaning, 33)
    val ALLAHUEKBER = Dhikr("allahuekber", "Allâhu ekber", Phrases.ALLAHU_AKBAR.arabic, "Allâhu ekber", Phrases.ALLAHU_AKBAR.meaning, 33)

    /** The three after every prayer, 33 each, then the closing tevhid. */
    val TESBIHAT = listOf(SUBHANALLAH, ELHAMDULILLAH, ALLAHUEKBER)

    const val CLOSING_ARABIC = "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ ٱلْمُلْكُ وَلَهُ ٱلْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ"
    const val CLOSING_READING =
        "Lâ ilâhe illallâhu vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr."

    val ALL = listOf(
        Dhikr("tesbihat", "Tesbihat", "", "", "Namazdan sonra: 33 Sübhânallah, 33 Elhamdülillah, 33 Allâhu ekber", 33),
        SUBHANALLAH, ELHAMDULILLAH, ALLAHUEKBER,
        Dhikr("tevhid", "Kelime-i Tevhid", Phrases.TAWHID.arabic, "Lâ ilâhe illallah", Phrases.TAWHID.meaning, 100),
        Dhikr("istigfar", "Estağfirullah", "أَسْتَغْفِرُ ٱللَّٰهَ", "Estağfirullah", "Allah'tan bağışlanma dilerim", 100),
        Dhikr("salavat", "Salavat", "ٱللَّٰهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Allâhümme salli alâ Muhammed", "Allah'ım, Muhammed'e salât eyle", 100),
        Dhikr("hasbunallah", "Hasbünallah", Phrases.HASBUNALLAH.arabic, "Hasbünallâhu ve ni'mel vekîl", "Allah bize yeter, O ne güzel vekildir", 100),
        Dhikr("serbest", "Serbest", "", "Serbest sayım", "Hedefsiz; istediğin kadar say", 0),
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

/**
 * Zikirmatik: tap anywhere on the beads (or press a volume key) to count. A
 * light tick each time, a stronger one at the end of a round. Counts are kept.
 */
@Composable
fun TesbihScreen(onMessage: (String) -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val s = remember { AppSettings(context) }
    val amiri = rememberAmiri()
    var preset by remember { mutableStateOf(Dhikrs.byId(s.tesbihPreset)) }
    var count by remember { mutableIntStateOf(s.tesbihCount) }
    var step by remember { mutableIntStateOf(s.tesbihStep) }
    var total by remember { mutableLongStateOf(s.tesbihTotal) }
    var haptics by remember { mutableStateOf(s.tesbihHaptics) }
    var confirmReset by remember { mutableStateOf(false) }
    val tesbihat = preset.id == "tesbihat"
    val done = tesbihat && step >= Dhikrs.TESBIHAT.size
    val current = if (tesbihat) Dhikrs.TESBIHAT.getOrNull(step) else preset

    fun save() {
        s.tesbihCount = count
        s.tesbihStep = step
        s.tesbihTotal = total
    }

    fun tick(strong: Boolean) {
        if (!haptics) return
        view.performHapticFeedback(
            if (!strong) HapticFeedbackConstants.KEYBOARD_TAP
            else if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
        )
    }

    fun increment() {
        if (done) return
        count++
        total++
        val target = current?.target ?: 0
        val roundDone = target > 0 && count % target == 0
        tick(roundDone)
        if (tesbihat && count >= 33) {
            step++
            count = 0
            if (step >= Dhikrs.TESBIHAT.size) {
                onMessage("Tesbihat tamam. Allah kabul etsin, ${HanifeBetul.NAME} ✨")
                HanifeBetul.find(context, HanifeBetul.Surprise.TESBIHAT)?.let(onMessage)
            }
        } else {
            HanifeBetul.tesbihMilestone(count)?.let(onMessage)
            if (count == HanifeBetul.SECRET_COUNT && !s.secretUnlocked) {
                s.secretUnlocked = true
                HanifeBetul.find(context, HanifeBetul.Surprise.SECRET)?.let(onMessage)
            }
        }
        save()
    }

    fun reset() {
        count = 0
        step = 0
        save()
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focus)
            .focusable()
            // Volume keys count too, so she can keep her eyes closed.
            .onPreviewKeyEvent { e ->
                if (e.key == Key.VolumeUp || e.key == Key.VolumeDown) {
                    if (e.type == KeyEventType.KeyDown) increment()
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
        ) {
            ScreenHeader("Zikir", subtitle = "Dokun ya da ses tuşuna bas", onSettings = onOpenSettings)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = Noor.Gutter)) {
                items(Dhikrs.ALL, key = { it.id }) { d ->
                    NoorChip(d.title, d.id == preset.id) {
                        if (d.id != preset.id) {
                            preset = d
                            s.tesbihPreset = d.id
                            reset()
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (done) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                ) {
                    Text("Tesbihat tamam ✓", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Son olarak:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Dhikrs.CLOSING_ARABIC, fontFamily = amiri, fontSize = 24.sp, textAlign = TextAlign.Center, lineHeight = 40.sp)
                    Text(Dhikrs.CLOSING_READING, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Allah'tan başka ilah yoktur. O tektir, ortağı yoktur. Mülk O'nundur, hamd O'na mahsustur. O her şeye kadirdir.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = ::reset) { Text("Yeniden başla") }
                }
            } else if (current != null) {
                if (current.arabic.isNotEmpty()) {
                    Text(current.arabic, fontFamily = amiri, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                }
                Text(current.reading, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    current.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::increment),
                ) {
                    Beads(count, current.target, Modifier.fillMaxWidth().aspectRatio(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$count", fontSize = 72.sp, fontWeight = FontWeight.Light, style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = Tabular))
                        val sub = when {
                            tesbihat -> "${step + 1}. / 3 · 33"
                            current.target > 0 -> "${count / current.target}. tur · hedef ${current.target}"
                            else -> "dokun ve say"
                        }
                        Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Toplam ${"%,d".format(com.noor.wallpapers.prayer.TurkishText.TR, total)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { haptics = !haptics; s.tesbihHaptics = haptics }) { Text(if (haptics) "Titreşim açık" else "Titreşim kapalı") }
                TextButton(onClick = { confirmReset = true }) { Text("Sıfırla") }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Sayaç sıfırlansın mı?") },
            text = { Text("Bu sayımdaki $count silinir. Toplam sayın korunur.") },
            confirmButton = { Button(onClick = { reset(); confirmReset = false }) { Text("Sıfırla") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") } },
        )
    }
}

/** A ring of 33 beads (the length of a tesbih); those counted this round glow gold. */
@Composable
private fun Beads(count: Int, target: Int, modifier: Modifier) {
    val gold = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val beads = 33
    val lit = if (count == 0) 0 else ((count - 1) % beads) + 1
    Canvas(modifier) {
        val c = center
        val r = min(size.width, size.height) / 2 * 0.86f
        drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.12f), Color.Transparent), c, r * 1.1f), r * 1.1f, c)
        drawCircle(dim, r, c, style = Stroke(width = 1.5.dp.toPx()))
        val br = (2 * PI * r / beads * 0.36).toFloat()
        for (i in 0 until beads) {
            val a = -PI / 2 + 2 * PI * i / beads
            val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
            val on = i < lit
            if (on) drawCircle(gold.copy(alpha = 0.35f), br * 1.6f, p)
            drawCircle(if (on) gold else dim, br, p)
        }
        // The imame: the long bead where a tesbih begins and ends.
        val top = Offset(c.x, c.y - r)
        drawRoundRectBead(top, br, if (target > 0 && count > 0 && count % target == 0) gold else dim)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectBead(at: Offset, br: Float, color: Color) {
    drawRoundRect(
        color,
        topLeft = Offset(at.x - br * 0.7f, at.y - br * 3.2f),
        size = androidx.compose.ui.geometry.Size(br * 1.4f, br * 2.2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(br * 0.7f),
    )
}
