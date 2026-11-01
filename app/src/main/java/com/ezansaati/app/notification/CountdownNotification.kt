package com.ezansaati.app.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ezansaati.app.MainActivity
import com.ezansaati.app.R
import com.ezansaati.app.data.SavedLocation
import com.ezansaati.app.prayer.NextPrayer
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.PrayerTimes
import com.ezansaati.app.ui.Texts

/**
 * Kilit ekranında da görünen, sessiz ve kalıcı geri sayım bildirimi.
 * Geri sayımı sistem kendisi işletir (chronometer); yalnızca vakit girişlerinde yenilenir.
 */
object CountdownNotification {

    private const val CHANNEL_ID = "geri_sayim"
    private const val NOTIFICATION_ID = 50
    private val TIME_FORMAT = Texts.TIME

    private fun createChannel(context: Context) {
        // IMPORTANCE_DEFAULT: "sessiz bildirim" sayılıp kilit ekranında gizlenmesin; ses ve titreşim kapalı.
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.channel_countdown), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_countdown_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, location: SavedLocation, today: PrayerTimes, next: NextPrayer) {
        if (!PrayerNotifications.hasPermission(context)) return
        createChannel(context)

        val openApp = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val times = Prayer.entries.joinToString("   ") { "${context.getString(it.nameRes)} ${today[it].format(TIME_FORMAT)}" }
        val subtitle = listOfNotNull(location.name, Texts.hijriDate(context, today.date)).joinToString(" · ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_crescent)
            .setContentTitle("${context.getString(next.next.remainingRes)} · ${next.nextTime.format(TIME_FORMAT)}")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$subtitle\n$times"))
            .setWhen(next.nextTime.toInstant().toEpochMilli())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp)
            // Android 16+: destekleyen telefonlarda "canlı güncelleme" olarak öne çıkarılmasını iste.
            .addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
