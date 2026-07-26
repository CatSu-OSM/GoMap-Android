package org.gomap.android.features.map

import org.gomap.android.osm.LatLon
import org.gomap.android.osm.OsmMapData
import org.gomap.android.osm.OsmNode
import org.gomap.android.osm.OsmWay
import org.gomap.android.osm.SelectedFeature
import org.gomap.android.osm.SelectionGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelMoveTest {
    @Test
    fun draftCreationAndTagChangesCanBeUndoneAndRedone() {
        val viewModel = MapViewModel()
        val coordinate = LatLon(38.0, -90.0)

        viewModel.dropDraftNode(coordinate)
        assertTrue(viewModel.uiState.value.canUndo)
        assertFalse(viewModel.uiState.value.canRedo)

        viewModel.updateSelectedFeatureTags(mapOf("amenity" to "bench"))
        assertEquals("bench", viewModel.uiState.value.draftNode?.tags?.get("amenity"))

        viewModel.undo()
        assertNull(viewModel.uiState.value.draftNode?.tags?.get("amenity"))
        assertTrue(viewModel.uiState.value.canUndo)
        assertTrue(viewModel.uiState.value.canRedo)

        viewModel.undo()
        assertNull(viewModel.uiState.value.draftNode)
        assertFalse(viewModel.uiState.value.canUndo)
        assertTrue(viewModel.uiState.value.canRedo)

        viewModel.redo()
        assertEquals(coordinate, viewModel.uiState.value.draftNode?.coordinate)
        assertTrue(viewModel.uiState.value.canUndo)
    }

    @Test
    fun newEditAfterUndoClearsRedoHistory() {
        val viewModel = MapViewModel()

        viewModel.dropDraftNode(LatLon(1.0, 1.0))
        viewModel.undo()
        assertTrue(viewModel.uiState.value.canRedo)

        viewModel.dropDraftNode(LatLon(2.0, 2.0))

        assertFalse(viewModel.uiState.value.canRedo)
        assertEquals(LatLon(2.0, 2.0), viewModel.uiState.value.draftNode?.coordinate)
    }

    @Test
    fun movingNodeUpdatesNodeAndConnectedWay() {
        val original = LatLon(1.0, 2.0)
        val moved = LatLon(1.5, 2.5)
        val node = OsmNode("1", original, mapOf("natural" to "tree"), 3)
        val way = OsmWay(
            id = "10",
            nodeRefs = listOf("1"),
            nodes = listOf(original),
            tags = mapOf("highway" to "service"),
            version = 2
        )
        val feature = SelectedFeature(
            kind = "node",
            id = "1",
            title = "Tree",
            subtitle = "Node 1",
            tags = node.tags,
            geometry = SelectionGeometry("point", listOf(moved))
        )

        val result = moveFeatureInData(OsmMapData(listOf(node), listOf(way)), feature)

        assertEquals(moved, result.nodes.single().coordinate)
        assertEquals(moved, result.ways.single().nodes.single())
    }

    @Test
    fun movingWayUpdatesReferencedNodesAndConnectedWays() {
        val first = OsmNode("1", LatLon(1.0, 1.0), emptyMap(), 1)
        val shared = OsmNode("2", LatLon(2.0, 2.0), emptyMap(), 1)
        val selected = OsmWay(
            id = "10",
            nodeRefs = listOf("1", "2"),
            nodes = listOf(first.coordinate, shared.coordinate),
            tags = mapOf("highway" to "service"),
            version = 1
        )
        val connected = OsmWay(
            id = "11",
            nodeRefs = listOf("2"),
            nodes = listOf(shared.coordinate),
            tags = mapOf("highway" to "residential"),
            version = 1
        )
        val movedCoordinates = listOf(LatLon(3.0, 3.0), LatLon(4.0, 4.0))
        val feature = SelectedFeature(
            kind = "way",
            id = "10",
            title = "Alley",
            subtitle = "Way 10",
            tags = mapOf("highway" to "service", "service" to "alley"),
            geometry = SelectionGeometry("line", movedCoordinates)
        )

        val result = moveFeatureInData(
            OsmMapData(nodes = listOf(first, shared), ways = listOf(selected, connected)),
            feature
        )

        assertEquals(movedCoordinates, result.ways.first { it.id == "10" }.nodes)
        assertEquals(movedCoordinates[1], result.ways.first { it.id == "11" }.nodes.single())
        assertEquals("alley", result.ways.first { it.id == "10" }.tags["service"])
    }
}
