package com.ezansaati.app.notification

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.widget.PrayerWidget
import kotlin.math.abs

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
            ?.let { name -> Prayer.entries.firstOrNull { it.name == name } }
        val time = intent.getLongExtra(AlarmScheduler.EXTRA_TIME, 0L)

        // Telefon uzun süre kapalı kaldıysa çoktan geçmiş bir vakit için bildirim gösterme.
        if (prayer != null && abs(System.currentTimeMillis() - time) < MAX_DELAY_MS) {
            PrayerNotifications.show(context, prayer, time)
        }
        AlarmScheduler.scheduleNext(context)
    }

    private companion object {
        const val MAX_DELAY_MS = 15 * 60 * 1000L
    }
}

/** Yeniden başlatma, saat değişimi vb. sonrasında alarmı ve widget'ı yeniden kurar. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        AlarmScheduler.scheduleNext(context)
        FridaySilence.schedule(context)
        PrayerWidget.updateAll(context)
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
