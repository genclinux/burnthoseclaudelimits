package com.noor.wallpapers.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.noor.wallpapers.art.Palette
import com.noor.wallpapers.prayer.OverlayPosition
import com.noor.wallpapers.prayer.OverlayStyle
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.service.AppSettings
import com.noor.wallpapers.service.Background
import com.noor.wallpapers.service.DailySource
import com.noor.wallpapers.service.Notifications
import com.noor.wallpapers.service.PrayerAlarms
import com.noor.wallpapers.service.PrayerWidget
import com.noor.wallpapers.service.RotationAlarms
import com.noor.wallpapers.service.Work
import com.noor.wallpapers.wallpaper.Prefs
import com.noor.wallpapers.wallpaper.Target
import com.noor.wallpapers.wallpaper.Wallpapers

/** Everything beyond the gallery: notifications, the live wallpaper's prayer panel, daily wallpapers, the app's colours. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    onPickLocation: () -> Unit,
    onThemeChanged: () -> Unit,
    onMessage: (String) -> Unit,
    onEditRotation: () -> Unit,
    onRotationChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val s = remember { AppSettings(context) }
    // Re-read after returning from system settings (notification or alarm permission).
    var resumed by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { resumed++ } }

    var notifyOn by remember { mutableStateOf(s.notificationsOn) }
    var prayers by remember { mutableStateOf(s.notifyPrayers) }
    var remind by remember { mutableIntStateOf(s.reminderMinutes) }
    var holy by remember { mutableStateOf(s.holyDayNotifications) }
    var overlay by remember { mutableStateOf(s.overlayPosition) }
    var style by remember { mutableStateOf(s.overlayStyle) }
    var follows by remember { mutableStateOf(s.liveFollowsPrayer) }
    var daily by remember { mutableStateOf(s.dailyWallpaper) }
    var source by remember { mutableStateOf(s.dailySource) }
    var target by remember { mutableStateOf(s.dailyTarget) }
    var theme by remember { mutableStateOf(s.themePalette) }
    var rotating by remember { mutableStateOf(RotationAlarms.active(context)) }
    val canExact = remember(resumed) { PrayerAlarms.canExact(context) }
    val permitted = remember(resumed) { Notifications.permitted(context) }

    fun applyAlarms() = Background.executor.execute { PrayerAlarms.reschedule(context) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            notifyOn = true
            s.notificationsOn = true
            Notifications.ensureChannels(context)
            applyAlarms()
        } else {
            onMessage("Bildirim izni verilmedi. Ayarlardan açabilirsin.")
        }
    }

    NoorSheet("Ayarlar", onDismiss) {
        SectionTitle("Konum")
        NoorCard {
            OptionRow(
                s.location?.label ?: "Seçilmedi",
                icon = Icons.Filled.LocationOn,
                subtitle = "Vakitler ve kıble bu yere göre",
                onClick = onPickLocation,
                trailing = { Text("Değiştir", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) },
            )
        }

        SectionTitle("Bildirimler")
        NoorCard {
            SwitchRow("Vakit girince bildir", "İstersen vakitten önce de hatırlatır.", notifyOn) { on ->
                if (on && Build.VERSION.SDK_INT >= 33 && !Notifications.permitted(context)) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notifyOn = on
                    s.notificationsOn = on
                    if (on) Notifications.ensureChannels(context)
                    applyAlarms()
                }
            }
            if (notifyOn) {
                if (!permitted) {
                    Hint("Bildirimler sistemde kapalı.") {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
                if (!canExact) {
                    Hint("Bildirimlerin dakikası dakikasına gelmesi için \"Alarmlar ve hatırlatıcılar\" iznini ver.") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                }
                FieldLabel("Hangi vakitler")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in Prayer.entries) {
                        NoorChip(p.title, p in prayers) {
                            prayers = if (p in prayers) prayers - p else prayers + p
                            s.notifyPrayers = prayers
                            applyAlarms()
                        }
                    }
                }
                FieldLabel("Önceden hatırlat")
                ChoiceChips(listOf(0, 5, 10, 15, 20, 30, 45).map { it to if (it == 0) "Hayır" else "$it dk" }, remind) { m ->
                    remind = m; s.reminderMinutes = m; applyAlarms()
                }
                SwitchRow("Kandil ve bayram tebrikleri", "Kandil gecesi akşam ezanında, bayram sabahı güneş doğarken.", holy) {
                    holy = it; s.holyDayNotifications = it
                }
                TextButton(onClick = { Notifications.test(context) }) { Text("Deneme bildirimi gönder") }
            }
        }

        SectionTitle("Duvar kağıdı")
        NoorCard {
            OptionRow(
                "Döngü",
                icon = Icons.Filled.Refresh,
                subtitle = if (rotating) {
                    "${RotationAlarms.designs(context).size} tasarım, ${s.rotationInterval.title.replaceFirstChar { it.lowercase() }} değişiyor" +
                        if (s.rotationLive) " · canlı" else ""
                } else {
                    "10 taneye kadar duvar kağıdı seç, sırayla değişsin"
                },
                onClick = onEditRotation,
                trailing = {
                    if (rotating) {
                        TextButton(onClick = {
                            RotationAlarms.stop(context)
                            rotating = false
                            onRotationChanged()
                            onMessage("Döngü durduruldu")
                        }) { Text("Durdur") }
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
            SwitchRow("Her gün yeni duvar kağıdı", "Kendiliğinden yeni bir tasarım; doğum gününde sana özel olanı. Döngünün yerini alır.", daily) {
                daily = it; s.dailyWallpaper = it
                if (it && rotating) {
                    RotationAlarms.stop(context)
                    rotating = false
                    onRotationChanged()
                }
                Work.ensure(context)
                if (it) {
                    Work.dailyNow(context)
                    onMessage("İlk duvar kağıdı hazırlanıyor… ✨")
                }
            }
            if (daily) {
                FieldLabel("Nereden seçilsin")
                ChoiceChips(DailySource.entries.map { it to it.title }, source) { source = it; s.dailySource = it }
                FieldLabel("Nereye")
                ChoiceChips(listOf("HOME" to "Ana ekran", "LOCK" to "Kilit ekranı", "BOTH" to "İkisi de"), target) { target = it; s.dailyTarget = it }
                TextButton(onClick = { Work.dailyNow(context); onMessage("Yeni duvar kağıdı hazırlanıyor… ✨") }) { Text("Şimdi değiştir") }
            }
        }

        SectionTitle("Canlı duvar kağıdı")
        NoorCard {
            FieldLabel("Vakit paneli")
            ChoiceChips(listOf<Pair<OverlayPosition?, String>>(null to "Kapalı") + OverlayPosition.entries.map { it to it.title }, overlay) {
                overlay = it; s.overlayPosition = it
            }
            if (overlay != null) {
                ChoiceChips(
                    OverlayStyle.entries.map { it to if (it == OverlayStyle.COMPACT) "Sade: sıradaki vakit" else "Ayrıntılı: altı vakit" },
                    style,
                ) { style = it; s.overlayStyle = it }
            }
            SwitchRow("Vakte göre değişen tasarım", "Seherde şafak, öğlende çini, akşamda gün batımı, gece yıldızlar.", follows) {
                follows = it; s.liveFollowsPrayer = it
                // The live Döngü would outrank the day cycle, so one replaces the other.
                if (it && rotating && s.rotationLive) {
                    RotationAlarms.stop(context)
                    rotating = false
                    onRotationChanged()
                    onMessage("Canlı döngü durduruldu; tasarım artık vakte göre değişecek")
                }
            }
            FilledTonalButton(onClick = {
                try {
                    context.startActivity(Wallpapers.liveWallpaperIntent(context, Prefs(context).liveSelection).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    onMessage("Bu cihaz canlı duvar kağıdını desteklemiyor")
                }
            }) { Text("Canlı duvar kağıdını kur") }
        }

        SectionTitle("Görünüm")
        NoorCard {
            FieldLabel("Uygulamanın renkleri")
            val palettes = remember { listOf<Palette?>(null) + Palette.ALL.filter { it.id != "emerald" } + s.customPalettes.map { Palette.byId(it) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in palettes) {
                    val selected = theme == p?.id
                    val colors = if (p == null) listOf(Color(0xFF0D4633), Color(0xFFD9B54A), Color(0xFF02140E))
                    else listOf(Color(p.bgTop), Color(p.line), Color(p.bgBottom))
                    Box(
                        Modifier
                            .size(40.dp)
                            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) colors[1] else Color.White.copy(alpha = 0.3f)), CircleShape)
                            .padding(4.dp)
                            .background(Brush.linearGradient(colors), CircleShape)
                            .clickable {
                                theme = p?.id
                                s.themePalette = p?.id
                                if (p?.id == "betul") HanifeBetul.find(context, HanifeBetul.Surprise.THEME)?.let(onMessage)
                                onThemeChanged()
                            },
                    )
                }
            }
            Text(
                (palettes.firstOrNull { it?.id == theme }?.name ?: "Zümrüt ve Altın (ilk hali)"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("Ana ekran widget'ı")
        NoorCard {
            Text(
                "Sıradaki vakti ve kalan süreyi gösterir. Arkasındaki tasarımı bir duvar kağıdının \"Duvar kağıdı yap\" menüsünden seçebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val manager = remember { AppWidgetManager.getInstance(context) }
            if (manager.isRequestPinAppWidgetSupported) {
                FilledTonalButton(onClick = {
                    manager.requestPinAppWidget(ComponentName(context, PrayerWidget::class.java), null, null)
                }) { Text("Widget'ı ana ekrana ekle") }
            }
        }

        Footnote(
            "Vakitler: T.C. Diyanet İşleri Başkanlığı (ezanvakti hizmeti aracılığıyla). İnternet yokken vakitler Diyanet " +
                "yöntemiyle hesaplanır ve Diyanet'in tablosuyla karşılaştırılarak düzeltilir.\n" +
                "Gerçek Camiler: Wikimedia Commons; her fotoğraf kendi fotoğrafçısının özgür lisansıyla (CC BY, CC BY-SA, CC0).\n" +
                "HBSnoor · Sürüm ${rememberVersionName()} · ${HanifeBetul.NAME} için ♡",
            Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    OptionRow(title, subtitle = subtitle, onClick = { onChange(!checked) }, trailing = { Switch(checked = checked, onCheckedChange = onChange) })
}

@Composable
private fun Hint(text: String, onFix: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, Noor.Tile)
            .clickable(onClick = onFix)
            .padding(12.dp),
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("Aç ›", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
    }
}
