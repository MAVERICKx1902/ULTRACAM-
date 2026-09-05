package com.ultracam.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

/**
 * Device attitude (pitch / roll / azimuth) from the rotation-vector sensor.
 * Used for the horizon level and the compass readout.
 */
data class Attitude(
    val pitchDeg: Float,
    val rollDeg: Float,
    val azimuthDeg: Float
) {
    val isLevel: Boolean
        get() = kotlin.math.abs(pitchDeg) < 0.8f && kotlin.math.abs(rollDeg) < 0.8f

    val pitchText: String
        get() = "${if (pitchDeg >= 0) "+" else ""}${(pitchDeg * 10).roundToInt() / 10f}°"

    val rollText: String
        get() = "${if (rollDeg >= 0) "+" else ""}${(rollDeg * 10).roundToInt() / 10f}°"
}

class AttitudeSensor(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val available: Boolean
        get() = sensor != null

    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    fun flow(): Flow<Attitude> {
        val s = sensor
            ?: return flowOf(Attitude(0f, 0f, 0f))
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                    trySend(compute(event.values))
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(
                listener, s, SensorManager.SENSOR_DELAY_GAME
            )
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }

    /**
     * Portrait-locked remap: X stays X, Z (out of screen) becomes the device's
     * "up" axis, so pitch = camera aim up/down and roll = horizon tilt.
     */
    private fun compute(values: FloatArray): Attitude {
        SensorManager.getRotationMatrixFromVector(rotation, values)
        SensorManager.remapCoordinateSystem(
            rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped
        )
        SensorManager.getOrientation(remapped, orientation)
        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
        return Attitude(pitch, roll, azimuth)
    }
}
