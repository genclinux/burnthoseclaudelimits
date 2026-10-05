package com.noor.wallpapers.ui

import android.Manifest
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.prayer.Qibla
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.service.LocationFinder
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Phone heading in degrees from true north, plus whether the compass needs calibrating. */
private class Heading(val degrees: Float, val unreliable: Boolean)

/** Kıble: a compass that turns with the phone, the Kaaba marked on its rim. */
@Composable
fun QiblaScreen(onMessage: (String) -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val schedule = rememberSchedule()
    val place = schedule?.location?.let { l -> if (l.hasCoordinates) l.latitude!! to l.longitude!! else null }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Noor.Gap),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            ScreenHeader("Kıble", subtitle = schedule?.location?.label, onSettings = onOpenSettings)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Noor.Gap),
                modifier = Modifier.widthIn(max = Noor.MaxWidth).padding(horizontal = Noor.Gutter),
            ) {
            if (place == null) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Kıble için bir yer gerekiyor; sağ üstteki ⚙ Ayarlar'dan seçebilirsin.",
                    textAlign = TextAlign.Center,
                )

            } else {
                val (lat, lon) = place
                val bearing = remember(place) { Qibla.bearing(lat, lon) }
                val distance = remember(place) { Qibla.distanceKm(lat, lon) }
                Compass(lat, lon, bearing, distance, onMessage)
                Text(
                    "Kıble açısı ${bearing.roundToInt()}° · ${Qibla.compassName(bearing)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Kâbe'ye ${"%,d".format(TurkishText.TR, distance.toLong())} km",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                schedule?.day(schedule.today(java.time.Instant.now()))?.qiblaTime?.let { q ->
                    InfoCard(
                        "Kıble saati ${TurkishText.hhmm(q)}",
                        "Pusula şaşırırsa: bugün ${TurkishText.at(q)} güneş tam kıble yönündedir. O an güneşe dönen kıbleye dönmüş olur.",
                    )
                }

                Text(
                    "Telefonu yere paralel tut, metal ve mıknatıslardan uzak dur. Kâbe simgesi üstteki işarete gelince kıbleye dönmüşsün demektir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            }
        }
    }
}

@Composable
private fun Compass(lat: Double, lon: Double, qibla: Double, distanceKm: Double, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var heading by remember { mutableStateOf<Heading?>(null) }
    var available by remember { mutableStateOf(true) }
    val declination = remember(lat, lon) {
        GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination
    }

    DisposableEffect(declination) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: sm?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        if (sm == null || sensor == null) {
            available = false
            return@DisposableEffect onDispose { }
        }
        val rot = FloatArray(9)
        val out = FloatArray(9)
        val orient = FloatArray(3)
        var sx = 0.0
        var sy = 0.0
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                when (view.display?.rotation ?: Surface.ROTATION_0) {
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, out)
                    Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, out)
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, out)
                    else -> System.arraycopy(rot, 0, out, 0, 9)
                }
                SensorManager.getOrientation(out, orient)
                val a = orient[0].toDouble()
                // Smooth on the unit circle so 359° → 1° doesn't swing the long way round.
                sx += 0.15 * (cos(a) - sx)
                sy += 0.15 * (sin(a) - sy)
                val deg = (Math.toDegrees(atan2(sy, sx)) + declination + 360) % 360
                heading = Heading(deg.toFloat(), accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW)
            }

            override fun onAccuracyChanged(s: Sensor, a: Int) {
                accuracy = a
            }
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    val h = heading
    val turn = if (h != null) Qibla.turn(h.degrees.toDouble(), qibla) else 999.0
    val aligned = abs(turn) < 3.0
    var buzzed by remember { mutableIntStateOf(0) }
    LaunchedEffect(aligned) {
        if (aligned) {
            view.performHapticFeedback(
                if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
            )
            if (buzzed++ == 0) {
                onMessage(HanifeBetul.qiblaMessage(distanceKm))
                HanifeBetul.find(context, HanifeBetul.Surprise.QIBLA)?.let(onMessage)
            }
        }
    }

    val gold = MaterialTheme.colorScheme.primary
    val text = MaterialTheme.colorScheme.onBackground
    val measurer = rememberTextMeasurer()
    val dialHeading = h?.degrees ?: 0f
    Box(Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2 * 0.92f
            if (aligned) {
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.45f), Color.Transparent), c, r * 1.1f), r * 1.1f, c)
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFF0B2A2A), Color(0xFF041014)), c, r), r, c)
            drawCircle(gold, r, c, style = Stroke(width = 3.dp.toPx()))
            drawCircle(gold.copy(alpha = 0.4f), r * 0.86f, c, style = Stroke(width = 1.dp.toPx()))

            // The dial turns against the phone, so north stays north.
            rotate(-dialHeading, c) {
                for (d in 0 until 360 step 5) {
                    val long = d % 30 == 0
                    val a = Math.toRadians(d.toDouble() - 90)
                    val r0 = if (long) r * 0.86f else r * 0.91f
                    drawLine(
                        if (long) gold else text.copy(alpha = 0.4f),
                        Offset(c.x + (r0 * cos(a)).toFloat(), c.y + (r0 * sin(a)).toFloat()),
                        Offset(c.x + (r * 0.97f * cos(a)).toFloat(), c.y + (r * 0.97f * sin(a)).toFloat()),
                        strokeWidth = if (long) 2.dp.toPx() else 1.dp.toPx(),
                    )
                }
                for ((label, d) in listOf("K" to 0, "D" to 90, "G" to 180, "B" to 270)) {
                    val a = Math.toRadians(d - 90.0)
                    val layout = measurer.measure(label, TextStyle(color = if (d == 0) gold else text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    val p = Offset(c.x + (r * 0.72f * cos(a)).toFloat(), c.y + (r * 0.72f * sin(a)).toFloat())
                    // Letters stay upright as the dial turns.
                    withTransform({ rotate(dialHeading, p) }) {
                        drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                    }
                }
                // The Kaaba on the rim, and a gilded line to it.
                val qa = Math.toRadians(qibla - 90)
                val qp = Offset(c.x + (r * 0.74f * cos(qa)).toFloat(), c.y + (r * 0.74f * sin(qa)).toFloat())
                drawLine(gold.copy(alpha = 0.8f), c, qp, strokeWidth = 3.dp.toPx())
                kaaba(qp, 26.dp.toPx(), dialHeading, gold)
            }
            star(c, r * 0.12f, gold)
            // Fixed pointer: where the phone is facing.
            val tip = Offset(c.x, c.y - r * 1.04f)
            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - 11.dp.toPx(), tip.y - 16.dp.toPx())
                    lineTo(tip.x + 11.dp.toPx(), tip.y - 16.dp.toPx())
                    close()
                },
                if (aligned) gold else text,
            )
        }
    }
    when {
        !available -> Text("Bu cihazda pusula yok; kıble saatini kullanabilirsin.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        h == null -> Text("Pusula hazırlanıyor…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        aligned -> Text("Kıbleye döndün 🕋", style = MaterialTheme.typography.titleLarge, color = gold)
        else -> Text(
            if (turn > 0) "${turn.roundToInt()}° sağa dön ›" else "‹ ${(-turn).roundToInt()}° sola dön",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (h?.unreliable == true) {
        Text(
            "Pusula kalibre değil: telefonu havada birkaç kez 8 çizer gibi çevir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

/** A small Kaaba: black cube with its gold band, kept upright. */
private fun DrawScope.kaaba(at: Offset, s: Float, dialHeading: Float, gold: Color) {
    withTransform({ rotate(dialHeading, at) }) {
        drawRect(Color(0xFF111111), Offset(at.x - s / 2, at.y - s / 2), Size(s, s))
        drawRect(gold, Offset(at.x - s / 2, at.y - s / 2 + s * 0.22f), Size(s, s * 0.12f))
        drawRect(gold.copy(alpha = 0.7f), Offset(at.x - s / 2, at.y - s / 2), Size(s, s), style = Stroke(width = 1.5f))
    }
}

/** An eight-pointed star at the centre of the dial. */
private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 16) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + PI * i / 8
        val x = c.x + (rr * cos(a)).toFloat()
        val y = c.y + (rr * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}
