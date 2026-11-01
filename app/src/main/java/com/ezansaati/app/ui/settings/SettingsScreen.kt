package com.ezansaati.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ezansaati.app.MainViewModel
import com.ezansaati.app.UiState
import com.ezansaati.app.data.City
import com.ezansaati.app.data.TURKEY_CITIES
import com.ezansaati.app.prayer.Prayer
import java.text.Collator
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr-TR")

@Composable
fun SettingsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var showCityPicker by remember { mutableStateOf(false) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.useCurrentLocation()
        else viewModel.showMessage("Konum izni verilmedi. Listeden şehir seçebilirsiniz.")
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- Konum ----
        Section("Konum") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    state.location?.let { "${it.name}  (%.3f, %.3f)".format(it.latitude, it.longitude) }
                        ?: "Konum seçilmedi",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !state.isLocating,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.useCurrentLocation()
                        else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                ) {
                    if (state.isLocating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Konumumu kullan")
                }
                OutlinedButton(onClick = { showCityPicker = true }) { Text("Şehir seç") }
            }
        }

        // ---- Bildirimler ----
        Section("Ezan bildirimleri") {
            if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FilledTonalButton(onClick = {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Bildirim izni ver") }
            }
            Prayer.entries.forEach { prayer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(prayer.displayName, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.notificationsEnabled[prayer] == true,
                        onCheckedChange = { viewModel.setNotificationEnabled(prayer, it) },
                    )
                }
            }
            if (!state.ignoresBatteryOptimization) {
                HorizontalDivider()
                Text(
                    "Bazı telefonlar pil tasarrufu için bildirimleri geciktirir. Zamanında bildirim için pil optimizasyonunu kapatın.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            "package:${context.packageName}".toUri(),
                        )
                    )
                }) { Text("Pil optimizasyonunu kapat") }
            }
        }

        // ---- Kilit ekranı ----
        Section("Kilit ekranı") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Geri sayımı göster", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Kilit ekranında ve bildirim panelinde sıradaki vakte kalan süre ile günün vakitleri. Sessizdir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.lockScreenCountdown,
                    onCheckedChange = viewModel::setLockScreenCountdown,
                )
            }
            if (state.lockScreenCountdown && !state.hasNotificationPermission) {
                Text(
                    "Görünmesi için bildirim izni gerekir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // ---- İnce ayar ----
        Section("Vakit ince ayarı (dakika)") {
            Text(
                "Diyanet takvimiyle karşılaştırın; fark varsa buradan düzeltin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Prayer.entries.forEach { prayer ->
                val offset = state.offsets[prayer] ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(prayer.displayName, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { viewModel.changeOffset(prayer, -1) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        if (offset > 0) "+$offset" else "$offset",
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { viewModel.changeOffset(prayer, +1) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            onDismiss = { showCityPicker = false },
            onSelect = {
                viewModel.selectCity(it)
                showCityPicker = false
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun CityPickerDialog(onDismiss: () -> Unit, onSelect: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    val collator = remember { Collator.getInstance(TURKISH) }
    val cities = remember(query) {
        val q = query.trim().lowercase(TURKISH)
        TURKEY_CITIES
            .filter { it.name.lowercase(TURKISH).contains(q) }
            .sortedWith(compareBy(collator) { it.name })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
        title = { Text("Şehir seç") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Ara") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(cities, key = { it.name }) { city ->
                        Text(
                            city.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(city) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
    )
}
