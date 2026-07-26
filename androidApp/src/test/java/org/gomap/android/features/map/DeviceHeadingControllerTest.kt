package org.gomap.android.features.map

import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceHeadingControllerTest {
    @Test
    fun shortestDeltaCrossesNorthWithoutSpinningBackward() {
        assertEquals(2f, shortestHeadingDelta(359f, 1f), 0.001f)
        assertEquals(-2f, shortestHeadingDelta(1f, 359f), 0.001f)
    }

    @Test
    fun lowLatencyFilterRespondsImmediatelyAcrossNorth() {
        assertEquals(0.1f, lowLatencyHeading(359f, 1f), 0.01f)
    }

    @Test
    fun fallbackAccuracyCanRepresentUncertainDirection() {
        assertEquals(20f, fallbackHeadingAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        assertEquals(60f, fallbackHeadingAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
        assertEquals(180f, fallbackHeadingAccuracy(SensorManager.SENSOR_STATUS_UNRELIABLE))
    }
}
