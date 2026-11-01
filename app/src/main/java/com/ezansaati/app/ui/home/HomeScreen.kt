package com.ezansaati.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ezansaati.app.UiState
import com.ezansaati.app.calendar.Lunar
import com.ezansaati.app.ui.decor.MoonPhase
import com.ezansaati.app.ui.theme.Glass
import com.ezansaati.app.ui.theme.Gold
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.nextPrayer
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr-TR")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TURKISH)
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HomeScreen(
    state: UiState,
    onDayChanged: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val location = state.location
    val today = state.today
    val tomorrow = state.tomorrow
    if (location == null || today == null || tomorrow == null) {
        NoLocation(onGoToSettings)
        return
    }

    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000 - System.currentTimeMillis() % 1000 + 5)
        }
    }
    val currentDate = now.toLocalDate()
    LaunchedEffect(currentDate) {
        if (currentDate != today.date) onDayChanged()
    }

    val next = nextPrayer(now, today, tomorrow)
    val remaining = Duration.between(now, next.nextTime).seconds.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val moonAge = Lunar.moonAgeDays(now.toInstant())
        val hijri = remember(currentDate) { Lunar.hijriDate(currentDate) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(6.dp))
                    Text(location.name, style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    now.format(DATE_FORMAT),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hijri != null) {
                    Text(hijri, style = MaterialTheme.typography.bodyMedium, color = Gold)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MoonPhase(moonAge, Modifier.size(56.dp))
                Text(
                    Lunar.phaseName(moonAge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Glass, contentColor = Color.White),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${next.next.dativeName} kalan süre", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "%02d:%02d:%02d".format(remaining / 3600, remaining % 3600 / 60, remaining % 60),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${next.next.displayName}: ${next.nextTime.format(TIME_FORMAT)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Prayer.entries.forEach { prayer ->
                    PrayerRow(
                        name = prayer.displayName,
                        time = today[prayer].format(TIME_FORMAT),
                        isCurrent = prayer == next.current && currentDate == today.date,
                    )
                }
            }
        }

        Text(
            "Diyanet yöntemiyle hesaplanmıştır. Takvimle fark varsa Ayarlar'dan dakika ayarı yapabilirsiniz.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrayerRow(name: String, time: String, isCurrent: Boolean) {
    val background = if (isCurrent) Gold.copy(alpha = 0.22f) else Color.Transparent
    val content = if (isCurrent) Gold else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
        Text(
            time,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
    }
}

@Composable
private fun NoLocation(onGoToSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Vakitleri hesaplamak için konumunuzu belirleyin.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGoToSettings) { Text("Konum seç") }
    }
}
