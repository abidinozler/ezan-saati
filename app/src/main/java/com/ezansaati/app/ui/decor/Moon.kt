package com.ezansaati.app.ui.decor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.ezansaati.app.calendar.Lunar

val MOON_LIGHT = Color(0xFFF4EBCB)

/** Ayın o anki evresini çizer. */
@Composable
fun MoonPhase(ageDays: Double, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2 * 0.8f
        val c = Offset(size.width / 2, size.height / 2)

        drawCircle(
            Brush.radialGradient(listOf(MOON_LIGHT.copy(alpha = 0.25f), Color.Transparent), c, r * 1.6f),
            radius = r * 1.6f, center = c,
        )
        drawCircle(Color.White.copy(alpha = 0.12f), r, c)

        val lit = Path()
        Lunar.litOutline(ageDays).forEachIndexed { i, (x, y) ->
            if (i == 0) lit.moveTo(c.x + x * r, c.y + y * r) else lit.lineTo(c.x + x * r, c.y + y * r)
        }
        lit.close()
        drawPath(lit, MOON_LIGHT)
    }
}
