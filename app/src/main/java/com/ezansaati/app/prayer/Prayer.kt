package com.ezansaati.app.prayer

import androidx.annotation.StringRes
import com.ezansaati.app.R
import java.time.LocalDate
import java.time.ZonedDateTime

enum class Prayer(
    @StringRes val nameRes: Int,
    /** "Akşam'a kalan" gibi geri sayım başlığı; dillere göre çekim farklı olduğu için ayrı metin. */
    @StringRes val remainingRes: Int,
    val notifyByDefault: Boolean,
) {
    IMSAK(R.string.prayer_imsak, R.string.remaining_imsak, true),
    GUNES(R.string.prayer_gunes, R.string.remaining_gunes, false),
    OGLE(R.string.prayer_ogle, R.string.remaining_ogle, true),
    IKINDI(R.string.prayer_ikindi, R.string.remaining_ikindi, true),
    AKSAM(R.string.prayer_aksam, R.string.remaining_aksam, true),
    YATSI(R.string.prayer_yatsi, R.string.remaining_yatsi, true),
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
