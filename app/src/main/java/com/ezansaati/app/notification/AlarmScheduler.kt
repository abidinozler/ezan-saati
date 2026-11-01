package com.ezansaati.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ezansaati.app.data.SettingsRepository
import com.ezansaati.app.prayer.Prayer
import java.time.ZonedDateTime

/**
 * Her seferinde yalnızca sıradaki (bildirimi açık) vakit için tek bir alarm kurar.
 * Alarm çalınca PrayerAlarmReceiver bir sonrakini kurar; böylece zincir hiç kopmaz.
 */
object AlarmScheduler {

    private const val REQUEST_CODE = 1001
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_TIME = "time"

    fun scheduleNext(context: Context) {
        val settings = SettingsRepository(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()

        val next = (0L..1L).asSequence()
            .mapNotNull { settings.timesFor(today.plusDays(it)) }
            .flatMap { times -> Prayer.entries.asSequence().map { it to times[it] } }
            .firstOrNull { (prayer, time) -> settings.isNotificationEnabled(prayer) && time.isAfter(now) }

        if (next == null) {
            existingPendingIntent(context)?.let(alarmManager::cancel)
            return
        }

        val (prayer, time) = next
        val triggerAt = time.toInstant().toEpochMilli()
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .putExtra(EXTRA_PRAYER, prayer.name)
            .putExtra(EXTRA_TIME, triggerAt)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun existingPendingIntent(context: Context): PendingIntent? = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, PrayerAlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
}
