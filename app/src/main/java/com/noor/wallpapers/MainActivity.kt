package com.noor.wallpapers

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.Category
import com.noor.wallpapers.art.Palette
import com.noor.wallpapers.prayer.Provinces
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.Background
import com.noor.wallpapers.service.Notifications
import com.noor.wallpapers.service.PrayerAlarms
import com.noor.wallpapers.service.PrayerRepository
import com.noor.wallpapers.service.PrayerWidget
import com.noor.wallpapers.service.RotationAlarms
import com.noor.wallpapers.service.RotationSchedule
import com.noor.wallpapers.service.Work
import com.noor.wallpapers.ui.CalendarScreen
import com.noor.wallpapers.ui.DedicationSheet
import com.noor.wallpapers.ui.DetailScreen
import com.noor.wallpapers.ui.GalleryScreen
import com.noor.wallpapers.ui.HanifeBetul
import com.noor.wallpapers.ui.LocationPicker
import com.noor.wallpapers.ui.NoorIcons
import com.noor.wallpapers.ui.NoorTheme
import com.noor.wallpapers.ui.PrayerScreen
import com.noor.wallpapers.ui.QiblaScreen
import com.noor.wallpapers.ui.RotationBar
import com.noor.wallpapers.ui.RotationIntroDialog
import com.noor.wallpapers.ui.SettingsSheet
import com.noor.wallpapers.ui.TesbihScreen
import com.noor.wallpapers.ui.WelcomeDialog
import com.noor.wallpapers.ui.rememberSettingsVersion
import com.noor.wallpapers.wallpaper.Prefs
import com.noor.wallpapers.wallpaper.Selection
import com.noor.wallpapers.wallpaper.Wallpapers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** Tab requested by a notification or the widget. */
    private val requestedTab = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedTab.value = intent?.getStringExtra(EXTRA_TAB)
        setContent { NoorApp(requestedTab.value) { requestedTab.value = null } }

        // Keep alarms, the widget and background refresh in step with whatever changed while closed.
        val app = applicationContext
        // No location prompt: start in Pendik, İstanbul (changeable in Ayarlar).
        if (AppSettings(app).location == null) {
            PrayerRepository.setLocation(app, Provinces.PENDIK)
            Work.refreshNow(app)
        }
        Notifications.ensureChannels(app)
        Work.ensure(app)
        Background.executor.execute {
            PrayerAlarms.reschedule(app)
            PrayerWidget.updateAll(app)
            RotationAlarms.reschedule(app)
        }
        // Catch up if the rotation missed a change while the phone slept.
        if (RotationAlarms.active(app)) RotationAlarms.applyNow(app)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_TAB)?.let { requestedTab.value = it }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_PRAYER = "vakitler"
    }
}

private enum class Tab(val key: String, val title: String) {
    GALLERY("galeri", "Galeri"),
    PRAYER(MainActivity.TAB_PRAYER, "Vakitler"),
    QIBLA("kible", "Kıble"),
    TESBIH("zikir", "Zikir"),
    CALENDAR("takvim", "Takvim"),
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.PRAYER -> NoorIcons.Vakit
    Tab.GALLERY -> NoorIcons.Galeri
    Tab.QIBLA -> NoorIcons.Kible
    Tab.TESBIH -> NoorIcons.Tesbih
    Tab.CALENDAR -> NoorIcons.Takvim
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoorApp(requestedTab: String?, onTabHandled: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val settings = remember { AppSettings(context) }
    // Only the settings this level shows; a tesbih tap must not recompose the whole app.
    val version = rememberSettingsVersion(setOf(AppSettings.KEY_THEME, AppSettings.KEY_SECRET))
    var themeVersion by remember { mutableIntStateOf(0) }
    // Keyed on the id so unrelated setting changes (every tesbih tap) don't rebuild the theme.
    val themeId = remember(themeVersion, version) { settings.themePalette }
    val theme = remember(themeId) { themeId?.let(Palette::byId) }

    NoorTheme(theme) {
        var tab by rememberSaveable { mutableStateOf(Tab.GALLERY) }
        var category by rememberSaveable { mutableStateOf<Category?>(null) }
        var favoritesOnly by rememberSaveable { mutableStateOf(false) }
        var favorites by remember { mutableStateOf(prefs.favorites) }
        var openEncoded by rememberSaveable { mutableStateOf<String?>(null) }
        // Bumped when a detail screen saves a customised palette/seed, so the grid re-reads it.
        var customVersion by remember { mutableIntStateOf(0) }
        var welcome by remember { mutableStateOf(!prefs.welcomed) }
        var dedication by rememberSaveable { mutableStateOf(false) }
        var picking by rememberSaveable { mutableStateOf(false) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var rotationIntro by remember { mutableStateOf(!prefs.rotationIntroSeen) }
        // Designs being chosen for the rotation, in order; null when not choosing.
        var picks by rememberSaveable { mutableStateOf<List<String>?>(null) }
        var rotationRunning by remember { mutableStateOf(RotationAlarms.active(context)) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        fun say(text: String) {
            scope.launch { snackbar.showSnackbar(text) }
        }

        LaunchedEffect(requestedTab) {
            Tab.entries.firstOrNull { it.key == requestedTab }?.let { tab = it; openEncoded = null }
            if (requestedTab != null) onTabHandled()
        }
        LaunchedEffect(Unit) {
            if (LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY) HanifeBetul.find(context, HanifeBetul.Surprise.FRIDAY)?.let(::say)
        }

        fun openCollection() {
            tab = Tab.GALLERY
            category = Category.HANIFE_BETUL
            favoritesOnly = false
            openEncoded = null
        }

        if (welcome) {
            WelcomeDialog(
                onOpenCollection = { prefs.welcomed = true; welcome = false; HanifeBetul.find(context, HanifeBetul.Surprise.WELCOME); openCollection() },
                onDismiss = { prefs.welcomed = true; welcome = false; HanifeBetul.find(context, HanifeBetul.Surprise.WELCOME) },
            )
        }
        fun startPicking() {
            tab = Tab.GALLERY
            openEncoded = null
            picks = settings.rotationIds.take(RotationSchedule.MAX)
        }

        // After the welcome, so a new install sees one dialog at a time.
        if (rotationIntro && !welcome) {
            RotationIntroDialog(
                onTry = { prefs.rotationIntroSeen = true; rotationIntro = false; startPicking() },
                onDismiss = { prefs.rotationIntroSeen = true; rotationIntro = false },
            )
        }
        if (dedication) {
            LaunchedEffect(Unit) { HanifeBetul.find(context, HanifeBetul.Surprise.DEDICATION) }
            DedicationSheet(
                onOpenCollection = { dedication = false; openCollection() },
                onDismiss = { dedication = false },
                onThemeChanged = { themeVersion++ },
            )
        }
        if (picking) {
            LocationPicker(onDone = { msg -> picking = false; say(msg) }, onDismiss = { picking = false })
        }
        if (settingsOpen) {
            SettingsSheet(
                onPickLocation = { settingsOpen = false; picking = true },
                onThemeChanged = { themeVersion++ },
                onMessage = ::say,
                onEditRotation = { settingsOpen = false; startPicking() },
                onRotationChanged = { rotationRunning = RotationAlarms.active(context) },
                onDismiss = { settingsOpen = false },
            )
        }

        fun toggleFavorite(id: String) {
            favorites = if (id in favorites) favorites - id else favorites + id
            prefs.favorites = favorites
        }

        val open = Selection.decode(openEncoded)
        BackHandler(enabled = open != null) { openEncoded = null }
        BackHandler(enabled = open == null && tab != Tab.GALLERY) { tab = Tab.GALLERY }
        BackHandler(enabled = open == null && tab == Tab.GALLERY && picks != null) { picks = null }

        if (open != null) {
            DetailScreen(
                initial = open,
                title = open.entry.title,
                favorite = open.entryId in favorites,
                onToggleFavorite = { toggleFavorite(open.entryId) },
                onChanged = {
                    prefs.saveCustomised(it)
                    customVersion++
                },
                onBack = { openEncoded = null },
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        for (t in Tab.entries) {
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon(), contentDescription = null) },
                                label = { Text(t.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                // Only the bottom is taken by the navigation bar; each screen still pads for the status bar itself.
                val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
                Box(Modifier.fillMaxSize().padding(bottom).consumeWindowInsets(bottom)) {
                    AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                        when (t) {
                            Tab.PRAYER -> PrayerScreen(
                                onPickLocation = { picking = true },
                                onOpenSettings = { settingsOpen = true },
                                onMessage = ::say,
                            )
                            Tab.GALLERY -> {
                                val unlocked = settings.secretUnlocked
                                val items = remember(category, favoritesOnly, favorites, customVersion, unlocked, version) {
                                    val only = category
                                    Catalog.visible(unlocked)
                                        .filter { only == null || it.inCategory(only) }
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
                                    picks = picks,
                                    onStartPicking = ::startPicking,
                                    onTogglePick = { id ->
                                        val current = picks.orEmpty()
                                        picks = when {
                                            id in current -> current - id
                                            current.size >= RotationSchedule.MAX -> {
                                                say("En fazla ${RotationSchedule.MAX} duvar kağıdı seçebilirsin")
                                                current
                                            }
                                            else -> current + id
                                        }
                                    },
                                    onClosePicking = { picks = null },
                                    pickingBar = {
                                        RotationBar(
                                            count = picks?.size ?: 0,
                                            running = rotationRunning,
                                            onStart = { interval, target, live ->
                                                val chosen = picks.orEmpty()
                                                RotationAlarms.start(context, chosen, interval, target, live)
                                                rotationRunning = true
                                                picks = null
                                                // Live mode needs HBSnoor's live wallpaper on screen; offer it if it isn't.
                                                val supported = !live || RotationAlarms.liveWallpaperSet(context) || try {
                                                    val first = Catalog.byId(chosen.first())?.let { prefs.customised(it.id) ?: Selection.of(it) }
                                                    first?.let { context.startActivity(Wallpapers.liveWallpaperIntent(context, it)) }
                                                    true
                                                } catch (_: Exception) {
                                                    false
                                                }
                                                if (supported) {
                                                    say("Döngü başladı: ${interval.title.replaceFirstChar { it.lowercase() }} yeni duvar kağıdı ✨")
                                                } else {
                                                    RotationAlarms.stop(context)
                                                    rotationRunning = false
                                                    say("Bu cihaz canlı duvar kağıdını desteklemiyor")
                                                }
                                            },
                                            onStop = {
                                                RotationAlarms.stop(context)
                                                rotationRunning = false
                                                picks = null
                                                say("Döngü durduruldu")
                                            },
                                        )
                                    },
                                    onBetulTheme = {
                                        settings.themePalette = if (settings.themePalette == "betul") null else "betul"
                                        themeVersion++
                                        HanifeBetul.find(context, HanifeBetul.Surprise.THEME)?.let(::say)
                                            ?: say(if (settings.themePalette == "betul") "Betül renkleri 🌸" else "Zümrüt ve altın ✨")
                                    },
                                )
                            }
                            Tab.QIBLA -> QiblaScreen(onMessage = ::say)
                            Tab.TESBIH -> TesbihScreen(onMessage = ::say)
                            Tab.CALENDAR -> CalendarScreen()
                        }
                    }
                }
            }
        }
    }
}
