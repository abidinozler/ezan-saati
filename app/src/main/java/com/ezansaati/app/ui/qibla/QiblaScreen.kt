package com.ezansaati.app.ui.qibla

import android.hardware.GeomagneticField
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ezansaati.app.R
import com.ezansaati.app.data.SavedLocation
import com.ezansaati.app.qibla.QiblaCalculator
import com.ezansaati.app.sensor.CompassReading
import com.ezansaati.app.sensor.CompassSensor
import com.ezansaati.app.ui.theme.Gold
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ALIGN_TOLERANCE = 3f

@Composable
fun QiblaScreen(location: SavedLocation?) {
    if (location == null) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.qibla_no_location), textAlign = TextAlign.Center)
        }
        return
    }

    val context = LocalContext.current
    val compass = remember { CompassSensor(context) }
    val qibla = remember(location) {
        QiblaCalculator.qiblaDirection(location.latitude, location.longitude).toFloat()
    }
    val distance = remember(location) {
        QiblaCalculator.distanceKm(location.latitude, location.longitude)
    }
    // Pusula manyetik kuzeyi gösterir; kıble açısı gerçek kuzeye göredir. Fark: manyetik sapma.
    val declination = remember(location) {
        GeomagneticField(
            location.latitude.toFloat(), location.longitude.toFloat(), 0f, System.currentTimeMillis(),
        ).declination
    }
    val reading: CompassReading? by remember { compass.readings() }
        .collectAsStateWithLifecycle(initialValue = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.qibla_angle, "%.1f".format(Locale.ROOT, qibla)), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.qibla_distance, "%,d".format(distance.roundToInt())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!compass.isAvailable) {
            WarningCard(stringResource(R.string.qibla_no_sensor))
            return@Column
        }

        val current = reading
        if (current == null) {
            Text(stringResource(R.string.qibla_starting))
            return@Column
        }

        val heading = (current.azimuth + declination + 360) % 360
        val diff = normalize180(qibla - heading)
        val aligned = abs(diff) < ALIGN_TOLERANCE

        val haptic = LocalHapticFeedback.current
        LaunchedEffect(aligned) {
            if (aligned) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        CompassDial(
            heading = heading,
            qibla = qibla,
            aligned = aligned,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
        )

        Text(
            when {
                aligned -> stringResource(R.string.qibla_aligned)
                diff > 0 -> stringResource(R.string.qibla_turn_right, abs(diff).roundToInt())
                else -> stringResource(R.string.qibla_turn_left, abs(diff).roundToInt())
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )

        if (current.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
            current.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
        ) {
            WarningCard(stringResource(R.string.qibla_low_accuracy))
        }

        Text(
            stringResource(R.string.qibla_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompassDial(heading: Float, qibla: Float, aligned: Boolean, modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val needleColor = if (aligned) MaterialTheme.colorScheme.primary else Gold
    val north = Color(0xFFD32F2F)
    val labelStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
    val cardinals = listOf(
        stringResource(R.string.compass_n) to 0f,
        stringResource(R.string.compass_e) to 90f,
        stringResource(R.string.compass_s) to 180f,
        stringResource(R.string.compass_w) to 270f,
    )

    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.88f

        fun pointAt(angleDeg: Float, r: Float) = Offset(
            center.x + r * sin(Math.toRadians(angleDeg.toDouble())).toFloat(),
            center.y - r * cos(Math.toRadians(angleDeg.toDouble())).toFloat(),
        )

        // Kadran telefonla birlikte döner: kuzey her zaman gerçek kuzeyi gösterir.
        rotate(degrees = -heading, pivot = center) {
            drawDialOrnament(center, radius)
            drawCircle(outline, radius, center, style = Stroke(width = 3.dp.toPx()))
            for (deg in 0 until 360 step 10) {
                val major = deg % 90 == 0
                val inner = radius * if (major) 0.86f else 0.93f
                drawLine(
                    color = if (deg == 0) north else outline,
                    start = pointAt(deg.toFloat(), inner),
                    end = pointAt(deg.toFloat(), radius),
                    strokeWidth = (if (major) 3f else 1.5f).dp.toPx(),
                )
            }
            cardinals.forEach { (label, deg) ->
                val layout = textMeasurer.measure(
                    label, if (deg == 0f) labelStyle.copy(color = north) else labelStyle,
                )
                val p = pointAt(deg, radius * 0.74f)
                drawText(layout, topLeft = p - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }

            // Kıble iğnesi ve Kâbe işareti
            val kaaba = pointAt(qibla, radius * 0.55f)
            drawLine(
                needleColor, center, pointAt(qibla, radius * 0.48f),
                strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round,
            )
            drawKaaba(kaaba, 26.dp.toPx(), glow = if (aligned) 0.55f else 0.3f)
            drawCircle(needleColor, 7.dp.toPx(), center)
            drawCircle(Color(0xFF0B1222), 3.dp.toPx(), center)
        }

        // Sabit işaret: telefonun üst kenarının baktığı yön.
        val tip = Offset(center.x, center.y - radius - 4.dp.toPx())
        val w = 10.dp.toPx()
        val marker = Path().apply {
            moveTo(tip.x, tip.y + 2 * w)
            lineTo(tip.x - w, tip.y)
            lineTo(tip.x + w, tip.y)
            close()
        }
        drawPath(marker, if (aligned) needleColor else onSurface)
    }
}

/** Kadran zemini: koyu cam, altın halkalar ve sekiz köşeli yıldız (Rub-ül Hizb) motifi. */
private fun DrawScope.drawDialOrnament(center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(listOf(Color(0xCC15233D), Color(0xE6070D1C)), center, radius),
        radius, center,
    )
    drawCircle(Gold.copy(alpha = 0.8f), radius + 7.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
    drawCircle(Gold.copy(alpha = 0.35f), radius + 11.dp.toPx(), center, style = Stroke(1.dp.toPx()))

    fun polar(angleDeg: Double, r: Float) = Offset(
        center.x + r * sin(Math.toRadians(angleDeg)).toFloat(),
        center.y - r * cos(Math.toRadians(angleDeg)).toFloat(),
    )

    // Sekiz köşeli yıldız: 45° döndürülmüş iki kare
    val starR = radius * 0.5f
    listOf(0.0, 45.0).forEach { offset ->
        val square = Path().apply {
            (0 until 4).forEach { i ->
                val p = polar(offset + i * 90, starR)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(square, Gold.copy(alpha = 0.06f))
        drawPath(square, Gold.copy(alpha = 0.35f), style = Stroke(1.2.dp.toPx()))
    }
    drawCircle(Gold.copy(alpha = 0.3f), starR * 0.55f, center, style = Stroke(1.dp.toPx()))

    // Harflerin içinde küçük baklava dizisi
    val d = 3.dp.toPx()
    for (deg in 15 until 360 step 30) {
        val p = polar(deg.toDouble(), radius * 0.62f)
        val diamond = Path().apply {
            moveTo(p.x, p.y - d)
            lineTo(p.x + d * 0.6f, p.y)
            lineTo(p.x, p.y + d)
            lineTo(p.x - d * 0.6f, p.y)
            close()
        }
        drawPath(diamond, Gold.copy(alpha = 0.55f))
    }
}

/** Kâbe: siyah örtü, altın kuşak (hizam) ve kapı, arkasında hafif ışıma. */
private fun DrawScope.drawKaaba(center: Offset, box: Float, glow: Float) {
    drawCircle(
        Brush.radialGradient(listOf(Gold.copy(alpha = glow), Color.Transparent), center, box * 1.3f),
        box * 1.3f, center,
    )
    val topLeft = center - Offset(box / 2, box / 2)
    val corner = CornerRadius(box * 0.08f)
    drawRoundRect(Color(0xFF111111), topLeft, Size(box, box), corner)
    drawRoundRect(Gold.copy(alpha = 0.6f), topLeft, Size(box, box), corner, style = Stroke(1.dp.toPx()))
    drawRect(Gold, topLeft + Offset(0f, box * 0.22f), Size(box, box * 0.12f))
    drawRect(Gold.copy(alpha = 0.85f), topLeft + Offset(box * 0.6f, box * 0.55f), Size(box * 0.2f, box * 0.33f))
}

@Composable
private fun WarningCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(text, Modifier.padding(16.dp))
    }
}

/** Açıyı -180..180 aralığına getirir (pozitif: sağa/saat yönüne dönülmeli). */
private fun normalize180(angle: Float): Float {
    var a = angle % 360
    if (a > 180) a -= 360
    if (a < -180) a += 360
    return a
}
