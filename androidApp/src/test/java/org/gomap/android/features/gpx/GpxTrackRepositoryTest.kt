package org.gomap.android.features.gpx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxTrackRepositoryTest {
    @Test
    fun distanceUsesGeographicCoordinates() {
        val distance = haversineMeters(38.6270, -90.1994, 38.6270, -90.1894)
        assertEquals(869.0, distance, 8.0)
    }

    @Test
    fun exportProducesGpxTrackPoints() {
        val track = GpxTrack(
            id = "test",
            startedAt = 1_700_000_000_000,
            endedAt = 1_700_000_005_000,
            points = listOf(
                GpxTrackPoint(38.6270, -90.1994, 142.5, 1_700_000_000_000),
                GpxTrackPoint(38.6280, -90.1984, null, 1_700_000_005_000)
            )
        )

        val xml = gpxTrackToXml(track)

        assertTrue(xml.contains("<gpx version=\"1.1\""))
        assertTrue(xml.contains("<trkpt lat=\"38.627\" lon=\"-90.1994\">"))
        assertTrue(xml.contains("<ele>142.5</ele>"))
        assertTrue(xml.contains("2023-11-14T22:13:20Z"))
        assertEquals(2, "<trkpt ".toRegex().findAll(xml).count())
    }
}
