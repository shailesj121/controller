package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class MotionData(
    val pitch: Float = 0f, // degrees: -90 to +90
    val roll: Float = 0f,  // degrees: -180 to +180
    val yaw: Float = 0f    // degrees: -180 to +180
)

class MotionSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _motionFlow = MutableStateFlow(MotionData())
    val motionFlow: StateFlow<MotionData> = _motionFlow.asStateFlow()

    private var isListening = false
    var isEnabled = false
        set(value) {
            field = value
            if (value) start() else stop()
        }

    fun start() {
        if (isListening || sensorManager == null) return
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        } else if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        }
    }

    fun stop() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
        _motionFlow.value = MotionData()
    }

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isEnabled) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // Convert radians to degrees
                val yawDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                _motionFlow.value = MotionData(pitch = pitchDeg, roll = rollDeg, yaw = yawDeg)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val pitch = (ay * 9f).coerceIn(-90f, 90f)
                val roll = (ax * 9f).coerceIn(-90f, 90f)
                _motionFlow.value = MotionData(pitch = pitch, roll = roll, yaw = 0f)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
