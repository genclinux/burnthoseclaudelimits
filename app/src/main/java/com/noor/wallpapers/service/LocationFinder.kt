package com.noor.wallpapers.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.noor.wallpapers.prayer.Diyanet
import com.noor.wallpapers.prayer.Place
import com.noor.wallpapers.prayer.PrayerLocation
import com.noor.wallpapers.prayer.Provinces
import com.noor.wallpapers.prayer.TurkishText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * "Konumumu bul": a coarse location fix, turned into a district in Diyanet's
 * lists (through the geocoder where the phone has one), so the times are
 * Diyanet's own and not a calculation.
 */
object LocationFinder {
    fun permitted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** A recent last-known fix, or a fresh one (up to 20 s). Null without permission or providers. */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): Location? {
        if (!permitted(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = (providers + LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60_000L) return last
        val provider = providers.firstOrNull() ?: return last
        val fresh = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    lm.getCurrentLocation(provider, signal, Executors.newSingleThreadExecutor()) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                } else {
                    // Android 10 still declares every LocationListener method abstract, so implement them all.
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (cont.isActive) cont.resume(location)
                        }

                        @Deprecated("Deprecated in Android")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                    }
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }
            }
        }
        return fresh ?: last
    }

    /** Address for a point, in Turkish, or null where the phone has no geocoder or is offline. */
    suspend fun address(context: Context, lat: Double, lon: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, TurkishText.TR)
        return withTimeoutOrNull(10_000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        lat, lon, 1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        },
                    )
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
    }

    /** Coordinates for a place name ("Köln, Almanya"), or null without a geocoder or a match. */
    suspend fun geocode(context: Context, query: String): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, TurkishText.TR)
        val address: Address? = withTimeoutOrNull(10_000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(
                        query, 1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        },
                    )
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching { geocoder.getFromLocationName(query, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
        return address?.let { it.latitude to it.longitude }
    }

    /**
     * Turns a fix into a place. In Türkiye: the province (from the geocoder or
     * the nearest centre) and the district by name in Diyanet's list, falling
     * back to the province centre. Abroad: Diyanet's list where names match,
     * otherwise a calculated place. The fix's own coordinates are kept for the
     * qibla and for calculating days Diyanet's table doesn't cover.
     */
    suspend fun resolve(context: Context, lat: Double, lon: Double): PrayerLocation {
        val address = address(context, lat, lon)
        val inTurkey = address?.countryCode?.equals("TR", ignoreCase = true) ?: Provinces.isInTurkey(lat, lon)
        if (inTurkey) {
            val province = address?.adminArea?.let(Provinces::byName) ?: Provinces.nearest(lat, lon)
            val districtName = address?.subAdminArea?.takeIf { it.isNotBlank() } ?: province.name
            val offline = Provinces.location(province).copy(district = districtName, latitude = lat, longitude = lon)
            return runCatching {
                val turkey = DiyanetApi.countries(context).first(Diyanet::isTurkey)
                val city = DiyanetApi.cities(context, turkey.id).first { Provinces.byName(it.name)?.plate == province.plate }
                val districts = DiyanetApi.districts(context, city.id, city.name)
                val district = match(districts, districtName) ?: districts.first()
                offline.copy(
                    countryId = turkey.id, country = turkey.title, cityId = city.id, city = city.title,
                    districtId = district.id, district = district.title,
                )
            }.getOrDefault(offline)
        }
        val zone = ZoneId.systemDefault().id
        val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Konumum"
        val calculated = PrayerLocation(
            countryId = null, country = address?.countryName ?: "", cityId = null, city = address?.adminArea ?: name,
            districtId = null, district = name, latitude = lat, longitude = lon, zoneId = zone,
        )
        if (address == null) return calculated
        return runCatching {
            val country = match(DiyanetApi.countries(context), address.countryName ?: "")!!
            val cities = DiyanetApi.cities(context, country.id)
            val city = listOfNotNull(address.adminArea, address.locality, address.subAdminArea).firstNotNullOf { match(cities, it) }
            val districts = DiyanetApi.districts(context, city.id, city.name)
            val district = listOfNotNull(address.locality, address.subAdminArea).firstNotNullOfOrNull { match(districts, it) } ?: districts.first()
            calculated.copy(
                countryId = country.id, country = country.title, cityId = city.id, city = city.title,
                districtId = district.id, district = district.title,
            )
        }.getOrDefault(calculated)
    }

    private fun match(places: List<Place>, name: String): Place? {
        val key = TurkishText.matchKey(name)
        if (key.isEmpty()) return null
        return places.firstOrNull { TurkishText.matchKey(it.name) == key }
            ?: places.firstOrNull { TurkishText.matchKey(it.name).let { k -> k.length >= 4 && (k.startsWith(key) || key.startsWith(k)) } }
    }
}
