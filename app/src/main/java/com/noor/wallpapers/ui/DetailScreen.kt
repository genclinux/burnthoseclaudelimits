package com.noor.wallpapers.ui

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noor.wallpapers.art.Palette
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.Background
import com.noor.wallpapers.service.PrayerWidget
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.Target
import com.noor.wallpapers.wallpaper.Wallpapers
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initial: Selection,
    title: String,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onChanged: (Selection) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var sel by remember { mutableStateOf(initial) }
    var full by remember { mutableStateOf<Bitmap?>(null) }
    /** The selection [full] was rendered for; while it differs from [sel] the sharp preview is stale. */
    var fullSel by remember { mutableStateOf<Selection?>(null) }
    var rendering by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf(false) }
    val thumb by rememberThumbnail(sel)
    val viewport = rememberViewport()
    var shuffles by remember { mutableIntStateOf(0) }
    val settings = remember { AppSettings(context) }
    var customPalettes by remember { mutableStateOf(settings.customPalettes) }
    var editingPalette by remember { mutableStateOf<String?>(null) } // palette id to return to if she cancels
    var options by remember { mutableStateOf(false) }

    /** Shows [text], then a "found a surprise" note if this was one. */
    fun say(text: String, surprise: HanifeBetul.Surprise? = null) {
        val found = surprise?.let { HanifeBetul.find(context, it) }
        scope.launch {
            snackbar.showSnackbar(text)
            if (found != null) snackbar.showSnackbar(found)
        }
    }

    LaunchedEffect(sel, viewport) {
        rendering = true
        full = Wallpapers.renderViewport(context, sel, viewport.width, viewport.height)
        fullSel = sel
        rendering = false
        // While she is still choosing colours, don't remember the half-made palette.
        if (editingPalette == null) onChanged(sel)
    }

    /**
     * Renders the real wallpaper bitmap (on a tablet, a square that works in
     * both orientations, so not the on-screen preview) and hands it to [action].
     */
    fun act(done: () -> String, surprise: HanifeBetul.Surprise? = null, action: suspend (Bitmap) -> Unit) {
        // While a new palette/seed renders, the preview still shows the previous one.
        if (rendering || busy) return
        scope.launch {
            busy = true
            try {
                action(Wallpapers.renderWallpaper(context, sel))
                val found = surprise?.let { HanifeBetul.find(context, it) }
                snackbar.showSnackbar(done())
                if (found != null) snackbar.showSnackbar(found)
            } catch (e: Exception) {
                snackbar.showSnackbar("Bir şeyler ters gitti: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // After a palette or shuffle, show the new colours at once (the quick low-res
        // thumbnail) instead of the old picture, then the sharp render when it lands.
        val stale = fullSel != sel
        val shown: ImageBitmap? = if (stale) (thumb ?: full?.asImageBitmap()) else full?.asImageBitmap() ?: thumb
        if (shown != null) {
            Image(
                shown,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        controls = !controls
                    },
            )
        }

        if (stale && rendering) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.size(10.dp))
                Text("Yeni renkler hazırlanıyor…", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopStart)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(sel.palette.name, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                }
                if (rendering || busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.size(12.dp))
                }
            }
        }

        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .navigationBarsPadding()
                    .padding(top = 48.dp, bottom = 16.dp),
            ) {
                val palettes = remember(customPalettes, sel.paletteId) {
                    val custom = (customPalettes + sel.paletteId).distinct().map(Palette::byId).filter { it.isCustom }
                    custom + Palette.ALL
                }
                PaletteRow(palettes, sel.paletteId, onAdd = {
                    editingPalette = sel.paletteId
                }) { sel = sel.copy(paletteId = it.id) }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    RoundAction(Icons.Filled.Refresh, "Karıştır") {
                        sel = sel.copy(seed = Random.nextInt(1, 100_000))
                        // Easter egg: every seventh shuffle earns a compliment.
                        if (++shuffles % 7 == 0) say(HanifeBetul.shuffleMessages[(shuffles / 7 - 1) % HanifeBetul.shuffleMessages.size], HanifeBetul.Surprise.SHUFFLE)
                    }
                    RoundAction(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favori", onToggleFavorite)
                    RoundAction(Icons.Filled.Edit, "Ayarla") { options = true }
                    Button(
                        onClick = { sheet = true },
                        enabled = full != null && !busy && !rendering,
                        modifier = Modifier.weight(1f).height(52.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) { Text("Duvar kağıdı yap") }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp))
    }

    editingPalette?.let { before ->
        PaletteEditor(
            initial = sel.palette,
            onPreview = { sel = sel.copy(paletteId = it.id) },
            onSave = { p ->
                customPalettes = (listOf(p.id) + customPalettes).distinct().take(12)
                settings.customPalettes = customPalettes
                editingPalette = null
                sel = sel.copy(paletteId = p.id)
                onChanged(sel)
                say("Paletin kaydedildi; her tasarımda kullanabilirsin 🎨", HanifeBetul.Surprise.PALETTE)
            },
            onDismiss = {
                editingPalette = null
                sel = sel.copy(paletteId = before)
            },
        )
    }

    if (options) {
        DesignOptionsSheet(
            options = sel.options,
            editableText = sel.entry.editableText,
            onChange = { o ->
                sel = sel.copy(options = o)
                if (sel.entry.editableText && o.text != null) HanifeBetul.find(context, HanifeBetul.Surprise.OWN_WORDS)?.let { say(it) }
            },
            onDismiss = { options = false },
        )
    }

    if (sheet) {
        NoorSheet("Nereye uygulansın?", onDismiss = { sheet = false }, subtitle = title) {
            val applied = HanifeBetul.Surprise.APPLIED
            SheetOption(Icons.Filled.Home, "Ana ekran") {
                sheet = false; act({ HanifeBetul.appliedMessages.random() }, applied) { Wallpapers.apply(context, it, Target.HOME) }
            }
            SheetOption(Icons.Filled.Lock, "Kilit ekranı") {
                sheet = false; act({ HanifeBetul.appliedMessages.random() }, applied) { Wallpapers.apply(context, it, Target.LOCK) }
            }
            SheetOption(Icons.Filled.Star, "Ana ekran ve kilit ekranı") {
                sheet = false; act({ HanifeBetul.appliedMessages.random() }, applied) { Wallpapers.apply(context, it, Target.BOTH) }
            }
            SheetOption(Icons.Filled.PlayArrow, "Canlı duvar kağıdı (yıldızlar ve vakitler)") {
                sheet = false
                try {
                    // She chose this design, so the live wallpaper shows it rather than following the day.
                    settings.liveFollowsPrayer = false
                    context.startActivity(Wallpapers.liveWallpaperIntent(context, sel))
                    HanifeBetul.find(context, HanifeBetul.Surprise.LIVE)
                } catch (_: ActivityNotFoundException) {
                    scope.launch { snackbar.showSnackbar("Bu cihaz canlı duvar kağıdını desteklemiyor") }
                }
            }
            SheetOption(NoorIcons.Vakit, "Vakit widget'ının arka planı") {
                sheet = false
                settings.widgetSelection = sel.encode()
                Background.executor.execute { PrayerWidget.updateAll(context.applicationContext) }
                say("Namaz vakitleri widget'ı artık bu tasarımla ✨")
            }
            SheetOption(Icons.Filled.KeyboardArrowDown, "Galeriye kaydet (PNG)") {
                sheet = false
                act({ "Resimler/noor by HBS klasörüne kaydedildi ✨" }) { Wallpapers.saveToGallery(context, it, "noor-${sel.entryId}-${sel.seed}") }
            }
        }
    }
}

@Composable
private fun PaletteRow(palettes: List<Palette>, selected: String, onAdd: () -> Unit, onSelect: (Palette) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        item(key = "+") { AddPaletteDot(onAdd) }
        items(palettes, key = { it.id }) { p -> PaletteDot(p, p.id == selected) { onSelect(p) } }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White,
        ),
    ) { Icon(icon, contentDescription = label) }
}

@Composable
private fun SheetOption(icon: ImageVector, label: String, onClick: () -> Unit) = OptionRow(label, icon = icon, onClick = onClick)
