package com.noor.wallpapers.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.wallpapers.art.Category
import com.noor.wallpapers.wallpaper.Selection

/** null category = all; [favoritesOnly] narrows further. */
@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val greeting = remember { HanifeBetul.greeting() }
    var titleTaps by remember { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scroll,
                title = {
                    // Easter egg: tap the title five times.
                    Column(
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            titleTaps++
                            if (titleTaps >= 5) {
                                titleTaps = 0
                                onDedication()
                            }
                        },
                    ) {
                        Text("Nur · نور", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            greeting,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDedication) {
                        Text("✦", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onFavoritesOnly(!favoritesOnly) }) {
                        Icon(
                            if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favoritesOnly) "Tümünü göster" else "Favorileri göster",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        val thumb = rememberThumbnailSize()
        val aspect = thumb.width / thumb.height.toFloat()
        val landscape = aspect > 1f
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (landscape) 220.dp else 150.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryRow(category, onCategory)
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
                    sel = sel,
                    title = title,
                    aspect = aspect,
                    favorite = sel.entryId in favorites,
                    onToggleFavorite = { onToggleFavorite(sel.entryId) },
                    onClick = { onOpen(sel) },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: Category?, onSelect: (Category?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        item { Chip("Tümü", null, selected == null) { onSelect(null) } }
        items(Category.entries) { c -> Chip(c.title, c.arabic, selected == c) { onSelect(c) } }
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
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun WallpaperCard(
    sel: Selection,
    title: String,
    aspect: Float,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val thumb by rememberThumbnail(sel)
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        val bmp = thumb
        if (bmp != null) {
            Image(bmp, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp).align(Alignment.Center),
            )
        }
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
