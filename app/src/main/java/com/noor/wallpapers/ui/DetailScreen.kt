package com.noor.wallpapers.ui

import android.content.ActivityNotFoundException
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
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.Target
import com.noor.wallpapers.wallpaper.Wallpapers
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.graphics.Bitmap

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
    var rendering by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf(false) }
    val thumb by rememberThumbnail(sel)

    LaunchedEffect(sel) {
        rendering = true
        full = Wallpapers.renderFullSize(context, sel)
        rendering = false
        onChanged(sel)
    }

    fun act(done: String, action: suspend (Bitmap) -> Unit) {
        val bmp = full ?: return
        scope.launch {
            busy = true
            try {
                action(bmp)
                snackbar.showSnackbar(done)
            } catch (e: Exception) {
                snackbar.showSnackbar("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val shown: ImageBitmap? = full?.asImageBitmap() ?: thumb
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                PaletteRow(sel.paletteId) { sel = sel.copy(paletteId = it.id) }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    RoundAction(Icons.Filled.Refresh, "Shuffle") { sel = sel.copy(seed = Random.nextInt(1, 100_000)) }
                    RoundAction(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favourite", onToggleFavorite)
                    RoundAction(Icons.Filled.KeyboardArrowDown, "Save to gallery") {
                        act("Saved to Pictures/Noor") { Wallpapers.saveToGallery(context, it, "noor-${sel.entryId}-${sel.paletteId}-${sel.seed}") }
                    }
                    Button(
                        onClick = { sheet = true },
                        enabled = full != null && !busy,
                        modifier = Modifier.weight(1f).height(52.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) { Text("Set wallpaper") }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp))
    }

    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Apply to",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SheetOption(Icons.Filled.Home, "Home screen") {
                    sheet = false; act("Home screen wallpaper set") { Wallpapers.apply(context, it, Target.HOME) }
                }
                SheetOption(Icons.Filled.Lock, "Lock screen") {
                    sheet = false; act("Lock screen wallpaper set") { Wallpapers.apply(context, it, Target.LOCK) }
                }
                SheetOption(Icons.Filled.Star, "Home and lock screens") {
                    sheet = false; act("Wallpaper set") { Wallpapers.apply(context, it, Target.BOTH) }
                }
                SheetOption(Icons.Filled.PlayArrow, "Live wallpaper (twinkling stars)") {
                    sheet = false
                    try {
                        context.startActivity(Wallpapers.liveWallpaperIntent(context, sel))
                    } catch (_: ActivityNotFoundException) {
                        scope.launch { snackbar.showSnackbar("Live wallpapers aren't supported on this device") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteRow(selected: String, onSelect: (Palette) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        items(Palette.ALL, key = { it.id }) { p ->
            val isSel = p.id == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        BorderStroke(if (isSel) 3.dp else 1.dp, if (isSel) Color(p.line) else Color.White.copy(alpha = 0.3f)),
                        CircleShape,
                    )
                    .padding(4.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(p.bgTop), Color(p.accentA), Color(p.bgBottom))),
                        CircleShape,
                    )
                    .clickable { onSelect(p) },
            ) {
                Box(Modifier.size(10.dp).background(Color(p.line), CircleShape))
            }
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White,
        ),
    ) { Icon(icon, contentDescription = label) }
}

@Composable
private fun SheetOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
