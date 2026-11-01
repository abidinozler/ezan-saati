package com.ezansaati.app.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.content.edit
import com.ezansaati.app.data.SettingsRepository
import com.ezansaati.app.prayer.FridayWindow
import com.ezansaati.app.prayer.Prayer
import java.time.ZonedDateTime

/**
 * Cuma öğle ezanından 5 dk önce telefonu sessize alır, 25 dk sonra önceki zil moduna döndürür.
 * Başlangıç ve bitiş için iki ayrı alarm kurulur; bitişte kullanıcı modu kendisi değiştirmişse dokunulmaz.
 */
object FridaySilence {

    private const val ACTION_START = "com.ezansaati.app.friday.START"
    private const val ACTION_END = "com.ezansaati.app.friday.END"
    private const val REQUEST_START = 3001
    private const val REQUEST_END = 3002
    private const val KEY_PREVIOUS_MODE = "friday_previous_ringer_mode"

    /** Sessize almak için "Rahatsız Etmeyin" erişimi gerekir (Android 7+). */
    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /** Sıradaki cuma için alarmları kurar; şu an aralığın içindeysek hemen sessize alır. */
    fun schedule(context: Context) {
        val settings = SettingsRepository(context)
        if (!settings.isFridaySilenceEnabled()) {
            cancel(context)
            restore(context)
            return
        }
        val now = ZonedDateTime.now()
        val window = FridayWindow.next(now) { date -> settings.timesFor(date)?.get(Prayer.OGLE) }
        if (window == null) {
            cancel(context)
            return
        }
        if (window.contains(now)) silence(context) else setAlarm(context, ACTION_START, REQUEST_START, window.start)
        setAlarm(context, ACTION_END, REQUEST_END, window.end)
    }

    private fun silence(context: Context) {
        if (!hasAccess(context)) return
        val audio = context.getSystemService(AudioManager::class.java)
        val prefs = prefs(context)
        if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT || prefs.contains(KEY_PREVIOUS_MODE)) return
        prefs.edit { putInt(KEY_PREVIOUS_MODE, audio.ringerMode) }
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
    }

    private fun restore(context: Context) {
        val prefs = prefs(context)
        val previous = prefs.getInt(KEY_PREVIOUS_MODE, -1)
        if (previous == -1) return
        prefs.edit { remove(KEY_PREVIOUS_MODE) }
        val audio = context.getSystemService(AudioManager::class.java)
        // Kullanıcı bu arada modu kendisi değiştirdiyse onun seçimine dokunma.
        if (hasAccess(context) && audio.ringerMode == AudioManager.RINGER_MODE_SILENT) {
            audio.ringerMode = previous
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("friday_silence", Context.MODE_PRIVATE)

    private fun pendingIntent(context: Context, action: String, requestCode: Int, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, FridaySilenceReceiver::class.java).setAction(action),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun setAlarm(context: Context, action: String, requestCode: Int, time: ZonedDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, action, requestCode, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val triggerAt = time.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        listOf(ACTION_START to REQUEST_START, ACTION_END to REQUEST_END).forEach { (action, code) ->
            pendingIntent(context, action, code, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        }
    }

    internal fun onAlarm(context: Context, action: String?) {
        when (action) {
            ACTION_START -> silence(context)
            ACTION_END -> restore(context)
        }
        schedule(context)
    }
}

class FridaySilenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FridaySilence.onAlarm(context, intent.action)
    }
}
