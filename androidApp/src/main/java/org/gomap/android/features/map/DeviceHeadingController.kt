package org.gomap.android.features.map

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceHeadingState(
    val headingDegrees: Float? = null,
    val accuracyDegrees: Float = 180f,
    val sensorAvailable: Boolean = true
)

class DeviceHeadingController(
    private val context: Context
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val _state = MutableStateFlow(
        DeviceHeadingState(sensorAvailable = rotationSensor != null)
    )
    val state: StateFlow<DeviceHeadingState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started || rotationSensor == null) return
        started = sensorManager.registerListener(
            this,
            rotationSensor,
            HeadingSamplingPeriodUs,
            0
        )
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotationMatrix = FloatArray(9)
        val displayMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val (axisX, axisY) = displayAxes(context.display?.rotation ?: Surface.ROTATION_0)
        if (!SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, displayMatrix)) return

        val orientation = FloatArray(3)
        SensorManager.getOrientation(displayMatrix, orientation)
        val rawHeading = normalizeHeading(Math.toDegrees(orientation[0].toDouble()).toFloat())
        val previous = _state.value.headingDegrees
        val filteredHeading = lowLatencyHeading(previous, rawHeading)
        val reportedAccuracy = event.values
            .getOrNull(4)
            ?.takeIf { it >= 0f }
            ?.let { radians -> Math.toDegrees(radians.toDouble()).toFloat() }
        val accuracy = (reportedAccuracy ?: fallbackHeadingAccuracy(event.accuracy))
            .coerceIn(MinimumAccuracyDegrees, MaximumAccuracyDegrees)

        if (
            previous == null ||
            abs(shortestHeadingDelta(previous, filteredHeading)) >= MinimumHeadingChangeDegrees ||
            abs(_state.value.accuracyDegrees - accuracy) >= MinimumAccuracyChangeDegrees
        ) {
            _state.value = DeviceHeadingState(
                headingDegrees = filteredHeading,
                accuracyDegrees = accuracy,
                sensorAvailable = true
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        val current = _state.value
        if (current.headingDegrees == null) return
        _state.value = current.copy(
            accuracyDegrees = fallbackHeadingAccuracy(accuracy)
        )
    }
}

internal fun lowLatencyHeading(previous: Float?, current: Float): Float {
    if (previous == null) return normalizeHeading(current)
    val delta = shortestHeadingDelta(previous, current)
    val response = if (abs(delta) >= FastResponseThresholdDegrees) 0.88f else 0.55f
    return normalizeHeading(previous + delta * response)
}

internal fun shortestHeadingDelta(from: Float, to: Float): Float =
    ((to - from + 540f) % 360f) - 180f

internal fun normalizeHeading(value: Float): Float = (value % 360f + 360f) % 360f

internal fun fallbackHeadingAccuracy(sensorAccuracy: Int): Float = when (sensorAccuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 20f
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 60f
    else -> 180f
}

private fun displayAxes(rotation: Int): Pair<Int, Int> = when (rotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
}

private const val HeadingSamplingPeriodUs = 20_000
private const val FastResponseThresholdDegrees = 4f
private const val MinimumHeadingChangeDegrees = 0.2f
private const val MinimumAccuracyChangeDegrees = 0.5f
private const val MinimumAccuracyDegrees = 8f
private const val MaximumAccuracyDegrees = 180f
