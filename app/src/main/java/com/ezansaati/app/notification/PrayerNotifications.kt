package com.ezansaati.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ezansaati.app.MainActivity
import com.ezansaati.app.R
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.ui.Texts
import java.time.Instant
import java.time.ZoneId

object PrayerNotifications {

    private const val CHANNEL_ID = "ezan_vakti"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.channel_prayer), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_prayer_description)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(context: Context, prayer: Prayer, timeMillis: Long) {
        if (!hasPermission(context)) return
        createChannel(context)

        val timeText = Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .format(Texts.TIME)
        val name = context.getString(prayer.nameRes)
        val (title, text) = if (prayer == Prayer.GUNES) {
            context.getString(R.string.notif_sunrise_title) to context.getString(R.string.notif_sunrise_text, timeText)
        } else {
            context.getString(R.string.notif_prayer_title, name) to context.getString(R.string.notif_prayer_text, name, timeText)
        }

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_crescent)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(100 + prayer.ordinal, notification)
    }
}
