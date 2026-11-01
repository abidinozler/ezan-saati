package com.ezansaati.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassReading(
    /** Manyetik kuzeyden saat yönünde, 0–360°. */
    val azimuth: Float,
    /** SensorManager.SENSOR_STATUS_* değeri; bilinmiyorsa -1. */
    val accuracy: Int,
)

class CompassSensor(context: Context) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isAvailable: Boolean get() = rotationSensor != null

    fun readings(): Flow<CompassReading> {
        val sensor = rotationSensor ?: return emptyFlow()
        return callbackFlow {
            val rotation = FloatArray(9)
            val orientation = FloatArray(3)
            var smoothSin = 0.0
            var smoothCos = 0.0
            var initialized = false
            var accuracy = -1

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    SensorManager.getOrientation(rotation, orientation)
                    val azimuth = orientation[0].toDouble()

                    // 359°→0° geçişinde zıplamaması için açı sin/cos üzerinden yumuşatılır.
                    if (!initialized) {
                        smoothSin = sin(azimuth)
                        smoothCos = cos(azimuth)
                        initialized = true
                    } else {
                        smoothSin += SMOOTHING * (sin(azimuth) - smoothSin)
                        smoothCos += SMOOTHING * (cos(azimuth) - smoothCos)
                    }
                    val degrees = (Math.toDegrees(atan2(smoothSin, smoothCos)) + 360) % 360
                    trySend(CompassReading(degrees.toFloat(), accuracy))
                }

                override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                    // Kalibrasyon durumu en güvenilir şekilde manyetometreden gelir.
                    if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) accuracy = newAccuracy
                }
            }

            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            magneticSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }

    private companion object {
        const val SMOOTHING = 0.15
    }
}
