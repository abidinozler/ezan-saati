package com.ezansaati.app.prayer

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Cuma namazı için telefonun sessizde kalacağı aralık. */
data class FridayWindow(val start: ZonedDateTime, val end: ZonedDateTime) {
    fun contains(time: ZonedDateTime): Boolean = !time.isBefore(start) && time.isBefore(end)

    companion object {
        const val MINUTES_BEFORE = 5L
        const val MINUTES_AFTER = 25L

        /** Bitişi [now]'dan sonra olan ilk cuma aralığı (şu an içindeysek bugünkü). */
        fun next(now: ZonedDateTime, dhuhrOn: (LocalDate) -> ZonedDateTime?): FridayWindow? {
            var friday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            repeat(2) {
                val dhuhr = dhuhrOn(friday) ?: return null
                val window = FridayWindow(dhuhr.minusMinutes(MINUTES_BEFORE), dhuhr.plusMinutes(MINUTES_AFTER))
                if (window.end.isAfter(now)) return window
                friday = friday.plusWeeks(1)
            }
            return null
        }
    }
}
