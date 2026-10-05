package com.noor.wallpapers.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.wallpapers.photos.Photo
import com.noor.wallpapers.photos.Photos
import com.noor.wallpapers.photos.Region
import com.noor.wallpapers.wallpaper.Target
import com.noor.wallpapers.wallpaper.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Gerçek Camiler": real photographs from Wikimedia Commons, Pendik first,
 * then İstanbul, then the wider world. While [picks] is not null, a tap
 * ticks a photo for Döngü instead of opening it.
 */
@Composable
fun PhotoGrid(
    padding: PaddingValues,
    header: @Composable () -> Unit,
    picks: List<String>?,
    onTogglePick: (String) -> Unit,
    onOpen: (Photo) -> Unit,
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Photo>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        loading = true
        if (photos == null) photos = withContext(Dispatchers.IO) { Photos.cached(context) }
        photos = runCatching { Photos.refresh(context) }.getOrDefault(photos)
        loading = false
    }

    val thumb = rememberThumbnailSize()
    val aspect = thumb.width / thumb.height.toFloat()
    val list = photos.orEmpty()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (aspect > 1f) 220.dp else 150.dp),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Gerçek fotoğraflar: önce Pendik, sonra İstanbul'dan birkaç ve dünyanın dört bir yanından camiler. " +
                    "Hepsi Wikimedia Commons'tan, özgür lisanslı; fotoğrafçısı her birinde yazıyor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (list.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)) {
                    if (loading) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Fotoğraflar getiriliyor…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Fotoğraflar için bir kez internet gerekiyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { attempt++ }) { Text("Tekrar dene") }
                    }
                }
            }
        }
        for (region in Region.entries) {
            val inRegion = list.filter { it.place.region == region }
            if (inRegion.isEmpty()) continue
            item(span = { GridItemSpan(maxLineSpan) }, key = "region-${region.name}") {
                Text(
                    region.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(inRegion, key = { it.id }) { photo ->
                PhotoCard(
                    photo = photo,
                    aspect = aspect,
                    pickNumber = picks?.let { it.indexOf(photo.id) + 1 },
                    onClick = { if (picks != null) onTogglePick(photo.id) else onOpen(photo) },
                )
            }
        }
    }
}

@Composable
private fun PhotoCard(photo: Photo, aspect: Float, pickNumber: Int?, onClick: () -> Unit) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(Photos.cachedThumbnail(photo)?.asImageBitmap(), photo.pageId) {
        value = runCatching { Photos.thumbnail(context, photo).asImageBitmap() }.getOrNull()
    }
    val shape = RoundedCornerShape(20.dp)
    val picked = pickNumber != null && pickNumber > 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .then(if (picked) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        val bmp = image
        if (bmp != null) {
            Image(bmp, contentDescription = photo.place.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp).align(Alignment.Center))
        }
        if (pickNumber != null) PickBadge(pickNumber, Modifier.align(Alignment.TopStart))
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
        ) {
            Text(photo.place.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "📷 ${photo.artist}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One photo, full screen, with its credit and "Duvar kağıdı yap". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(photo: Photo, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val viewport = rememberViewport()
    var controls by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val full by produceState<Bitmap?>(null, photo.pageId, viewport) {
        value = runCatching { Photos.viewport(context, photo, viewport.width, viewport.height) }
            .onFailure { failed = true }
            .getOrNull()
    }
    val small by produceState(Photos.cachedThumbnail(photo)?.asImageBitmap(), photo.pageId) {
        value = runCatching { Photos.thumbnail(context, photo).asImageBitmap() }.getOrNull()
    }

    fun apply(target: Target) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                val bmp = Photos.wallpaper(context, photo)
                Wallpapers.apply(context, bmp, target)
                bmp.recycle()
                snackbar.showSnackbar(HanifeBetul.appliedMessages.random())
            } catch (e: Exception) {
                snackbar.showSnackbar("Bir şeyler ters gitti: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val shown = full?.asImageBitmap() ?: small
        if (shown != null) {
            Image(
                shown,
                contentDescription = photo.place.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { controls = !controls },
            )
        }
        if (full == null) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                if (failed) {
                    Text("Fotoğraf indirilemedi; internet bağlantını kontrol et.", color = Color.White)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.size(10.dp))
                        Text("Fotoğraf iniyor…", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
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
                    Text(photo.place.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(photo.place.region.title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                }
                if (busy) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp, color = Color.White)
            }
        }

        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .navigationBarsPadding()
                    .padding(top = 48.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
            ) {
                // Credit, as the licence asks; a tap opens the photo's page on Commons.
                Text(
                    "${photo.credit} · Wikimedia Commons",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(photo.pageUrl))) }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(photo.pageUrl))) }
                    }) { Text("Kaynak", color = Color.White) }
                    Button(
                        onClick = { sheet = true },
                        enabled = full != null && !busy,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Duvar kağıdı yap") }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp))
    }

    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text("Nereye uygulansın?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                for ((target, icon, label) in listOf(
                    Triple(Target.HOME, Icons.Filled.Home, "Ana ekran"),
                    Triple(Target.LOCK, Icons.Filled.Lock, "Kilit ekranı"),
                    Triple(Target.BOTH, Icons.Filled.Star, "Ana ekran ve kilit ekranı"),
                )) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sheet = false; apply(target) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(16.dp))
                        Text(label)
                    }
                }
                Text(
                    "Döngüye eklemek için Galeri'de ↻ düğmesine dokun ve bu fotoğrafı seç.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}
