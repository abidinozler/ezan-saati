package com.ezansaati.app.calendar

import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.math.cos
import kotlin.math.sin

/** Ay evresi ve Hicri tarih; ikisi de internet gerektirmeden hesaplanır. */
object Lunar {

    const val SYNODIC_MONTH_DAYS = 29.530588853

    /** Referans yeni ay: 6 Ocak 2000 18:14 UTC. */
    private val REFERENCE_NEW_MOON = Instant.parse("2000-01-06T18:14:00Z")

    private val HIJRI_MONTHS = listOf(
        "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir",
        "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce",
    )

    /** Son yeni aydan bu yana geçen gün (0 ≤ yaş < 29,53). */
    fun moonAgeDays(instant: Instant): Double {
        val days = Duration.between(REFERENCE_NEW_MOON, instant).toMillis() / 86_400_000.0
        return ((days % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
    }

    /** Ayın aydınlık kısmının oranı (0: yeni ay, 1: dolunay). */
    fun illumination(ageDays: Double): Double =
        (1 - cos(2 * Math.PI * ageDays / SYNODIC_MONTH_DAYS)) / 2

    /**
     * Ayın aydınlık kısmının birim çember üzerindeki dış hatları (y aşağı doğru).
     * Önce dış kenar (limb) yukarıdan aşağı, sonra gölge sınırı (terminator) aşağıdan yukarı.
     */
    fun litOutline(ageDays: Double, steps: Int = 48): List<Pair<Float, Float>> {
        val k = cos(2 * Math.PI * ageDays / SYNODIC_MONTH_DAYS).toFloat()
        val side = if (ageDays < SYNODIC_MONTH_DAYS / 2) 1f else -1f // büyürken sağ taraf aydınlık
        fun point(i: Int, scale: Float): Pair<Float, Float> {
            val phi = -Math.PI / 2 + Math.PI * i / steps
            return side * cos(phi).toFloat() * scale to sin(phi).toFloat()
        }
        return (0..steps).map { point(it, 1f) } + (steps downTo 0).map { point(it, k) }
    }

    fun phaseName(ageDays: Double): String {
        val f = ageDays / SYNODIC_MONTH_DAYS
        return when {
            f < 0.034 || f >= 0.966 -> "Yeni ay"
            f < 0.216 -> "Hilal"
            f < 0.284 -> "İlk dördün"
            f < 0.466 -> "Şişkin ay"
            f < 0.534 -> "Dolunay"
            f < 0.716 -> "Şişkin ay"
            f < 0.784 -> "Son dördün"
            else -> "Hilal"
        }
    }

    /** Ümmü'l-Kura takvimine göre Hicri tarih; Diyanet takviminden ±1 gün farklı olabilir. */
    fun hijriDate(date: LocalDate): String? = try {
        val hijri = HijrahDate.from(date)
        "${hijri.get(ChronoField.DAY_OF_MONTH)} ${HIJRI_MONTHS[hijri.get(ChronoField.MONTH_OF_YEAR) - 1]} " +
            "${hijri.get(ChronoField.YEAR)}"
    } catch (_: DateTimeException) {
        null
    }
}
