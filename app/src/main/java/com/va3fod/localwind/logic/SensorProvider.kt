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
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /**
     * Damping window size.
     * 60 samples at ~50Hz = ~1.2 seconds of rolling average.
     * This provides a professional "weighted" feel without being sluggish.
     */
    private val WINDOW_SIZE = 60

    fun getSensorUpdates(): Flow<SensorData> = callbackFlow {
        val sinWindow = LinkedList<Float>()
        val cosWindow = LinkedList<Float>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val orientation = FloatArray(3)
                    
                    // 1. Detect physical orientation for stability
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val physicalPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    
                    val isSteady = Math.abs(physicalPitch) > 60

                    // 2. Adaptive Coordinate Remapping
                    val outR = FloatArray(9)
                    if (isSteady) {
                        // Vertical: Back camera is North reference
                        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, outR)
                    } else {
                        // Horizontal: Standard Top is North reference
                        System.arraycopy(rotationMatrix, 0, outR, 0, 9)
                    }
                    
                    SensorManager.getOrientation(outR, orientation)
                    val rawAzimuthRad = orientation[0]

                    // 3. Component-based Moving Average Filter
                    sinWindow.addLast(sin(rawAzimuthRad))
                    cosWindow.addLast(cos(rawAzimuthRad))
                    while (sinWindow.size > WINDOW_SIZE) {
                        sinWindow.removeFirst()
                        cosWindow.removeFirst()
                    }

                    val avgSin = sinWindow.average().toFloat()
                    val avgCos = cosWindow.average().toFloat()

                    // 4. Reconstruct Angle
                    val filteredAzimuthRad = atan2(avgSin, avgCos)
                    
                    // Standard Android Azimuth is 0 at North, pi/2 at East, -pi/2 at West.
                    // This is already clockwise positive 0-180 and negative 0-180.
                    val azimuthDeg = (Math.toDegrees(filteredAzimuthRad.toDouble()).toFloat() + 360) % 360

                    trySend(SensorData(
                        azimuth = azimuthDeg,
                        pitch = physicalPitch,
                        roll = Math.toDegrees(orientation[2].toDouble()).toFloat(),
                        isSteady = isSteady
                    ))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
