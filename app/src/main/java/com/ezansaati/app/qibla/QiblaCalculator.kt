package com.ezansaati.app.qibla

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object QiblaCalculator {

    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    /** Gerçek (coğrafi) kuzeyden saat yönünde kıble açısı, 0–360°. Büyük daire yönü. */
    fun qiblaDirection(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** Kâbe'ye kuş uçuşu uzaklık (haversine). */
    fun distanceKm(latitude: Double, longitude: Double): Double {
        val dPhi = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LATITUDE)) *
            sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }
}
