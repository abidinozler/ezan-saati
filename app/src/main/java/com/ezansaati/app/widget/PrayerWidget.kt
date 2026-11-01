package com.ezansaati.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.ezansaati.app.MainActivity
import com.ezansaati.app.R
import com.ezansaati.app.calendar.Lunar
import com.ezansaati.app.data.SavedLocation
import com.ezansaati.app.data.SettingsRepository
import com.ezansaati.app.notification.CountdownNotification
import com.ezansaati.app.prayer.NextPrayer
import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.PrayerTimes
import com.ezansaati.app.prayer.nextPrayer
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        PrayerWidget.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == PrayerWidget.ACTION_REFRESH) PrayerWidget.updateAll(context)
    }

    override fun onDisabled(context: Context) {
        PrayerWidget.updateAll(context)
    }
}

/**
 * Widget'ı ve kilit ekranı geri sayım bildirimini birlikte yeniler.
 * Geri sayım Chronometer ile kendiliğinden işler; yalnızca her vakit girişinde
 * ve gece yarısında yeniden çizilir.
 */
object PrayerWidget {

    const val ACTION_REFRESH = "com.ezansaati.app.widget.REFRESH"
    private const val REQUEST_CODE = 2001
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    private class Column(val container: Int, val time: Int)

    private val COLUMNS = mapOf(
        Prayer.IMSAK to Column(R.id.col_imsak, R.id.time_imsak),
        Prayer.GUNES to Column(R.id.col_gunes, R.id.time_gunes),
        Prayer.OGLE to Column(R.id.col_ogle, R.id.time_ogle),
        Prayer.IKINDI to Column(R.id.col_ikindi, R.id.time_ikindi),
        Prayer.AKSAM to Column(R.id.col_aksam, R.id.time_aksam),
        Prayer.YATSI to Column(R.id.col_yatsi, R.id.time_yatsi),
    )

    /** Vakit aralığına göre gökyüzü zemini (uygulamadaki renklerle aynı). */
    private val BACKGROUNDS = mapOf(
        Prayer.IMSAK to R.drawable.widget_bg_imsak,
        Prayer.GUNES to R.drawable.widget_bg_gunes,
        Prayer.OGLE to R.drawable.widget_bg_ogle,
        Prayer.IKINDI to R.drawable.widget_bg_ikindi,
        Prayer.AKSAM to R.drawable.widget_bg_aksam,
        Prayer.YATSI to R.drawable.widget_bg_yatsi,
    )

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))

        val settings = SettingsRepository(context)
        val now = ZonedDateTime.now()
        val date = now.toLocalDate()
        val location = settings.location()
        val today = settings.timesFor(date)
        val tomorrow = settings.timesFor(date.plusDays(1))
        val next = if (today != null && tomorrow != null) nextPrayer(now, today, tomorrow) else null
        val countdown = settings.isLockScreenCountdownEnabled()

        if (location != null && today != null && next != null && countdown) {
            CountdownNotification.show(context, location, today, next)
        } else {
            CountdownNotification.cancel(context)
        }

        if (ids.isNotEmpty()) {
            val views = if (location != null && today != null && next != null) {
                buildViews(context, now, location, today, next)
            } else {
                emptyViews(context)
            }
            manager.updateAppWidget(ids, views)
        }

        if (next != null && (ids.isNotEmpty() || countdown)) {
            val midnight = date.plusDays(1).atStartOfDay(now.zone)
            scheduleRefresh(context, minOf(next.nextTime, midnight))
        } else {
            cancelRefresh(context)
        }
    }

    private fun baseViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_prayer)
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        return views
    }

    private fun emptyViews(context: Context) = baseViews(context).apply {
        setViewVisibility(R.id.widget_content, View.GONE)
        setViewVisibility(R.id.widget_empty, View.VISIBLE)
    }

    private fun buildViews(
        context: Context,
        now: ZonedDateTime,
        location: SavedLocation,
        today: PrayerTimes,
        next: NextPrayer,
    ): RemoteViews {
        val views = baseViews(context)
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setInt(R.id.widget_root, "setBackgroundResource", BACKGROUNDS.getValue(next.current))

        views.setTextViewText(R.id.widget_location, location.name)
        views.setTextViewText(R.id.widget_hijri, Lunar.hijriDate(today.date) ?: "")
        views.setTextViewText(
            R.id.widget_next_label,
            "${next.next.dativeName} kalan · ${next.nextTime.format(TIME_FORMAT)}",
        )
        views.setImageViewBitmap(R.id.widget_moon, moonBitmap(Lunar.moonAgeDays(now.toInstant())))

        val remainingMs = Duration.between(now, next.nextTime).toMillis()
        views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + remainingMs, null, true)
        views.setChronometerCountDown(R.id.widget_countdown, true)

        COLUMNS.forEach { (prayer, column) ->
            views.setTextViewText(column.time, today[prayer].format(TIME_FORMAT))
            views.setInt(
                column.container, "setBackgroundResource",
                if (prayer == next.current) R.drawable.widget_highlight else 0,
            )
        }
        return views
    }

    /** Ay evresini widget için küçük bir bitmap olarak çizer. */
    private fun moonBitmap(ageDays: Double, sizePx: Int = 96): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = sizePx / 2f
        val r = c * 0.8f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x1FFFFFFF
        canvas.drawCircle(c, c, r, paint)
        val lit = Path()
        Lunar.litOutline(ageDays).forEachIndexed { i, (x, y) ->
            if (i == 0) lit.moveTo(c + x * r, c + y * r) else lit.lineTo(c + x * r, c + y * r)
        }
        lit.close()
        paint.color = 0xFFF4EBCB.toInt()
        canvas.drawPath(lit, paint)
        return bitmap
    }

    private fun refreshIntent(context: Context, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_REFRESH),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Uyandırmayan alarm: ekran kapalıyken bekler, telefon açılınca hemen çalışır. */
    private fun scheduleRefresh(context: Context, time: ZonedDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = refreshIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val triggerAt = time.toInstant().toEpochMilli() + 1000
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC, triggerAt, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC, triggerAt, pendingIntent)
        }
    }

    private fun cancelRefresh(context: Context) {
        refreshIntent(context, PendingIntent.FLAG_NO_CREATE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
        }
    }
}
