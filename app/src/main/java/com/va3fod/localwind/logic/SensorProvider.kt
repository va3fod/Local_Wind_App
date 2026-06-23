package com.va3fod.localwind.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.LinkedList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class SensorData(
    val azimuth: Float, // Magnetic Heading
    val pitch: Float,
    val roll: Float,
    val isSteady: Boolean
)

class SensorProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /**
     * Increased Window Size for extreme damping.
     * Original was 30, now 90 (~1.8 seconds of lag at 50Hz)
     * to make the compass 3 times slower and rock-solid.
     */
    private val WINDOW_SIZE = 90

    fun getSensorUpdates(): Flow<SensorData> = callbackFlow {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null
        
        // Rolling windows for Sine and Cosine
        val sinWindow = LinkedList<Float>()
        val cosWindow = LinkedList<Float>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravity = event.values.clone()
                }
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values.clone()
                }

                if (gravity != null && geomagnetic != null) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        
                        val rawAzimuthRad = orientation[0]
                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                        // Component-based smoothing
                        val s = sin(rawAzimuthRad)
                        val c = cos(rawAzimuthRad)

                        // Manage Window
                        sinWindow.addLast(s)
                        cosWindow.addLast(c)
                        while (sinWindow.size > WINDOW_SIZE) {
                            sinWindow.removeFirst()
                            cosWindow.removeFirst()
                        }

                        // Average the components (Ultra-Heavy Damping)
                        val avgSin = sinWindow.average().toFloat()
                        val avgCos = cosWindow.average().toFloat()

                        // Reconstruct Angle
                        val filteredAzimuthRad = atan2(avgSin, avgCos)
                        val azimuthDeg = Math.toDegrees(filteredAzimuthRad.toDouble()).toFloat()

                        // Stability check: capture is vertical.
                        val isSteady = Math.abs(pitch) > 75

                        trySend(SensorData(
                            azimuth = (azimuthDeg + 360) % 360,
                            pitch = pitch,
                            roll = roll,
                            isSteady = isSteady
                        ))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
