package com.ezansaati.app.ui.decor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.PrayerTimes
import com.ezansaati.app.prayer.nextPrayer
import com.ezansaati.app.ui.theme.Gold
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.random.Random

/** Vakit aralığına göre gökyüzü renkleri (üst, orta, ufuk). */
private class SkyPalette(val top: Color, val middle: Color, val horizon: Color, val stars: Float)

private val PALETTES = mapOf(
    Prayer.IMSAK to SkyPalette(Color(0xFF111A40), Color(0xFF34386F), Color(0xFF9A6585), 0.6f),
    Prayer.GUNES to SkyPalette(Color(0xFF1F5F9E), Color(0xFF4F8CC2), Color(0xFFD9A06E), 0f),
    Prayer.OGLE to SkyPalette(Color(0xFF14528F), Color(0xFF2E77B5), Color(0xFF6FA9D6), 0f),
    Prayer.IKINDI to SkyPalette(Color(0xFF214E7C), Color(0xFF557CA3), Color(0xFFD29A55), 0f),
    Prayer.AKSAM to SkyPalette(Color(0xFF231640), Color(0xFF7A3157), Color(0xFFD9643C), 0.25f),
    Prayer.YATSI to SkyPalette(Color(0xFF040817), Color(0xFF0B1633), Color(0xFF1A2852), 1f),
)

private val SILHOUETTE = Color(0xF00A0E1C)

/** Şu an hangi vakit aralığında olduğumuzu döndürür; konum yoksa saate göre tahmin eder. */
@Composable
fun rememberCurrentPrayer(today: PrayerTimes?, tomorrow: PrayerTimes?): Prayer {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = ZonedDateTime.now()
        }
    }
    if (today != null && tomorrow != null) return nextPrayer(now, today, tomorrow).current
    return when (now.hour) {
        in 5..6 -> Prayer.IMSAK
        in 7..11 -> Prayer.GUNES
        in 12..14 -> Prayer.OGLE
        in 15..17 -> Prayer.IKINDI
        in 18..19 -> Prayer.AKSAM
        else -> Prayer.YATSI
    }
}

/** Vakte göre renk değiştiren gökyüzü ve yıldızlar. */
@Composable
fun SkyBackground(period: Prayer, modifier: Modifier = Modifier) {
    val palette = PALETTES.getValue(period)
    val spec = tween<Color>(durationMillis = 2000)
    val top by animateColorAsState(palette.top, spec, label = "skyTop")
    val middle by animateColorAsState(palette.middle, spec, label = "skyMiddle")
    val horizon by animateColorAsState(palette.horizon, spec, label = "skyHorizon")
    val starAlpha by animateFloatAsState(palette.stars, tween(2000), label = "stars")
    val stars = remember {
        val random = Random(42)
        List(70) { Triple(random.nextFloat(), random.nextFloat() * 0.6f, 0.5f + random.nextFloat()) }
    }

    Canvas(modifier) {
        drawRect(Brush.verticalGradient(0f to top, 0.55f to middle, 1f to horizon))
        if (starAlpha > 0.01f) {
            stars.forEach { (x, y, scale) ->
                drawCircle(
                    Color.White.copy(alpha = starAlpha * (0.35f + 0.45f * (scale - 0.5f))),
                    radius = scale * 1.4f * density,
                    center = Offset(x * size.width, y * size.height),
                )
            }
        }
    }
}

/** Ekranın altındaki cami silueti; gece pencereleri yanar. */
@Composable
fun MosqueSilhouette(period: Prayer, modifier: Modifier = Modifier) {
    val night = period == Prayer.YATSI || period == Prayer.IMSAK || period == Prayer.AKSAM
    val windowAlpha by animateFloatAsState(if (night) 0.75f else 0f, tween(2000), label = "windows")
    Canvas(modifier) { drawMosque(windowAlpha) }
}

private fun DrawScope.drawMosque(windowAlpha: Float) {
    val h = size.height
    val u = minOf(size.width / 100f, h / 56f)
    val cx = size.width / 2
    fun x(v: Float) = cx + (v - 50f) * u
    fun y(v: Float) = h - v * u

    val path = Path().apply {
        // Zemin
        addRect(Rect(0f, y(2f), size.width, h))
        // Ana gövde, yan kanatlar
        addRect(Rect(x(30f), y(18f), x(70f), h))
        addRect(Rect(x(20f), y(11f), x(30f), h))
        addRect(Rect(x(70f), y(11f), x(80f), h))
        // Ana kubbe ve yan kubbeler
        addArc(x(35f), y(18f), x(65f), 18f * u)
        addArc(x(21f), y(11f), x(29f), 7f * u)
        addArc(x(71f), y(11f), x(79f), 7f * u)
        addArc(x(28f), y(18f), x(36f), 6f * u)
        addArc(x(64f), y(18f), x(72f), 6f * u)
        // Alem
        addRect(Rect(x(49.6f), y(41f), x(50.4f), y(35f)))
        addOval(Rect(Offset(x(50f), y(42f)), 1.1f * u))
        // Minareler
        listOf(10f, 90f).forEach { m -> addMinaret(x(m), h, u) }
    }
    drawPath(path, SILHOUETTE)

    if (windowAlpha > 0.01f) {
        val glow = Gold.copy(alpha = windowAlpha)
        listOf(36f, 43f, 54f, 61f).forEach { wx ->
            drawWindow(x(wx), y(6f), 2.6f * u, 6f * u, glow)
        }
        drawWindow(x(48.5f), y(6f), 3f * u, 8f * u, glow)
        listOf(23.5f, 73.5f).forEach { wx -> drawWindow(x(wx), y(4f), 2f * u, 4f * u, glow) }
    }
}

/** (left, base) üzerine yükselen yarım elips kubbe. */
private fun Path.addArc(left: Float, base: Float, right: Float, height: Float) {
    arcTo(Rect(left, base - height, right, base + height), 180f, 180f, forceMoveTo = true)
    close()
}

private fun Path.addMinaret(center: Float, ground: Float, u: Float) {
    val half = 1.5f * u
    addRect(Rect(center - half, ground - 44f * u, center + half, ground))
    addRect(Rect(center - 2.6f * u, ground - 31.5f * u, center + 2.6f * u, ground - 30f * u))
    addRect(Rect(center - 2.3f * u, ground - 40.5f * u, center + 2.3f * u, ground - 39f * u))
    moveTo(center - half, ground - 44f * u)
    lineTo(center, ground - 54f * u)
    lineTo(center + half, ground - 44f * u)
    close()
}

/** Kemerli pencere: alt köşe (left, bottom), genişlik ve yükseklik. */
private fun DrawScope.drawWindow(left: Float, bottom: Float, width: Float, height: Float, color: Color) {
    val r = width / 2
    val window = Path().apply {
        moveTo(left, bottom)
        lineTo(left, bottom - height + r)
        arcTo(Rect(left, bottom - height, left + width, bottom - height + width), 180f, 180f, false)
        lineTo(left + width, bottom)
        close()
    }
    drawCircle(color.copy(alpha = color.alpha * 0.25f), radius = width * 1.4f, center = Offset(left + r, bottom - height / 2))
    drawPath(window, color)
}
