package com.noor.wallpapers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noor.wallpapers.art.Colors
import com.noor.wallpapers.art.DesignOptions
import com.noor.wallpapers.art.Palette
import com.noor.wallpapers.art.PaletteMaker
import kotlin.random.Random

/** Swatches for her own palette: deep grounds, metals and pale ornaments, and jewel accents. */
private object Swatches {
    val BACKGROUNDS = listOf(
        "#123A44", "#0D4633", "#14275A", "#431A38", "#5E1A2A", "#5E2215", "#2E1456", "#1E1E24",
        "#3A4220", "#0E3B5A", "#1F3A2B", "#4A2A2E", "#0A0A0A", "#B98C5F", "#CDBA96", "#E9DCC9",
    ).map(Colors::hex)
    val ORNAMENTS = listOf(
        "#D9B54A", "#E8B4A0", "#F0D9A8", "#D8DDE3", "#F4EEDC", "#C9824A", "#B08D57", "#FFFFFF",
        "#7FD6CF", "#F4C7C3", "#C9A7FF", "#1B1611",
    ).map(Colors::hex)
    val ACCENTS = listOf(
        "#C97B84", "#1C7A58", "#2A4FA8", "#1C9AA0", "#B83A2E", "#5B2A9A", "#E0A030", "#4F7F72",
        "#A88BC9", "#3C7BC4", "#B0466E", "#7A8B3A", "#F4A261", "#E76F51",
    ).map(Colors::hex)
}

/**
 * Kendi paletin: pick a background, an ornament and an accent (or roll the
 * dice). The design behind the sheet redraws as she picks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteEditor(initial: Palette, onPreview: (Palette) -> Unit, onSave: (Palette) -> Unit, onDismiss: () -> Unit) {
    val seeds = remember { PaletteMaker.seeds(initial) }
    var bg by remember { mutableIntStateOf(seeds.first) }
    var ornament by remember { mutableIntStateOf(seeds.second) }
    var accent by remember { mutableIntStateOf(seeds.third) }
    val rnd = remember { Random(System.nanoTime()) }

    fun update() = onPreview(PaletteMaker.make(bg, ornament, accent))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
        scrimColor = Color.Transparent,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Kendi paletin", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val p = PaletteMaker.random(rnd)
                    val s = PaletteMaker.seeds(p)
                    bg = s.first; ornament = s.second; accent = s.third
                    update()
                }) { Text("Zar at 🎲") }
            }
            SwatchRow("Zemin", Swatches.BACKGROUNDS, bg) { bg = it; update() }
            SwatchRow("Süsleme (yaldız, çizgi, hat)", Swatches.ORNAMENTS, ornament) { ornament = it; update() }
            SwatchRow("Vurgu (yıldızlar, çini, fener)", Swatches.ACCENTS, accent) { accent = it; update() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Vazgeç") }
                Button(onClick = { onSave(PaletteMaker.make(bg, ornament, accent)) }, modifier = Modifier.weight(1f)) { Text("Kaydet") }
            }
        }
    }
}

@Composable
private fun SwatchRow(title: String, colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(6.dp))
        // A custom (random) colour that isn't a swatch is shown first so it stays visible.
        val list = if (colors.any { it == selected }) colors else listOf(selected) + colors
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
            items(list) { c ->
                val isSel = c == selected
                Box(
                    Modifier
                        .size(38.dp)
                        .border(BorderStroke(if (isSel) 3.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f)), CircleShape)
                        .padding(4.dp)
                        .background(Color(c), CircleShape)
                        .clickable { onPick(c) },
                )
            }
        }
    }
}

/**
 * Ayarla: captions on or off, how much paper texture, how dark, and for
 * "Kendi Sözün" her own words.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignOptionsSheet(options: DesignOptions, editableText: Boolean, onChange: (DesignOptions) -> Unit, onDismiss: () -> Unit) {
    var captions by remember { mutableStateOf(options.captions) }
    var texture by remember { mutableFloatStateOf(options.texture) }
    var dim by remember { mutableFloatStateOf(options.dim) }
    var text by remember { mutableStateOf(options.text ?: "") }

    fun current() = DesignOptions(captions, texture, dim, text.trim().takeIf { editableText && it.isNotEmpty() })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
        scrimColor = Color.Transparent,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
        ) {
            Text("Ayarla", style = MaterialTheme.typography.titleLarge)
            if (editableText) {
                Text("Sözün", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(DesignOptions.MAX_TEXT) },
                    placeholder = { Text("Yolun nur olsun") },
                    supportingText = { Text("${text.length}/${DesignOptions.MAX_TEXT} · satır atlamak için alt satıra geç") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onChange(current()) }) { Text("Levhaya yaz") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Okunuş ve anlam")
                    Text("Hattın altındaki Türkçe satırlar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = captions, onCheckedChange = { captions = it; onChange(current()) })
            }
            Text("Kâğıt dokusu: %${(texture * 100).toInt()}")
            Slider(value = texture, onValueChange = { texture = it }, onValueChangeFinished = { onChange(current()) })
            Text("Karartma: %${(dim * 100).toInt()}")
            Slider(
                value = dim,
                onValueChange = { dim = it },
                valueRange = 0f..DesignOptions.MAX_DIM,
                onValueChangeFinished = { onChange(current()) },
            )
            Text(
                "Karartma simgeleri ve saati okumayı kolaylaştırır; OLED ekranda pil de kazandırır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                captions = true; texture = 1f; dim = 0f; text = ""
                onChange(DesignOptions.DEFAULT)
            }) { Text("İlk haline döndür") }
        }
    }
}

/** A palette as a small round swatch: background, ornament and accent. */
@Composable
fun PaletteDot(p: Palette, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .border(
                BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Color(p.line) else Color.White.copy(alpha = 0.3f)),
                CircleShape,
            )
            .padding(4.dp)
            .background(Brush.linearGradient(listOf(Color(p.bgTop), Color(p.accentA), Color(p.bgBottom))), CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.size(10.dp).background(Color(p.line), CircleShape))
    }
}

/** The "+" at the end of the palette row. */
@Composable
fun AddPaletteDot(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), CircleShape)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(onClick = onClick),
    ) {
        Text("+", color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}
