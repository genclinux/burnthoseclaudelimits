package com.noor.wallpapers.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noor.wallpapers.prayer.Diyanet
import com.noor.wallpapers.prayer.Place
import com.noor.wallpapers.prayer.PrayerLocation
import com.noor.wallpapers.prayer.Province
import com.noor.wallpapers.prayer.Provinces
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.service.DiyanetApi
import com.noor.wallpapers.service.LocationFinder
import com.noor.wallpapers.service.PrayerAlarms
import com.noor.wallpapers.service.PrayerRepository
import com.noor.wallpapers.service.PrayerWidget
import com.noor.wallpapers.service.Work
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Step { COUNTRY, CITY, DISTRICT, OFFLINE }

/**
 * Choose where the times are for: Diyanet's own lists (country, city,
 * district), "find my location", or, with no internet, the 81 provinces.
 */
@Composable
fun LocationPicker(onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.CITY) }
    var country by remember { mutableStateOf<Place?>(null) }
    var city by remember { mutableStateOf<Place?>(null) }
    var items by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    fun save(loc: PrayerLocation) {
        scope.launch {
            busy = "${loc.label} kaydediliyor…"
            onDone(choose(context, loc))
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            scope.launch {
                busy = "Konumun bulunuyor…"
                val fix = LocationFinder.current(context)
                if (fix == null) {
                    busy = null
                    error = "Konum alınamadı. Konum servisi açık mı? Listeden de seçebilirsin."
                } else {
                    busy = "Diyanet listesinde aranıyor…"
                    save(LocationFinder.resolve(context, fix.latitude, fix.longitude))
                }
            }
        } else {
            error = "Konum izni verilmedi; listeden seçebilirsin."
        }
    }

    LaunchedEffect(step, country, city, attempt) {
        query = ""
        if (step == Step.OFFLINE) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            items = when (step) {
                Step.COUNTRY -> DiyanetApi.countries(context)
                Step.CITY -> {
                    val c = country ?: DiyanetApi.countries(context).first(Diyanet::isTurkey).also { country = it }
                    DiyanetApi.cities(context, c.id)
                }
                Step.DISTRICT -> DiyanetApi.districts(context, city!!.id, city!!.name)
                Step.OFFLINE -> emptyList()
            }
        } catch (e: Exception) {
            items = emptyList()
            error = "Diyanet'in listesine ulaşılamadı (${e.message ?: "bağlantı yok"})."
        } finally {
            loading = false
        }
    }

    fun back() {
        if (step == Step.CITY) onDismiss() else step = Step.CITY
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    IconButton(onClick = ::back) {
                        Icon(if (step == Step.CITY) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (step) {
                                Step.COUNTRY -> "Ülke seç"
                                Step.CITY -> "Şehir seç"
                                Step.DISTRICT -> "İlçe seç"
                                Step.OFFLINE -> "İl seç (internetsiz)"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val crumb = listOfNotNull(country?.title, city?.title.takeIf { step == Step.DISTRICT }).joinToString(" › ")
                        if (crumb.isNotEmpty() && step != Step.OFFLINE) {
                            Text(crumb, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (step == Step.CITY) TextButton(onClick = { step = Step.COUNTRY }) { Text("Başka ülke") }
                }

                val working = busy
                if (working != null) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(working, textAlign = TextAlign.Center)
                    }
                } else {
                    PickerBody(
                        step, query, { query = it }, loading, error, items, city,
                        onFindMe = { permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
                        onRetry = { attempt++ },
                        onOffline = { error = null; step = Step.OFFLINE },
                        onProvince = { save(Provinces.location(it)) },
                        onPlace = { p ->
                            when (step) {
                                Step.COUNTRY -> { country = p; city = null; step = Step.CITY }
                                Step.CITY -> { city = p; step = Step.DISTRICT }
                                Step.DISTRICT -> scope.launch {
                                    busy = "${p.title} kaydediliyor…"
                                    save(diyanetLocation(context, country!!, city!!, p))
                                }
                                Step.OFFLINE -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerBody(
    step: Step,
    query: String,
    onQuery: (String) -> Unit,
    loading: Boolean,
    error: String?,
    items: List<Place>,
    city: Place?,
    onFindMe: () -> Unit,
    onRetry: () -> Unit,
    onOffline: () -> Unit,
    onProvince: (Province) -> Unit,
    onPlace: (Place) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (step == Step.CITY || step == Step.COUNTRY) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFindMe)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Konumumu bul", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Bulunduğun ilçeyi Diyanet'in listesinde seçer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Ara") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null && step != Step.OFFLINE -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Tekrar dene") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOffline) { Text("İnternetsiz: il listesinden seç") }
            }
            step == Step.OFFLINE -> {
                val key = TurkishText.matchKey(query)
                val list = Provinces.SORTED.filter { key.isEmpty() || TurkishText.matchKey(it.name).contains(key) }
                Text(
                    "Vakitler Diyanet yöntemiyle hesaplanır; internete bağlanınca Diyanet'in kendi tablosuna geçilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.plate }) { p: Province ->
                        Row_(p.name, "${p.plate}") { onProvince(p) }
                    }
                }
            }
            else -> {
                val key = TurkishText.matchKey(query)
                val list = items.filter { key.isEmpty() || TurkishText.matchKey(it.name).contains(key) }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { p ->
                        val centre = step == Step.DISTRICT && TurkishText.matchKey(p.name) == TurkishText.matchKey(city?.name ?: "")
                        Row_(p.title, if (centre) "merkez" else null) { onPlace(p) }
                    }
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun Row_(title: String, trailing: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** A Diyanet district with coordinates: the province centre in Türkiye, the geocoder abroad. */
private suspend fun diyanetLocation(context: Context, country: Place, city: Place, district: Place): PrayerLocation {
    val turkey = Diyanet.isTurkey(country)
    val coords = if (turkey) {
        Provinces.byName(city.name)?.let { it.latitude to it.longitude }
    } else {
        LocationFinder.geocode(context, "${district.title}, ${city.title}, ${country.title}")
            ?: LocationFinder.geocode(context, "${city.title}, ${country.title}")
    }
    return PrayerLocation(
        countryId = country.id, country = country.title, cityId = city.id, city = city.title,
        districtId = district.id, district = district.title,
        latitude = coords?.first, longitude = coords?.second,
        zoneId = if (turkey) Provinces.TURKEY_ZONE else null,
    )
}

/** Saves the place, fetches its table, and sets alarms and the widget going. Returns a message for her. */
private suspend fun choose(context: Context, loc: PrayerLocation): String {
    PrayerRepository.setLocation(context, loc)
    Work.ensure(context)
    val message = try {
        val days = PrayerRepository.refresh(context, force = true)
        if (days > 0) "${loc.label} için Diyanet vakitleri alındı ✓" else "${loc.label} seçildi"
    } catch (_: Exception) {
        Work.refreshNow(context)
        "${loc.label} seçildi. Diyanet'e ulaşılamadı; şimdilik vakitler hesaplanıyor."
    }
    withContext(Dispatchers.Default) {
        PrayerAlarms.reschedule(context)
        PrayerWidget.updateAll(context)
    }
    return message
}
