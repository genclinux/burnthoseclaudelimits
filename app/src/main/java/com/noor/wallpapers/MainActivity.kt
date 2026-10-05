package com.noor.wallpapers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.Category
import com.noor.wallpapers.ui.DedicationSheet
import com.noor.wallpapers.ui.DetailScreen
import com.noor.wallpapers.ui.WelcomeDialog
import com.noor.wallpapers.ui.GalleryScreen
import com.noor.wallpapers.ui.NoorTheme
import com.noor.wallpapers.wallpaper.Prefs
import com.noor.wallpapers.wallpaper.Selection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { NoorTheme { NoorApp() } }
    }
}

@Composable
private fun NoorApp() {
    val prefs = Prefs(LocalContext.current)
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(prefs.favorites) }
    var openEncoded by rememberSaveable { mutableStateOf<String?>(null) }
    // Bumped when a detail screen saves a customised palette/seed, so the grid re-reads it.
    var customVersion by remember { mutableStateOf(0) }
    var welcome by remember { mutableStateOf(!prefs.welcomed) }
    var dedication by rememberSaveable { mutableStateOf(false) }

    fun openCollection() {
        category = Category.HANIFE_BETUL
        favoritesOnly = false
        openEncoded = null
    }

    if (welcome) {
        WelcomeDialog(
            onOpenCollection = { prefs.welcomed = true; welcome = false; openCollection() },
            onDismiss = { prefs.welcomed = true; welcome = false },
        )
    }
    if (dedication) {
        DedicationSheet(
            onOpenCollection = { dedication = false; openCollection() },
            onDismiss = { dedication = false },
        )
    }

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.favorites = favorites
    }

    val open = Selection.decode(openEncoded)
    BackHandler(enabled = open != null) { openEncoded = null }

    AnimatedContent(open, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { sel ->
        if (sel == null) {
            val items = remember(category, favoritesOnly, favorites, customVersion) {
                Catalog.entries
                    .filter { category == null || it.category == category }
                    .filter { !favoritesOnly || it.id in favorites }
                    .map { (prefs.customised(it.id) ?: Selection.of(it)) to it.title }
            }
            GalleryScreen(
                items = items,
                category = category,
                favoritesOnly = favoritesOnly,
                favorites = favorites,
                onCategory = { category = it },
                onFavoritesOnly = { favoritesOnly = it },
                onToggleFavorite = ::toggleFavorite,
                onOpen = { openEncoded = it.encode() },
                onDedication = { dedication = true },
            )
        } else {
            DetailScreen(
                initial = sel,
                title = sel.entry.title,
                favorite = sel.entryId in favorites,
                onToggleFavorite = { toggleFavorite(sel.entryId) },
                onChanged = {
                    prefs.saveCustomised(it)
                    customVersion++
                },
                onBack = { openEncoded = null },
            )
        }
    }
}
