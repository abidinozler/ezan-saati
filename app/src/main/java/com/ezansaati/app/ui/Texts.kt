package com.ezansaati.app.ui

import android.content.Context
import com.ezansaati.app.R
import com.ezansaati.app.calendar.Lunar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ekranlar, widget ve bildirimlerin ortak kullandığı yerelleştirilmiş metinler. */
object Texts {

    /** Saatler her dilde 24 saat ve Batı rakamlarıyla gösterilir. */
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun hijriDate(context: Context, date: LocalDate): String? = Lunar.hijriDate(date)?.let {
        val month = context.resources.getStringArray(R.array.hijri_months)[it.month - 1]
        context.getString(R.string.hijri_date, it.day, month, it.year)
    }
}
