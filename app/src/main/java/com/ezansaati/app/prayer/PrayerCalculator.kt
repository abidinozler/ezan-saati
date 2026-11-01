package com.ezansaati.app.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomik namaz vakti hesabı (PrayTimes.org algoritması), Diyanet parametreleriyle:
 * imsak 18°, yatsı 17°, ikindi Şafii (gölge = 1 boy) ve Diyanet temkin süreleri.
 * İnternet gerektirmez.
 */
object PrayerCalculator {

    private const val FAJR_ANGLE = 18.0
    private const val ISHA_ANGLE = 17.0
    private const val SUNRISE_ANGLE = 0.833 // atmosferik kırılma + güneşin yarıçapı
    private const val ASR_SHADOW_FACTOR = 1.0

    /** Diyanet temkin süreleri (dakika). */
    val DIYANET_TEMKIN: Map<Prayer, Int> = mapOf(
        Prayer.IMSAK to 0,
        Prayer.GUNES to -7,
        Prayer.OGLE to 5,
        Prayer.IKINDI to 4,
        Prayer.AKSAM to 7,
        Prayer.YATSI to 0,
    )

    private val INITIAL_GUESS = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0)

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId = ZoneId.systemDefault(),
        userOffsets: Map<Prayer, Int> = emptyMap(),
    ): PrayerTimes {
        val jDate = julianDate(date) - longitude / (15 * 24)

        // Saatler "yerel güneş saati" cinsinden; iki tur iterasyonla güneş konumu inceltilir.
        var t = INITIAL_GUESS.copyOf()
        repeat(2) {
            val h = DoubleArray(6) { i -> if (t[i].isNaN()) INITIAL_GUESS[i] else t[i] }
            t = doubleArrayOf(
                sunAngleTime(jDate, latitude, FAJR_ANGLE, h[0], counterClockwise = true),
                sunAngleTime(jDate, latitude, SUNRISE_ANGLE, h[1], counterClockwise = true),
                midDay(jDate, h[2]),
                asrTime(jDate, latitude, ASR_SHADOW_FACTOR, h[3]),
                sunAngleTime(jDate, latitude, SUNRISE_ANGLE, h[4], counterClockwise = false),
                sunAngleTime(jDate, latitude, ISHA_ANGLE, h[5], counterClockwise = false),
            )
        }

        // Yüksek enlemlerde güneş 18°/17° altına inmiyorsa: gecenin açıya oranlı bir kısmı.
        val night = 24 - (t[4] - t[1])
        if (t[0].isNaN()) t[0] = t[1] - night * FAJR_ANGLE / 60
        if (t[5].isNaN()) t[5] = t[4] + night * ISHA_ANGLE / 60

        val utcMidnight = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val times = Prayer.entries.associateWith { prayer ->
            val utcHours = t[prayer.ordinal] - longitude / 15
            val minutes = utcHours * 60 +
                DIYANET_TEMKIN.getValue(prayer) +
                (userOffsets[prayer] ?: 0)
            val instant: Instant = utcMidnight.plusSeconds(minutes.roundToLong() * 60)
            instant.atZone(zone)
        }
        return PrayerTimes(date, times)
    }

    // ---- Astronomi ----

    private fun julianDate(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + date.dayOfMonth + b - 1524.5
    }

    /** @return (deklinasyon derece, zaman denklemi saat) */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(darctan2(dcos(e) * dsin(l), dcos(l)) / 15)
        val eqt = q / 15 - ra
        val decl = darcsin(dsin(e) * dsin(l))
        return decl to eqt
    }

    private fun midDay(jDate: Double, time: Double): Double {
        val eqt = sunPosition(jDate + time / 24).second
        return fixHour(12 - eqt)
    }

    private fun sunAngleTime(
        jDate: Double,
        latitude: Double,
        angle: Double,
        time: Double,
        counterClockwise: Boolean,
    ): Double {
        val decl = sunPosition(jDate + time / 24).first
        val noon = midDay(jDate, time)
        val t = darccos(
            (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
        ) / 15
        return noon + if (counterClockwise) -t else t
    }

    private fun asrTime(jDate: Double, latitude: Double, factor: Double, time: Double): Double {
        val decl = sunPosition(jDate + time / 24).first
        val angle = -darccot(factor + dtan(abs(latitude - decl)))
        return sunAngleTime(jDate, latitude, angle, time, counterClockwise = false)
    }

    // ---- Derece cinsinden trigonometri ----

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))

    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun fix(a: Double, b: Double): Double {
        val r = a - b * floor(a / b)
        return if (r < 0) r + b else r
    }
}
