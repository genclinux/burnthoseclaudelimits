package com.noor.wallpapers.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.art.Category
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.PrayerRepository
import com.noor.wallpapers.service.RotationSchedule
import com.noor.wallpapers.wallpaper.Selection
import java.time.LocalDateTime

/**
 * null category = all; [favoritesOnly] narrows further. While [picks] is not
 * null the grid is choosing designs for the wallpaper rotation: a tap ticks a
 * card instead of opening it, and [pickingBar] sits at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    items: List<Pair<Selection, String>>,
    category: Category?,
    favoritesOnly: Boolean,
    favorites: Set<String>,
    onCategory: (Category?) -> Unit,
    onFavoritesOnly: (Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpen: (Selection) -> Unit,
    onDedication: () -> Unit,
    onBetulTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    picks: List<String>? = null,
    onStartPicking: () -> Unit = {},
    onTogglePick: (String) -> Unit = {},
    onClosePicking: () -> Unit = {},
    pickingBar: @Composable () -> Unit = {},
    photos: Boolean = false,
    onPhotos: (Boolean) -> Unit = {},
    /** "Gerçek Camiler": the photo grid, given the padding and the category row to put on top. */
    photoGrid: @Composable (PaddingValues, @Composable () -> Unit) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val greeting = remember {
        HanifeBetul.greeting(LocalDateTime.now(), AppSettings(context).birthday, PrayerRepository.hijri(context))
    }
    var titleTaps by remember { mutableIntStateOf(0) }
    if (favoritesOnly && items.isEmpty() && !photos) {
        LaunchedEffect(Unit) { HanifeBetul.find(context, HanifeBetul.Surprise.EMPTY_FAVORITES) }
    }
    val gridState = rememberLazyGridState()
    Scaffold(
        bottomBar = { if (picks != null) pickingBar() },
        // Döngü, labelled and always in reach, instead of an unexplained icon.
        floatingActionButton = {
            if (picks == null) {
                ExtendedFloatingActionButton(
                    onClick = onStartPicking,
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    text = { Text("Döngü") },
                    // Shrinks to its icon while she scrolls through the designs, and opens again at the top.
                    expanded = !gridState.canScrollBackward || !gridState.lastScrolledForward,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        topBar = {
            if (picks != null) {
                ScreenHeader(
                    title = "Döngü için seç",
                    subtitle = "${picks.size} / ${RotationSchedule.MAX} seçildi · sırayla gösterilir",
                ) {
                    IconButton(onClick = onClosePicking) { Icon(Icons.Filled.Close, contentDescription = "Vazgeç") }
                }
            } else {
                ScreenHeader(
                    title = "Galeri",
                    subtitle = greeting,
                    // Easter egg: tap the title five times.
                    onTitle = {
                        titleTaps++
                        if (titleTaps >= 5) {
                            titleTaps = 0
                            HanifeBetul.find(context, HanifeBetul.Surprise.TITLE_TAPS)
                            onDedication()
                        }
                    },
                    onSettings = onOpenSettings,
                ) {
                    // ✦ opens her page; a long press paints the app in her colours.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .combinedClickable(onClick = onDedication, onLongClick = onBetulTheme),
                    ) {
                        Text("✦", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onFavoritesOnly(!favoritesOnly) }) {
                        Icon(
                            if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favoritesOnly) "Tümünü göster" else "Favorileri göster",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    ) { padding ->
        val categoryRow: @Composable () -> Unit = {
            CategoryRow(
                selected = category,
                photos = photos,
                onSelect = { onPhotos(false); onCategory(it) },
                onPhotos = { onPhotos(true) },
            )
        }
        if (photos) {
            photoGrid(padding, categoryRow)
        } else {
            val thumb = rememberThumbnailSize()
            val aspect = thumb.width / thumb.height.toFloat()
            val landscape = aspect > 1f
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = if (landscape) 220.dp else 150.dp),
                contentPadding = PaddingValues(
                    start = Noor.Gutter, end = Noor.Gutter, top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp, // clear of the Döngü button
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    categoryRow()
                }
                if (items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            HanifeBetul.EMPTY_FAVOURITES,
                            modifier = Modifier.padding(vertical = 48.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(items, key = { it.first.entryId }) { (sel, title) ->
                    WallpaperCard(
                        // Cards glide into place when the category or favourites change.
                        modifier = Modifier.animateItem(),
                        sel = sel,
                        title = title,
                        aspect = aspect,
                        favorite = sel.entryId in favorites,
                        onToggleFavorite = { onToggleFavorite(sel.entryId) },
                        onClick = { if (picks != null) onTogglePick(sel.entryId) else onOpen(sel) },
                        pickNumber = picks?.let { it.indexOf(sel.entryId) + 1 },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "noor by HBS · Sürüm ${rememberVersionName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: Category?, photos: Boolean, onSelect: (Category?) -> Unit, onPhotos: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        item { Chip("Tümü", null, !photos && selected == null) { onSelect(null) } }
        item { Chip("📷 Gerçek Camiler", null, photos, onPhotos) }
        items(Category.entries) { c -> Chip(c.title, c.arabic, !photos && selected == c) { onSelect(c) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chip(label: String, arabic: String?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (arabic != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(arabic, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                }
            }
        },
        shape = Noor.Pill,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun WallpaperCard(
    modifier: Modifier = Modifier,
    sel: Selection,
    title: String,
    aspect: Float,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    /** null = not choosing; 0 = not picked; otherwise its place in the rotation. */
    pickNumber: Int? = null,
) {
    val thumb by rememberThumbnail(sel)
    val shape = Noor.Card
    val picked = pickNumber != null && pickNumber > 0
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .pressable(onClick)
            .then(if (picked) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        FadeInImage(thumb, contentDescription = title, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        if (pickNumber != null) PickBadge(pickNumber, Modifier.align(Alignment.TopStart))
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(start = 12.dp, end = 4.dp, top = 24.dp, bottom = 4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart).padding(end = 40.dp),
            )
            IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favorite) "Favorilerden çıkar" else "Favorilere ekle",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        }
    }
}

/** Döngü: an empty ring, or the card's place in the rotation. */
@Composable
fun PickBadge(pickNumber: Int, modifier: Modifier = Modifier) {
    val picked = pickNumber > 0
    // Picking pops the badge; un-picking lets it settle back.
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (picked) 1.15f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "pick",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(10.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(30.dp)
            .clip(CircleShape)
            .background(if (picked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f))
            .border(2.dp, if (picked) MaterialTheme.colorScheme.primary else Color.White, CircleShape),
    ) {
        if (picked) Text("$pickNumber", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

/** The installed versionName, so it's easy to tell which build is on the device. */
@Composable
fun rememberVersionName(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        @Suppress("DEPRECATION")
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
}
