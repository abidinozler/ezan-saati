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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ezansaati.app.MainViewModel
import com.ezansaati.app.R
import com.ezansaati.app.UiState
import com.ezansaati.app.data.City
import com.ezansaati.app.data.TURKEY_CITIES
import com.ezansaati.app.prayer.AsrMethod
import com.ezansaati.app.prayer.CalculationMethod
import com.ezansaati.app.prayer.Prayer
import java.text.Collator
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr-TR")

@Composable
fun SettingsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var showCityPicker by remember { mutableStateOf(false) }

    val locationDenied = stringResource(R.string.msg_location_denied)
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.useCurrentLocation()
        else viewModel.showMessage(locationDenied)
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
        Section(stringResource(R.string.settings_location)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    state.location?.let { "${it.name}  (%.3f, %.3f)".format(Locale.ROOT, it.latitude, it.longitude) }
                        ?: stringResource(R.string.settings_no_location),
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
                    Text(stringResource(R.string.action_use_my_location))
                }
                OutlinedButton(onClick = { showCityPicker = true }) { Text(stringResource(R.string.action_choose_city)) }
            }
        }

        // ---- Hesaplama yöntemi ----
        Section(stringResource(R.string.settings_method)) {
            val suggested = state.location?.let { CalculationMethod.suggestFor(it.latitude, it.longitude) }
                ?: CalculationMethod.DIYANET
            OptionRow(
                label = stringResource(R.string.method_auto, stringResource(suggested.nameRes)),
                selected = state.methodChoice == null,
                onClick = { viewModel.setMethodChoice(null) },
            )
            CalculationMethod.entries.forEach { method ->
                OptionRow(
                    label = stringResource(method.nameRes),
                    selected = state.methodChoice == method,
                    onClick = { viewModel.setMethodChoice(method) },
                )
            }
            HorizontalDivider()
            Text(stringResource(R.string.settings_asr), style = MaterialTheme.typography.bodyLarge)
            AsrMethod.entries.forEach { asr ->
                OptionRow(
                    label = stringResource(asr.nameRes),
                    selected = state.asrMethod == asr,
                    onClick = { viewModel.setAsrMethod(asr) },
                )
            }
        }

        // ---- Bildirimler ----
        Section(stringResource(R.string.settings_notifications)) {
            if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FilledTonalButton(onClick = {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.action_grant_notifications)) }
            }
            Prayer.entries.forEach { prayer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(prayer.nameRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.notificationsEnabled[prayer] == true,
                        onCheckedChange = { viewModel.setNotificationEnabled(prayer, it) },
                    )
                }
            }
            if (!state.ignoresBatteryOptimization) {
                HorizontalDivider()
                Text(
                    stringResource(R.string.settings_battery_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            "package:${context.packageName}".toUri(),
                        )
                    )
                }) { Text(stringResource(R.string.action_disable_battery_optimization)) }
            }
        }

        // ---- Kilit ekranı ----
        Section(stringResource(R.string.settings_lock_screen)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_lock_screen_countdown), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_lock_screen_hint),
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
                    stringResource(R.string.settings_needs_notification_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // ---- İnce ayar ----
        Section(stringResource(R.string.settings_offsets)) {
            Text(
                stringResource(R.string.settings_offsets_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Prayer.entries.forEach { prayer ->
                val offset = state.offsets[prayer] ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(prayer.nameRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        title = { Text(stringResource(R.string.action_choose_city)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.action_search)) },
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
