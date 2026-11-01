package com.ezansaati.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFC9A54B)

/** Yarı saydam kart zemini: arkadaki gökyüzü görünsün. */
val Glass = Color(0x8C0B1222)

// Arka plan her zaman gökyüzü olduğu için yalnızca koyu tema kullanılır.
private val Colors = darkColorScheme(
    primary = Color(0xFF8CD5C1),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF0F5C4D),
    onPrimaryContainer = Color(0xFFB8EBDD),
    secondary = Color(0xFFE8C66A),
    onSecondary = Color(0xFF3D2E00),
    onSurface = Color(0xFFF2F1EC),
    onSurfaceVariant = Color(0xFFD3D6DE),
    outline = Color(0xFF9AA3B5),
    surfaceContainerHighest = Glass,
    surfaceContainerHigh = Color(0xF0151C2E),
    surfaceContainer = Color(0xF0151C2E),
)

@Composable
fun EzanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
