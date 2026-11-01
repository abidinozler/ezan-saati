package com.ezansaati.app.data

import android.content.Context
import androidx.core.content.edit
import com.ezansaati.app.prayer.AsrMethod
import com.ezansaati.app.prayer.CalculationMethod
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.PrayerCalculator
import com.ezansaati.app.prayer.PrayerTimes
import java.time.LocalDate

data class SavedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Ayarlar SharedPreferences'ta tutulur; BroadcastReceiver'lardan da senkron okunabilir.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ezan_settings", Context.MODE_PRIVATE)

    fun location(): SavedLocation? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
        return SavedLocation(name, lat, lng)
    }

    fun saveLocation(location: SavedLocation) {
        prefs.edit {
            putString(KEY_NAME, location.name)
            putString(KEY_LAT, location.latitude.toString())
            putString(KEY_LNG, location.longitude.toString())
        }
    }

    fun isNotificationEnabled(prayer: Prayer): Boolean =
        prefs.getBoolean("notify_${prayer.name}", prayer.notifyByDefault)

    fun setNotificationEnabled(prayer: Prayer, enabled: Boolean) {
        prefs.edit { putBoolean("notify_${prayer.name}", enabled) }
    }

    /** Kilit ekranında ve bildirim panelinde sürekli geri sayım bildirimi. */
    fun isLockScreenCountdownEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_SCREEN, true)

    fun setLockScreenCountdownEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LOCK_SCREEN, enabled) }
    }

    /** Kullanıcının seçtiği yöntem; null ise konuma göre otomatik. */
    fun methodChoice(): CalculationMethod? =
        prefs.getString(KEY_METHOD, null)?.let { name -> CalculationMethod.entries.firstOrNull { it.name == name } }

    fun setMethodChoice(method: CalculationMethod?) {
        prefs.edit { if (method == null) remove(KEY_METHOD) else putString(KEY_METHOD, method.name) }
    }

    /** Hesapta kullanılan yöntem. */
    fun method(): CalculationMethod = methodChoice()
        ?: location()?.let { CalculationMethod.suggestFor(it.latitude, it.longitude) }
        ?: CalculationMethod.DIYANET

    fun asrMethod(): AsrMethod =
        prefs.getString(KEY_ASR, null)?.let { name -> AsrMethod.entries.firstOrNull { it.name == name } }
            ?: AsrMethod.SHAFII

    fun setAsrMethod(asr: AsrMethod) {
        prefs.edit { putString(KEY_ASR, asr.name) }
    }

    /** Resmî takvime göre kullanıcının dakika cinsinden ince ayarı. */
    fun offset(prayer: Prayer): Int = prefs.getInt("offset_${prayer.name}", 0)

    fun setOffset(prayer: Prayer, minutes: Int) {
        prefs.edit { putInt("offset_${prayer.name}", minutes) }
    }

    fun offsets(): Map<Prayer, Int> = Prayer.entries.associateWith { offset(it) }

    fun timesFor(date: LocalDate): PrayerTimes? {
        val loc = location() ?: return null
        return PrayerCalculator.calculate(
            date = date,
            latitude = loc.latitude,
            longitude = loc.longitude,
            userOffsets = offsets(),
            method = method(),
            asr = asrMethod(),
        )
    }

    private companion object {
        const val KEY_NAME = "location_name"
        const val KEY_LAT = "location_lat"
        const val KEY_LNG = "location_lng"
        const val KEY_LOCK_SCREEN = "lock_screen_countdown"
        const val KEY_METHOD = "calculation_method"
        const val KEY_ASR = "asr_method"
    }
}
