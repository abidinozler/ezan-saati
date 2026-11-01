package com.ezansaati.app.prayer

import java.time.LocalDate
import java.time.ZonedDateTime

enum class Prayer(
    val displayName: String,
    /** "Akşam'a kalan süre" gibi ifadeler için yönelme hâli. */
    val dativeName: String,
    val notifyByDefault: Boolean,
) {
    IMSAK("İmsak", "İmsak'a", true),
    GUNES("Güneş", "Güneş'e", false),
    OGLE("Öğle", "Öğle'ye", true),
    IKINDI("İkindi", "İkindi'ye", true),
    AKSAM("Akşam", "Akşam'a", true),
    YATSI("Yatsı", "Yatsı'ya", true),
}

data class PrayerTimes(
    val date: LocalDate,
    val times: Map<Prayer, ZonedDateTime>,
) {
    operator fun get(prayer: Prayer): ZonedDateTime = times.getValue(prayer)
}

data class NextPrayer(
    val current: Prayer,
    val next: Prayer,
    val nextTime: ZonedDateTime,
)

/** Şu anki vakti ve sıradaki vakti bulur. Yatsıdan sonra sıradaki vakit yarının imsakıdır. */
fun nextPrayer(now: ZonedDateTime, today: PrayerTimes, tomorrow: PrayerTimes): NextPrayer {
    val upcoming = Prayer.entries.firstOrNull { today[it].isAfter(now) }
        ?: return NextPrayer(Prayer.YATSI, Prayer.IMSAK, tomorrow[Prayer.IMSAK])
    val current = if (upcoming == Prayer.IMSAK) Prayer.YATSI else Prayer.entries[upcoming.ordinal - 1]
    return NextPrayer(current, upcoming, today[upcoming])
}
