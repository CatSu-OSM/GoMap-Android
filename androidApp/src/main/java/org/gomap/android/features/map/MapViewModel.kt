package org.gomap.android.features.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.gomap.android.osm.BoundingBox
import org.gomap.android.osm.LatLon
import org.gomap.android.osm.OsmApiRepository
import org.gomap.android.osm.OsmMapData
import org.gomap.android.osm.OsmNode
import org.gomap.android.osm.OsmWay
import org.gomap.android.osm.SelectedFeature
import org.gomap.android.osm.SelectionGeometry

data class MapUiState(
    val cameraCenter: LatLon = LatLon(0.0, 0.0),
    val viewportBounds: BoundingBox? = null,
    val zoom: Double = 2.0,
    val draftNode: OsmNode? = null,
    val downloadedData: OsmMapData = OsmMapData(),
    val selectedFeature: SelectedFeature? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isLoading: Boolean = false,
    val status: String = "Load map data for the current viewport, then tap features to inspect them."
)

class MapViewModel(
    private val repository: OsmApiRepository = OsmApiRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private val undoHistory = mutableListOf<EditSnapshot>()
    private val redoHistory = mutableListOf<EditSnapshot>()

    fun onViewportChanged(center: LatLon, zoom: Double, bounds: BoundingBox?) {
        val currentCenter = _uiState.value.cameraCenter
        val hasRealCenter = currentCenter.latitude != 0.0 || currentCenter.longitude != 0.0
        val isLateGlobalCallback = zoom <= 2.1 &&
            center.latitude in -0.01..0.01 &&
            center.longitude in -0.01..0.01
        if (hasRealCenter && isLateGlobalCallback) {
            Log.d("GoMapData", "Ignored stale global viewport callback")
            return
        }
        Log.d("GoMapData", "Viewport zoom=$zoom bounds=$bounds")
        _uiState.value = _uiState.value.copy(
            cameraCenter = center,
            zoom = zoom,
            viewportBounds = bounds
        )
    }

    fun dropDraftNode(latLon: LatLon) {
        val current = _uiState.value
        val draft = OsmNode(
            id = "draft-${UUID.randomUUID()}",
            coordinate = latLon,
            tags = emptyMap(),
            version = 1
        )
        recordEdit(current)
        _uiState.value = current.copy(
            draftNode = draft,
            cameraCenter = latLon,
            selectedFeature = SelectedFeature(
                kind = "draft",
                id = draft.id,
                title = "Draft node",
                subtitle = "Not uploaded yet",
                tags = mapOf(
                    "lat" to "%.6f".format(latLon.latitude),
                    "lon" to "%.6f".format(latLon.longitude)
                ),
                geometry = SelectionGeometry("point", listOf(latLon))
            ),
            canUndo = undoHistory.isNotEmpty(),
            canRedo = false,
            status = "Draft node ready at %.5f, %.5f".format(latLon.latitude, latLon.longitude)
        )
    }

    fun selectFeature(feature: SelectedFeature?) {
        _uiState.value = _uiState.value.copy(
            selectedFeature = feature,
            status = feature?.let { "Selected ${it.kind} ${it.id}" }
                ?: _uiState.value.status
        )
    }

    fun moveSelectedFeature(feature: SelectedFeature) {
        val current = _uiState.value
        val movedData = moveFeatureInData(current.downloadedData, feature)
        val movedDraft = if (feature.kind == "draft") {
            feature.geometry?.coordinates?.firstOrNull()?.let { coordinate ->
                current.draftNode?.copy(
                    coordinate = coordinate,
                    tags = feature.tags
                )
            } ?: current.draftNode
        } else {
            current.draftNode
        }
        if (movedData == current.downloadedData &&
            movedDraft == current.draftNode &&
            feature == current.selectedFeature
        ) {
            return
        }
        recordEdit(current)
        _uiState.value = current.copy(
            downloadedData = movedData,
            draftNode = movedDraft,
            selectedFeature = feature,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = false,
            status = "Moved ${feature.title}"
        )
    }

    fun updateSelectedFeatureTags(tags: Map<String, String>) {
        val current = _uiState.value
        val selected = current.selectedFeature ?: return
        if (selected.tags == tags) return
        val updated = selected.copy(tags = tags)
        val updatedData = moveFeatureInData(current.downloadedData, updated)
        val updatedDraft = if (updated.kind == "draft") {
            current.draftNode?.copy(tags = tags)
        } else {
            current.draftNode
        }
        recordEdit(current)
        _uiState.value = current.copy(
            downloadedData = updatedData,
            draftNode = updatedDraft,
            selectedFeature = updated,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = false,
            status = "Updated tags for ${updated.title}"
        )
    }

    fun undo() {
        if (undoHistory.isEmpty()) return
        val current = _uiState.value
        redoHistory += current.toEditSnapshot()
        val previous = undoHistory.removeLast()
        _uiState.value = current.restore(
            snapshot = previous,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = true,
            status = "Undid last edit"
        )
    }

    fun redo() {
        if (redoHistory.isEmpty()) return
        val current = _uiState.value
        undoHistory += current.toEditSnapshot()
        val next = redoHistory.removeLast()
        _uiState.value = current.restore(
            snapshot = next,
            canUndo = true,
            canRedo = redoHistory.isNotEmpty(),
            status = "Redid edit"
        )
    }

    fun requestUpload() {
        if (undoHistory.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            status = "Upload is not available yet. OpenStreetMap sign-in and conflict handling must be configured first."
        )
    }

    fun clearOsmDataAndReload() {
        val current = _uiState.value
        clearEditHistory()
        _uiState.value = current.copy(
            draftNode = null,
            downloadedData = OsmMapData(),
            selectedFeature = null,
            canUndo = false,
            canRedo = false,
            isLoading = false,
            status = "Cleared cached OpenStreetMap data. Refreshing the current viewport..."
        )
        loadCurrentViewport()
    }

    fun loadCurrentViewport() {
        val currentState = _uiState.value
        val bounds = currentState.viewportBounds
        Log.d("GoMapData", "Load requested zoom=${currentState.zoom} bounds=$bounds")
        if (bounds == null) {
            _uiState.value = _uiState.value.copy(
                status = "Move the map first so the viewport bounds are known."
            )
            return
        }
        if (currentState.zoom < 15.0) {
            _uiState.value = currentState.copy(
                status = "Zoom in a bit more before loading. The OpenStreetMap map API is best used on smaller viewports."
            )
            return
        }
        if ((bounds.maxLatitude - bounds.minLatitude) > 0.08 ||
            (bounds.maxLongitude - bounds.minLongitude) > 0.08
        ) {
            _uiState.value = currentState.copy(
                status = "This viewport is still too large. Zoom in until you are focused on a neighborhood-sized area."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                status = "Loading OpenStreetMap data for the current viewport..."
            )
            runCatching {
                repository.fetchMapData(bounds)
            }.onSuccess { data ->
                Log.d("GoMapData", "Loaded nodes=${data.nodes.size} ways=${data.ways.size} relations=${data.relations.size}")
                val taggedNodes = data.nodes.count { it.tags.isNotEmpty() }
                clearEditHistory()
                _uiState.value = _uiState.value.copy(
                    downloadedData = data,
                    draftNode = null,
                    isLoading = false,
                    selectedFeature = null,
                    canUndo = false,
                    canRedo = false,
                    status = "Loaded ${data.nodes.size} nodes (${taggedNodes} tagged), ${data.ways.size} ways, ${data.relations.size} relations."
                )
            }.onFailure { error ->
                Log.e("GoMapData", "Viewport load failed", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = error.message ?: "Failed to load OpenStreetMap data."
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun centerOnLastKnownLocation(context: Context) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers
            .asSequence()
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return

        val center = LatLon(latitude = location.latitude, longitude = location.longitude)
        _uiState.value = _uiState.value.copy(
            cameraCenter = center,
            status = "Centered on your last known location. Tap Load Here to pull real OSM data."
        )
    }

    private fun recordEdit(state: MapUiState) {
        undoHistory += state.toEditSnapshot()
        if (undoHistory.size > MaxEditHistory) {
            undoHistory.removeAt(0)
        }
        redoHistory.clear()
    }

    private fun clearEditHistory() {
        undoHistory.clear()
        redoHistory.clear()
    }
}

private const val MaxEditHistory = 100

private data class EditSnapshot(
    val draftNode: OsmNode?,
    val downloadedData: OsmMapData,
    val selectedFeature: SelectedFeature?
)

private fun MapUiState.toEditSnapshot() = EditSnapshot(
    draftNode = draftNode,
    downloadedData = downloadedData,
    selectedFeature = selectedFeature
)

private fun MapUiState.restore(
    snapshot: EditSnapshot,
    canUndo: Boolean,
    canRedo: Boolean,
    status: String
) = copy(
    draftNode = snapshot.draftNode,
    downloadedData = snapshot.downloadedData,
    selectedFeature = snapshot.selectedFeature,
    canUndo = canUndo,
    canRedo = canRedo,
    status = status
)

internal fun moveFeatureInData(
    data: OsmMapData,
    feature: SelectedFeature
): OsmMapData {
    val coordinates = feature.geometry?.coordinates.orEmpty()
    if (coordinates.isEmpty()) return data

    return when (feature.kind) {
        "node" -> {
            val movedCoordinate = coordinates.first()
            val movedNodes = data.nodes.map { node ->
                if (node.id == feature.id) {
                    node.copy(coordinate = movedCoordinate, tags = feature.tags)
                } else {
                    node
                }
            }
            data.copy(
                nodes = movedNodes,
                ways = refreshWayCoordinates(data.ways, movedNodes)
            )
        }

        "way" -> {
            val selectedWay = data.ways.firstOrNull { it.id == feature.id } ?: return data
            if (selectedWay.nodeRefs.size != coordinates.size) return data
            val movedByNodeId = selectedWay.nodeRefs.zip(coordinates).toMap()
            val movedNodes = data.nodes.map { node ->
                movedByNodeId[node.id]?.let { coordinate ->
                    node.copy(coordinate = coordinate)
                } ?: node
            }
            val refreshedWays = refreshWayCoordinates(data.ways, movedNodes).map { way ->
                if (way.id == feature.id) way.copy(tags = feature.tags) else way
            }
            data.copy(nodes = movedNodes, ways = refreshedWays)
        }

        else -> data
    }
}

private fun refreshWayCoordinates(
    ways: List<OsmWay>,
    nodes: List<OsmNode>
): List<OsmWay> {
    val coordinatesById = nodes.associate { it.id to it.coordinate }
    return ways.map { way ->
        way.copy(
            nodes = way.nodeRefs.mapIndexedNotNull { index, nodeId ->
                coordinatesById[nodeId] ?: way.nodes.getOrNull(index)
            }
        )
    }
}
