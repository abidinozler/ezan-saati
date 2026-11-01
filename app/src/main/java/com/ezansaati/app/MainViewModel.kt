package com.ezansaati.app

import android.app.AlarmManager
import android.app.Application
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezansaati.app.data.City
import com.ezansaati.app.data.SavedLocation
import com.ezansaati.app.data.SettingsRepository
import com.ezansaati.app.data.nearestCity
import com.ezansaati.app.location.LocationProvider
import com.ezansaati.app.notification.AlarmScheduler
import com.ezansaati.app.notification.PrayerNotifications
import com.ezansaati.app.prayer.AsrMethod
import com.ezansaati.app.prayer.CalculationMethod
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.PrayerTimes
import com.ezansaati.app.widget.PrayerWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class UiState(
    val location: SavedLocation? = null,
    val today: PrayerTimes? = null,
    val tomorrow: PrayerTimes? = null,
    val notificationsEnabled: Map<Prayer, Boolean> = emptyMap(),
    val offsets: Map<Prayer, Int> = emptyMap(),
    val lockScreenCountdown: Boolean = true,
    /** null: konuma göre otomatik */
    val methodChoice: CalculationMethod? = null,
    val method: CalculationMethod = CalculationMethod.DIYANET,
    val asrMethod: AsrMethod = AsrMethod.SHAFII,
    val hasNotificationPermission: Boolean = true,
    val ignoresBatteryOptimization: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val isLocating: Boolean = false,
    val message: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val locationProvider = LocationProvider(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Vakitleri yeniden hesaplar ve alarmı yeniden kurar. Uygulama öne geldiğinde ve gün değiştiğinde çağrılır. */
    fun refresh() {
        val app = getApplication<Application>()
        val today = LocalDate.now()
        _state.update {
            it.copy(
                location = settings.location(),
                today = settings.timesFor(today),
                tomorrow = settings.timesFor(today.plusDays(1)),
                notificationsEnabled = Prayer.entries.associateWith(settings::isNotificationEnabled),
                offsets = settings.offsets(),
                lockScreenCountdown = settings.isLockScreenCountdownEnabled(),
                methodChoice = settings.methodChoice(),
                method = settings.method(),
                asrMethod = settings.asrMethod(),
                hasNotificationPermission = PrayerNotifications.hasPermission(app),
                ignoresBatteryOptimization = app.getSystemService(PowerManager::class.java)
                    .isIgnoringBatteryOptimizations(app.packageName),
                canScheduleExactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    app.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
            )
        }
        AlarmScheduler.scheduleNext(app)
        PrayerWidget.updateAll(app)
    }

    fun selectCity(city: City) {
        settings.saveLocation(SavedLocation(city.name, city.latitude, city.longitude))
        refresh()
    }

    /** Konum izni alındıktan sonra çağrılmalı. */
    fun useCurrentLocation() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true) }
            val location = locationProvider.currentLocation()
            if (location == null) {
                showMessage(app.getString(R.string.msg_location_failed))
            } else {
                val city = nearestCity(location.latitude, location.longitude)
                val name = city?.let { app.getString(R.string.location_gps_city, it.name) }
                    ?: "%.3f°, %.3f°".format(Locale.ROOT, location.latitude, location.longitude)
                settings.saveLocation(SavedLocation(name, location.latitude, location.longitude))
                showMessage(app.getString(R.string.msg_location_updated, city?.name ?: name))
            }
            _state.update { it.copy(isLocating = false) }
            refresh()
        }
    }

    fun setNotificationEnabled(prayer: Prayer, enabled: Boolean) {
        settings.setNotificationEnabled(prayer, enabled)
        refresh()
    }

    fun setMethodChoice(method: CalculationMethod?) {
        settings.setMethodChoice(method)
        refresh()
    }

    fun setAsrMethod(asr: AsrMethod) {
        settings.setAsrMethod(asr)
        refresh()
    }

    fun setLockScreenCountdown(enabled: Boolean) {
        settings.setLockScreenCountdownEnabled(enabled)
        refresh()
    }

    fun changeOffset(prayer: Prayer, delta: Int) {
        settings.setOffset(prayer, (settings.offset(prayer) + delta).coerceIn(-MAX_OFFSET, MAX_OFFSET))
        refresh()
    }

    fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private companion object {
        const val MAX_OFFSET = 15
    }
}
