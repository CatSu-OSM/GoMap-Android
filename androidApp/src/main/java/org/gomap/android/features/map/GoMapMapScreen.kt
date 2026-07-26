package org.gomap.android.features.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.location.LocationManager
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonElement
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow
import org.gomap.android.osm.BoundingBox
import org.gomap.android.osm.LatLon
import org.gomap.android.osm.OsmMapData
import org.gomap.android.osm.OsmWay
import org.gomap.android.osm.SelectedFeature
import org.gomap.android.osm.SelectionGeometry
import org.gomap.android.features.presets.PresetCatalog
import org.gomap.android.features.presets.PresetCategory
import org.gomap.android.features.presets.PresetIcon
import org.gomap.android.features.presets.PresetItem
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.PropertyFactory.symbolPlacement
import org.maplibre.android.style.layers.PropertyFactory.symbolSpacing
import org.maplibre.android.style.layers.PropertyFactory.textKeepUpright
import org.maplibre.android.style.layers.PropertyFactory.textRotationAlignment
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val AerialStyle = "asset://styles/aerial_style.json"
private const val StreetStyle = "asset://styles/osm_raster_style.json"
private const val DraftNodeSourceId = "draft-node-source"
private const val DraftNodeLayerId = "draft-node-layer"
private const val DownloadedNodesSourceId = "downloaded-nodes-source"
private const val DownloadedNodeHaloLayerId = "downloaded-node-halo-layer"
private const val DownloadedNodesLayerId = "downloaded-nodes-layer"
private const val DownloadedWaysSourceId = "downloaded-ways-source"
private const val DownloadedAreasSourceId = "downloaded-areas-source"
private const val DownloadedLabelsSourceId = "downloaded-labels-source"
private const val DownloadedAreasLayerId = "downloaded-areas-layer"
private const val DownloadedWaysLayerId = "downloaded-ways-layer"
private const val DownloadedRoadCasingLayerId = "downloaded-road-casing-layer"
private const val DownloadedRoadsLayerId = "downloaded-roads-layer"
private const val DownloadedLabelsLayerId = "downloaded-labels-layer"
private const val SelectedGeometrySourceId = "selected-geometry-source"
private const val SelectedVerticesSourceId = "selected-vertices-source"
private const val SelectedFillLayerId = "selected-fill-layer"
private const val SelectedLineLayerId = "selected-line-layer"
private const val SelectedDirectionLayerId = "selected-direction-layer"
private const val SelectedVerticesLayerId = "selected-vertices-layer"
private const val SelectedPointLayerId = "selected-point-layer"
private val Glass = Color(0xA34E514B)
private val EditorPink = Color(0xFFFF9A9F)

@Composable
fun GoMapMapScreen(
    state: StateFlow<MapUiState>,
    hasLocationPermission: Boolean,
    onMapLongPress: (LatLon) -> Unit,
    onCenterOnUser: () -> Unit,
    onViewportChanged: (LatLon, Double, BoundingBox?) -> Unit,
    onGrantLocation: () -> Unit,
    onLoadCurrentViewport: () -> Unit,
    onFeatureSelected: (SelectedFeature?) -> Unit,
    onFeatureMoved: (SelectedFeature) -> Unit,
    onFeatureTagsChanged: (Map<String, String>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUpload: () -> Unit
) {
    val uiState by state.collectAsStateWithLifecycle()
    GoMapMapScreen(
        state = uiState,
        hasLocationPermission = hasLocationPermission,
        onMapLongPress = onMapLongPress,
        onCenterOnUser = onCenterOnUser,
        onViewportChanged = onViewportChanged,
        onGrantLocation = onGrantLocation,
        onLoadCurrentViewport = onLoadCurrentViewport,
        onFeatureSelected = onFeatureSelected,
        onFeatureMoved = onFeatureMoved,
        onFeatureTagsChanged = onFeatureTagsChanged,
        onUndo = onUndo,
        onRedo = onRedo,
        onUpload = onUpload
    )
}

@Composable
fun GoMapMapScreen(
    state: MapUiState,
    hasLocationPermission: Boolean,
    onMapLongPress: (LatLon) -> Unit,
    onCenterOnUser: () -> Unit,
    onViewportChanged: (LatLon, Double, BoundingBox?) -> Unit,
    onGrantLocation: () -> Unit,
    onLoadCurrentViewport: () -> Unit,
    onFeatureSelected: (SelectedFeature?) -> Unit,
    onFeatureMoved: (SelectedFeature) -> Unit,
    onFeatureTagsChanged: (Map<String, String>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUpload: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            onCreate(null)
        }
    }
    var mapReady by remember { mutableStateOf(false) }
    var aerialEnabled by remember { mutableStateOf(true) }
    var locationTrackingEnabled by remember { mutableStateOf(hasLocationPermission) }
    var initialViewportRequested by remember { mutableStateOf(false) }
    var selectionScreenPoint by remember { mutableStateOf<PointF?>(null) }
    var selectionScreenGeometry by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var selectionCoordinates by remember { mutableStateOf<List<LatLon>>(emptyList()) }
    var selectionAnchor by remember { mutableStateOf<LatLon?>(null) }
    var pendingMovedFeature by remember { mutableStateOf<SelectedFeature?>(null) }
    var mapController by remember { mutableStateOf<MapLibreMap?>(null) }
    var showTagEditor by remember { mutableStateOf(false) }
    val selectionHitRadiusPx = context.resources.displayMetrics.density * 16f
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mapReady, state.cameraCenter, state.zoom) {
        if (!mapReady) return@LaunchedEffect
        mapView.getMapAsync { map ->
            val current = map.cameraPosition.target ?: LatLng(0.0, 0.0)
            val requested = LatLng(state.cameraCenter.latitude, state.cameraCenter.longitude)
            if (current.latitude != requested.latitude || current.longitude != requested.longitude) {
                map.cameraPosition = CameraPosition.Builder()
                    .target(requested)
                    .zoom(if (requested.latitude == 0.0 && requested.longitude == 0.0) 2.0 else maxOf(state.zoom, 17.0))
                    .build()
            }
        }
    }

    LaunchedEffect(mapReady, state.draftNode, state.downloadedData) {
        if (!mapReady) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                syncDraftNode(style, state)
                syncDownloadedData(style, state.downloadedData)
            }
        }
    }

    LaunchedEffect(mapReady, state.selectedFeature) {
        if (!mapReady) return@LaunchedEffect
        pendingMovedFeature = null
        selectionAnchor = state.selectedFeature?.geometry?.anchor
        selectionCoordinates = state.selectedFeature?.geometry?.coordinates.orEmpty()
        mapView.getMapAsync { map ->
            map.getStyle { style -> syncSelection(style, state.selectedFeature) }
            selectionScreenPoint = selectionAnchor?.let { anchor ->
                map.projection.toScreenLocation(LatLng(anchor.latitude, anchor.longitude))
            }
            selectionScreenGeometry = selectionCoordinates.map { coordinate ->
                map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            locationTrackingEnabled = false
        } else {
            locationTrackingEnabled = true
        }
    }

    LaunchedEffect(mapReady, hasLocationPermission, locationTrackingEnabled) {
        if (!mapReady) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                syncLocationComponent(
                    context,
                    map,
                    style,
                    hasLocationPermission && locationTrackingEnabled
                )
            }
        }
    }

    LaunchedEffect(aerialEnabled) {
        if (!mapReady) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(if (aerialEnabled) AerialStyle else StreetStyle)) { style ->
                installOverlayLayers(style)
                syncDraftNode(style, state)
                syncDownloadedData(style, state.downloadedData)
                syncSelection(style, state.selectedFeature)
                syncLocationComponent(
                    context,
                    map,
                    style,
                    hasLocationPermission && locationTrackingEnabled
                )
            }
        }
    }

    LaunchedEffect(state.viewportBounds, state.zoom, state.downloadedData.nodes.size) {
        if (!initialViewportRequested &&
            state.zoom >= 16.0 &&
            state.viewportBounds != null &&
            state.downloadedData.nodes.isEmpty()
        ) {
            initialViewportRequested = true
            onLoadCurrentViewport()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapController = map
                        map.uiSettings.isCompassEnabled = false
                        map.setStyle(Style.Builder().fromUri(AerialStyle)) { style ->
                            installOverlayLayers(style)
                            syncLocationComponent(
                                context,
                                map,
                                style,
                                hasLocationPermission && locationTrackingEnabled
                            )
                            mapReady = true
                        }
                        map.addOnMapLongClickListener { latLng ->
                            onMapLongPress(LatLon(latLng.latitude, latLng.longitude))
                            true
                        }
                        map.addOnMapClickListener { latLng ->
                            val selected = queryFeatureAtTap(map, latLng, selectionHitRadiusPx)
                            selectionAnchor = selected?.geometry?.anchor
                            selectionCoordinates = selected?.geometry?.coordinates.orEmpty()
                            selectionScreenPoint = selectionAnchor?.let { anchor ->
                                map.projection.toScreenLocation(LatLng(anchor.latitude, anchor.longitude))
                            }
                            selectionScreenGeometry = selectionCoordinates.map { coordinate ->
                                map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
                            }
                            map.getStyle { style -> syncSelection(style, selected) }
                            onFeatureSelected(selected)
                            false
                        }
                        map.addOnCameraMoveListener {
                            selectionScreenPoint = selectionAnchor?.let { anchor ->
                                map.projection.toScreenLocation(LatLng(anchor.latitude, anchor.longitude))
                            }
                            selectionScreenGeometry = selectionCoordinates.map { coordinate ->
                                map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
                            }
                        }
                        map.addOnCameraIdleListener {
                            val target = map.cameraPosition.target ?: return@addOnCameraIdleListener
                            onViewportChanged(
                                LatLon(target.latitude, target.longitude),
                                map.cameraPosition.zoom,
                                map.projection.visibleRegion?.latLngBounds?.toBoundingBox()
                            )
                            selectionScreenPoint = selectionAnchor?.let { anchor ->
                                map.projection.toScreenLocation(LatLng(anchor.latitude, anchor.longitude))
                            }
                            selectionScreenGeometry = selectionCoordinates.map { coordinate ->
                                map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
                            }
                        }
                    }
                }
            }
        )

        if (state.selectedFeature?.geometry?.type != "point" && selectionScreenGeometry.size >= 2) {
            SelectionWayHighlight(selectionScreenGeometry)
        }

        ImageryBadge(
            aerialEnabled = aerialEnabled,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 10.dp, top = 27.dp)
        )

        if (state.isLoading || state.status.isActionableError()) {
            StatusPill(
                status = if (state.isLoading) "Loading OpenStreetMap data…" else state.status,
                loading = state.isLoading,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 68.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 10.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompassControl {
                mapView.getMapAsync { map ->
                    map.animateCamera(CameraUpdateFactory.bearingTo(0.0))
                }
            }
            NavigationControl(
                active = hasLocationPermission && locationTrackingEnabled,
                onClick = {
                    if (!hasLocationPermission) {
                        onGrantLocation()
                    } else {
                        locationTrackingEnabled = !locationTrackingEnabled
                        if (locationTrackingEnabled) {
                            onCenterOnUser()
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = 50.dp)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapControl(
                icon = Icons.Outlined.Map,
                description = "Toggle aerial imagery",
                onClick = { aerialEnabled = !aerialEnabled }
            )
            PlusControl(
                onClick = { onMapLongPress(state.cameraCenter) }
            )
        }

        Crosshair(modifier = Modifier.align(Alignment.Center))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 10.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScaleBar(state)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsControl(
                    onClick = { aerialEnabled = !aerialEnabled }
                )
                MapControl(
                    icon = Icons.Rounded.Search,
                    description = "Load data in this area",
                    loading = state.isLoading,
                    onClick = onLoadCurrentViewport
                )
            }
        }

        if (state.canUndo || state.canRedo) {
            EditHistoryControls(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 10.dp),
                onUndo = onUndo,
                onRedo = onRedo,
                onUpload = onUpload
            )
        }

        state.selectedFeature?.let { feature ->
            selectionScreenPoint?.let { point ->
                if (feature.geometry?.type == "point") {
                    SelectionPointHighlight(point)
                }
                SelectionCallout(
                    feature = feature,
                    point = point,
                    screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() },
                    onMove = { dragAmount ->
                        val map = mapController ?: return@SelectionCallout
                        val geometryType = feature.geometry?.type ?: return@SelectionCallout
                        val shiftedPoints = selectionScreenGeometry.map { screenPoint ->
                            PointF(screenPoint.x + dragAmount.x, screenPoint.y + dragAmount.y)
                        }
                        val movedCoordinates = shiftedPoints.map { screenPoint ->
                            map.projection.fromScreenLocation(screenPoint).let { coordinate ->
                                LatLon(coordinate.latitude, coordinate.longitude)
                            }
                        }
                        val movedGeometry = SelectionGeometry(geometryType, movedCoordinates)
                        val movedFeature = feature.copy(geometry = movedGeometry)
                        selectionScreenGeometry = shiftedPoints
                        selectionScreenPoint = selectionScreenPoint?.let { screenPoint ->
                            PointF(screenPoint.x + dragAmount.x, screenPoint.y + dragAmount.y)
                        }
                        selectionCoordinates = movedCoordinates
                        selectionAnchor = movedGeometry.anchor
                        pendingMovedFeature = movedFeature
                        map.getStyle { style ->
                            syncDownloadedData(
                                style,
                                moveFeatureInData(state.downloadedData, movedFeature)
                            )
                            if (movedFeature.kind == "draft") {
                                val movedDraft = movedCoordinates.firstOrNull()?.let { coordinate ->
                                    state.draftNode?.copy(
                                        coordinate = coordinate,
                                        tags = movedFeature.tags
                                    )
                                }
                                syncDraftNode(style, state.copy(draftNode = movedDraft))
                            }
                            syncSelection(style, movedFeature)
                        }
                    },
                    onMoveFinished = {
                        pendingMovedFeature?.let(onFeatureMoved)
                        pendingMovedFeature = null
                    }
                )
            }
            SelectionActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp),
                onTags = { showTagEditor = true }
            )
        }

        if (!hasLocationPermission) {
            Surface(
                onClick = onGrantLocation,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp),
                color = Glass,
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    "Enable location",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (showTagEditor) {
            state.selectedFeature?.let { feature ->
                CommonTagsEditor(
                    feature = feature,
                    onCancel = { showTagEditor = false },
                    onConfirm = { updatedTags ->
                        onFeatureTagsChanged(updatedTags)
                        showTagEditor = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: String, loading: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Glass, shape = RoundedCornerShape(18.dp), shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            }
            Text(status, color = Color.White, fontSize = 12.sp, maxLines = 2)
        }
    }
}

private fun String.isActionableError(): Boolean {
    val normalized = lowercase()
    return normalized.contains("failed") ||
        normalized.contains("returned http") ||
        normalized.contains("too large") ||
        normalized.contains("zoom in")
}

@Composable
private fun ImageryBadge(aerialEnabled: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.Transparent) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = if (aerialEnabled) "▶ Aerial  Esri  (0.0)" else "▶ OpenStreetMap  (0.0)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Glass).border(1.dp, Color.White.copy(alpha = 0.82f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Imagery information", tint = Color.White, modifier = Modifier.size(25.dp))
            }
        }
    }
}

@Composable
private fun MapControl(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    enabled: Boolean = true,
    selected: Boolean = false,
    loading: Boolean = false,
    large: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (large) 58.dp else 48.dp
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (selected) Color(0xC24A5B4E) else Glass,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.32f))
    ) {
        IconButton(onClick = onClick, enabled = enabled && !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    icon,
                    contentDescription = description,
                    tint = tint.copy(alpha = if (enabled) tint.alpha else 0.28f),
                    modifier = Modifier.size(if (large) 32.dp else 25.dp)
                )
            }
        }
    }
}

@Composable
private fun NavigationControl(active: Boolean, onClick: () -> Unit) {
    RoundControl(size = 48.dp, onClick = onClick) {
        Canvas(
            modifier = Modifier
                .size(29.dp)
                .semantics {
                    contentDescription = if (active) {
                        "Disable GPS location"
                    } else {
                        "Enable GPS location"
                    }
                }
        ) {
            val arrow = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.82f)
                lineTo(size.width * 0.48f, size.height * 0.10f)
                lineTo(size.width * 0.82f, size.height * 0.78f)
                lineTo(size.width * 0.53f, size.height * 0.64f)
                close()
            }
            if (active) {
                drawPath(arrow, Color(0xB0000000), style = Stroke(width = 6f))
                drawPath(arrow, Color(0xFF13B7F4))
                drawPath(arrow, Color.White.copy(alpha = 0.75f), style = Stroke(width = 1.2f))
            } else {
                drawPath(arrow, Color(0xB0000000), style = Stroke(width = 6f))
                drawPath(arrow, Color(0xFF13B7F4), style = Stroke(width = 3.2f))
            }
        }
    }
}

@Composable
private fun PlusControl(onClick: () -> Unit) {
    RoundControl(size = 58.dp, onClick = onClick) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            drawLine(Color(0x99000000), androidx.compose.ui.geometry.Offset(centerX, 1f), androidx.compose.ui.geometry.Offset(centerX, size.height - 1f), 8f, StrokeCap.Round)
            drawLine(Color(0x99000000), androidx.compose.ui.geometry.Offset(1f, centerY), androidx.compose.ui.geometry.Offset(size.width - 1f, centerY), 8f, StrokeCap.Round)
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(centerX, 1f), androidx.compose.ui.geometry.Offset(centerX, size.height - 1f), 4.5f, StrokeCap.Round)
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(1f, centerY), androidx.compose.ui.geometry.Offset(size.width - 1f, centerY), 4.5f, StrokeCap.Round)
        }
    }
}

@Composable
private fun SettingsControl(onClick: () -> Unit) {
    RoundControl(size = 48.dp, onClick = onClick) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val innerRadius = size.minDimension * 0.22f
            val ringRadius = size.minDimension * 0.34f
            val toothRadius = size.minDimension * 0.46f
            repeat(14) { index ->
                val angle = (Math.PI * 2.0 * index / 14.0).toFloat()
                val start = androidx.compose.ui.geometry.Offset(
                    center.x + cos(angle) * ringRadius,
                    center.y + sin(angle) * ringRadius
                )
                val end = androidx.compose.ui.geometry.Offset(
                    center.x + cos(angle) * toothRadius,
                    center.y + sin(angle) * toothRadius
                )
                drawLine(Color.White, start, end, strokeWidth = 3.5f, cap = StrokeCap.Round)
            }
            drawCircle(Color.White, radius = ringRadius, center = center, style = Stroke(width = 3f))
            drawCircle(Color.White, radius = innerRadius, center = center, style = Stroke(width = 2.5f))
        }
    }
}

@Composable
private fun RoundControl(size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Glass,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.32f))
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun CompassControl(onClick: () -> Unit) {
    RoundControl(size = 52.dp, onClick = onClick) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawCircle(Color.White.copy(alpha = 0.14f))
                drawCircle(Color.White.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
                val north = Path().apply {
                    moveTo(size.width / 2f, 3f)
                    lineTo(size.width * 0.64f, size.height / 2f)
                    lineTo(size.width / 2f, size.height * 0.44f)
                    lineTo(size.width * 0.36f, size.height / 2f)
                    close()
                }
                val south = Path().apply {
                    moveTo(size.width / 2f, size.height - 3f)
                    lineTo(size.width * 0.64f, size.height / 2f)
                    lineTo(size.width / 2f, size.height * 0.56f)
                    lineTo(size.width * 0.36f, size.height / 2f)
                    close()
                }
                drawPath(north, Color(0xFFFF4B55))
                drawPath(south, Color.White.copy(alpha = 0.85f))
            }
    }
}

@Composable
private fun Crosshair(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(48.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        drawLine(Color.Black, start = androidx.compose.ui.geometry.Offset(centerX, 2f), end = androidx.compose.ui.geometry.Offset(centerX, size.height - 2f), strokeWidth = 7f, cap = StrokeCap.Square)
        drawLine(Color.Black, start = androidx.compose.ui.geometry.Offset(2f, centerY), end = androidx.compose.ui.geometry.Offset(size.width - 2f, centerY), strokeWidth = 7f, cap = StrokeCap.Square)
        drawLine(Color(0xFFFFF06A), start = androidx.compose.ui.geometry.Offset(centerX, 2f), end = androidx.compose.ui.geometry.Offset(centerX, size.height - 2f), strokeWidth = 3f, cap = StrokeCap.Square)
        drawLine(Color(0xFFFFF06A), start = androidx.compose.ui.geometry.Offset(2f, centerY), end = androidx.compose.ui.geometry.Offset(size.width - 2f, centerY), strokeWidth = 3f, cap = StrokeCap.Square)
    }
}

@Composable
private fun ScaleBar(state: MapUiState) {
    val density = LocalDensity.current.density
    val widthDp = 116.dp
    val pixels = widthDp.value * density
    val meters = 156543.03392 * cos(Math.toRadians(state.cameraCenter.latitude)) / 2.0.pow(state.zoom) * pixels
    val feet = meters * 3.28084
    val label = when {
        feet < 1000 -> "%.0f ft".format(feet)
        else -> "%.2f mi".format(feet / 5280.0)
    }
    Box(modifier = Modifier.width(widthDp).height(25.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height - 3f
            drawLine(Color.Black, androidx.compose.ui.geometry.Offset(1f, y), androidx.compose.ui.geometry.Offset(size.width - 1f, y), 4f)
            drawLine(Color.Black, androidx.compose.ui.geometry.Offset(1f, y - 14f), androidx.compose.ui.geometry.Offset(1f, y + 1f), 4f)
            drawLine(Color.Black, androidx.compose.ui.geometry.Offset(size.width - 1f, y - 14f), androidx.compose.ui.geometry.Offset(size.width - 1f, y + 1f), 4f)
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(2f, y - 1f), androidx.compose.ui.geometry.Offset(size.width - 2f, y - 1f), 1.5f)
        }
        Text(label, modifier = Modifier.align(Alignment.TopCenter), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectionWayHighlight(points: List<PointF>) {
    val strokeWidth = with(LocalDensity.current) { 3.5.dp.toPx() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        drawPath(
            path = path,
            color = Color(0xFF20F275),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SelectionPointHighlight(point: PointF) {
    val density = LocalDensity.current
    val size = 22.dp
    val halfSizePx = with(density) { size.toPx() / 2f }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (point.x - halfSizePx).roundToInt(),
                    (point.y - halfSizePx).roundToInt()
                )
            }
            .size(size)
            .border(2.dp, Color(0xFF20F275))
    )
}

@Composable
private fun SelectionCallout(
    feature: SelectedFeature,
    point: PointF,
    screenWidthPx: Float,
    onMove: (Offset) -> Unit,
    onMoveFinished: () -> Unit
) {
    val density = LocalDensity.current
    val width = 180.dp
    val widthPx = with(density) { width.toPx() }
    val edgePx = with(density) { 8.dp.toPx() }
    val x = (point.x - widthPx / 2f).coerceIn(edgePx, screenWidthPx - widthPx - edgePx)
    val y = point.y + with(density) { 8.dp.toPx() }
    Column(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(width),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(width = 38.dp, height = 34.dp)) {
            val pointer = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(pointer, Color(0xD98A8A8A))
            drawPath(pointer, Color.White.copy(alpha = 0.8f), style = Stroke(width = 2f))
        }
        Surface(
            color = Color(0xE0878787),
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(start = 9.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    feature.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(feature.id) {
                            detectDragGestures(
                                onDragEnd = onMoveFinished,
                                onDragCancel = onMoveFinished
                            ) { change, dragAmount ->
                                change.consume()
                                onMove(dragAmount)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.OpenWith,
                        contentDescription = "Drag to move selected object",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommonTagsEditor(
    feature: SelectedFeature,
    onCancel: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val catalog = remember { PresetCatalog.load(context) }
    val draft = remember(feature.id) {
        mutableStateMapOf<String, String>().apply { putAll(feature.tags) }
    }
    var selectedTab by remember(feature.id) { mutableStateOf("common") }
    var customKey by remember(feature.id) { mutableStateOf("") }
    var customValue by remember(feature.id) { mutableStateOf("") }
    var choosingPreset by remember(feature.id) { mutableStateOf(false) }
    val featureGeometry = when (feature.geometry?.type) {
        "point" -> "Point"
        "area" -> "Area"
        else -> "Way"
    }
    val schemaGeometry = when (feature.geometry?.type) {
        "point" -> "point"
        "area" -> "area"
        else -> "line"
    }
    val currentPreset = catalog.matchingPreset(draft, schemaGeometry)

    BackHandler(enabled = choosingPreset) {
        choosingPreset = false
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 58.dp, bottom = 7.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final).changes.forEach { change ->
                            if (!change.isConsumed) change.consume()
                        }
                    }
                }
            },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(34.dp),
        shadowElevation = 16.dp
    ) {
        if (choosingPreset) {
            PresetPicker(
                catalog = catalog,
                geometry = schemaGeometry,
                currentTags = draft,
                onBack = { choosingPreset = false },
                onEditorTabSelected = { tab ->
                    selectedTab = tab
                    choosingPreset = false
                },
                onSelected = { preset ->
                    if (!preset.isNameSuggestion) {
                        currentPreset?.tags?.keys?.forEach(draft::remove)
                    }
                    preset.tags.forEach { (key, value) ->
                        if (value != "*") draft[key] = value
                    }
                    choosingPreset = false
                }
            )
            return@Surface
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(86.dp)) {
                EditorHeaderButton(
                    icon = Icons.Rounded.Close,
                    description = "Cancel tag changes",
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                )
                Text(
                    "Common tags",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                EditorHeaderButton(
                    icon = Icons.Rounded.Check,
                    description = "Save tag changes",
                    onClick = {
                        val result = draft.toMutableMap()
                        if (customKey.isNotBlank()) {
                            if (customValue.isBlank()) result.remove(customKey.trim())
                            else result[customKey.trim()] = customValue.trim()
                        }
                        onConfirm(result)
                    },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)
            ) {
                if (selectedTab == "common") {
                    Text(
                        "Type",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 6.dp)
                    )
                    TagTypeRow(
                        typeLabel = currentPreset?.name ?: featureGeometry,
                        iconName = currentPreset?.icon,
                        onClick = { choosingPreset = true }
                    )
                    TagFieldRow("Name", "name", draft, "Common name (if any)")
                    TagSectionGap()
                    TagFieldRow("Description", "description", draft)
                    TagFieldRow("Elevation (Meters)", "ele", draft, trailingText = "m   ft")
                    TagFieldRow("Fix Me", "fixme", draft)
                    TagFieldRow("Image", "image", draft, "https://example.com/photo.jpg")
                    TagFieldRow(
                        "Last Checked Date",
                        "check_date",
                        draft,
                        "YYYY-MM-DD",
                        trailingIcon = Icons.Outlined.CalendarMonth
                    )
                    TagFieldRow("Mapillary Image ID", "mapillary", draft)
                    TagFieldRow("Note", "note", draft)
                    TagFieldRow(
                        "Panoramax Image ID",
                        "panoramax",
                        draft,
                        trailingIcon = Icons.Outlined.CameraAlt
                    )
                    TagFieldRow("Start Date", "start_date", draft, "YYYY-MM-DD")
                    TagFieldRow("Website", "website", draft, "https://example.com")
                    TagFieldRow("Wikidata", "wikidata", draft)
                    TagFieldRow("Wikimedia Commons Page", "wikimedia_commons", draft, "File:Example.jpg")
                    TagFieldRow("Wikipedia", "wikipedia", draft)
                    TagSectionGap()
                    CustomTagEntry(
                        key = customKey,
                        value = customValue,
                        onKeyChanged = { customKey = it },
                        onValueChanged = { customValue = it }
                    )
                    EditorLink("Custom Features")
                    EditorLink("Custom Fields")
                } else {
                    Text(
                        "All tags",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 6.dp)
                    )
                    if (draft.isEmpty()) {
                        Text(
                            "No tags on this object",
                            color = Color(0xFF6D6D72),
                            fontSize = 17.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        draft.keys.sorted().forEach { key ->
                            TagFieldRow(key, key, draft)
                        }
                    }
                    TagSectionGap()
                    CustomTagEntry(
                        key = customKey,
                        value = customValue,
                        onKeyChanged = { customKey = it },
                        onValueChanged = { customValue = it }
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            TagEditorTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            )
        }
    }
}

@Composable
private fun EditorHeaderButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color(0xFF3A3A3C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(29.dp))
        }
    }
}

@Composable
private fun TagTypeRow(
    typeLabel: String,
    iconName: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Type", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.34f))
        PresetIcon(
            iconName = iconName,
            tint = Color(0xFF0A84FF),
            modifier = Modifier.size(24.dp).padding(2.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            typeLabel,
            color = Color(0xFF0A84FF),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f)
        )
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF636366), modifier = Modifier.size(24.dp))
    }
    HorizontalDivider(color = Color(0xFF3A3A3C))
}

@Composable
private fun PresetPicker(
    catalog: PresetCatalog,
    geometry: String,
    currentTags: Map<String, String>,
    onBack: () -> Unit,
    onEditorTabSelected: (String) -> Unit,
    onSelected: (PresetItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PresetCategory?>(null) }
    var showAllChoices by remember { mutableStateOf(false) }
    val selectedPresetId = catalog.matchingPreset(currentTags, geometry)?.id
    val searchResults = remember(query, geometry, currentTags.toMap()) {
        catalog.search(query, geometry, currentTags)
    }
    val recent = remember(geometry, selectedPresetId) {
        buildList {
            selectedPresetId?.let(catalog::preset)?.let(::add)
            listOf(
                catalog.preset("shop/vacant"),
                catalog.suggestion("Walgreens"),
                catalog.suggestion("Taco Bell"),
                catalog.preset("building"),
                catalog.preset("building/house"),
                catalog.preset("leisure/garden/community")
            ).filterNotNull().filter { it.supports(geometry) }.forEach(::add)
        }.distinctBy(PresetItem::id)
    }
    val categories = remember(geometry) {
        val order = listOf(
            "category-landuse",
            "category-building",
            "category-water",
            "category-natural"
        )
        catalog.categoriesFor(geometry).sortedWith(
            compareBy<PresetCategory> {
                order.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index }
            }.thenBy(PresetCategory::name)
        )
    }
    val featured = remember(geometry) {
        listOf(
            "leisure/park",
            "amenity/hospital",
            "amenity/place_of_worship",
            "amenity/cafe",
            "amenity/restaurant",
            "area"
        ).mapNotNull(catalog::preset).filter { it.supports(geometry) }
    }
    val categoryResults = selectedCategory?.let { catalog.categoryMembers(it, geometry) }.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(82.dp)) {
            EditorHeaderButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = "Back to tags",
                onClick = {
                    if (selectedCategory != null) selectedCategory = null else onBack()
                },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
            )
            Text(
                selectedCategory?.name ?: "POI Type",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                onClick = { showAllChoices = !showAllChoices },
                color = Color(0xFF3A3A3C),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
            ) {
                Text(
                    if (showAllChoices) "Done" else "Configure",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFF3A3A3C))

        Surface(
            color = Color(0xFF2C2C2E),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(23.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            innerTextField()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    Surface(
                        onClick = { query = "" },
                        color = Color(0xFF636366),
                        shape = CircleShape,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFF1C1C1E),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFF3A3A3C))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = 18.dp
            )
        ) {
            when {
                query.isNotBlank() -> {
                    item { PresetSectionTitle("Search results") }
                    items(searchResults, key = PresetItem::id) { preset ->
                        PresetResultRow(
                            preset = preset,
                            selected = preset.id == selectedPresetId,
                            onClick = { onSelected(preset) }
                        )
                    }
                    if (searchResults.isEmpty()) {
                        item { EmptyPresetResults() }
                    }
                }

                selectedCategory != null -> {
                    item { PresetSectionTitle("All choices") }
                    items(categoryResults, key = PresetItem::id) { preset ->
                        PresetResultRow(
                            preset = preset,
                            selected = preset.id == selectedPresetId,
                            onClick = { onSelected(preset) }
                        )
                    }
                }

                else -> {
                    if (!showAllChoices) {
                        item { PresetSectionTitle("Most recent") }
                        items(recent, key = { "recent-${it.id}" }) { preset ->
                            PresetResultRow(
                                preset = preset,
                                selected = preset.id == selectedPresetId,
                                onClick = { onSelected(preset) }
                            )
                        }
                    }
                    item { PresetSectionTitle("All choices") }
                    items(categories, key = PresetCategory::id) { category ->
                        PresetCategoryRow(category) { selectedCategory = category }
                    }
                    items(featured, key = { "featured-${it.id}" }) { preset ->
                        PresetResultRow(
                            preset = preset,
                            selected = preset.id == selectedPresetId,
                            onClick = { onSelected(preset) }
                        )
                    }
                }
            }
        }

        PresetEditorTabs(
            onCommonTags = {},
            onAllTags = { onEditorTabSelected("all") },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun PresetResultRow(
    preset: PresetItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresetIcon(
            iconName = preset.icon,
            tint = Color.White,
            modifier = Modifier.size(56.dp).padding(5.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (preset.isNameSuggestion) "☆ ${preset.name}" else preset.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                presetDescription(preset),
                color = Color(0xFF9B9BA1),
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selected preset",
                tint = Color(0xFF0A84FF),
                modifier = Modifier.size(25.dp)
            )
        } else {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF636366),
                modifier = Modifier.size(23.dp)
            )
        }
    }
    HorizontalDivider(
        color = Color(0xFF3A3A3C),
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
private fun PresetSectionTitle(label: String) {
    Text(
        label,
        color = Color(0xFF9B9BA1),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 20.dp, bottom = 8.dp)
    )
    HorizontalDivider(color = Color(0xFF3A3A3C))
}

@Composable
private fun PresetCategoryRow(category: PresetCategory, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category.name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 6.dp)
        )
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF636366),
            modifier = Modifier.size(24.dp)
        )
    }
    HorizontalDivider(color = Color(0xFF3A3A3C))
}

@Composable
private fun EmptyPresetResults() {
    Text(
        "No matching presets",
        color = Color(0xFF8E8E93),
        fontSize = 17.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp)
    )
}

private fun presetDescription(preset: PresetItem): String {
    val exact = mapOf(
        "building/apartments" to "A building arranged into individual dwellings, often on separate floors. May also have retail outlets on the ground floor.",
        "shop/vacant" to "Shop that is currently not being used.",
        "building" to "A man-made structure with a roof, standing more or less permanently in one place.",
        "building/house" to "A single dwelling unit usually inhabited by one family.",
        "leisure/garden/community" to "The general type and purpose of a given garden.",
        "leisure/park" to "A park, usually in an urban setting, created for recreation and relaxation.",
        "amenity/hospital" to "A hospital providing in-patient medical treatment.",
        "amenity/place_of_worship" to "A place where religious services are conducted.",
        "amenity/cafe" to "For describing useful and important facilities for visitors and residents.",
        "amenity/restaurant" to "A restaurant sells full sit-down meals with servers, and may sell alcohol.",
        "area" to "A generic area feature."
    )
    exact[preset.id]?.let { return it }
    if (preset.isNameSuggestion) {
        return when (preset.tags["amenity"]) {
            "fast_food" -> "Fast Food"
            "pharmacy" -> "Drugstore"
            "cafe" -> "Cafe"
            else -> preset.tags["shop"]?.replace('_', ' ')?.replaceFirstChar(Char::uppercase)
                ?: "Name suggestion"
        }
    }
    return preset.tags.entries.firstOrNull()?.let { (key, value) ->
        "${key.replace('_', ' ').replaceFirstChar(Char::uppercase)}: ${value.replace('_', ' ')}"
    } ?: "OpenStreetMap feature"
}

@Composable
private fun PresetEditorTabs(
    onCommonTags: () -> Unit,
    onAllTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(288.dp).height(62.dp),
        color = Color(0xFF3A3A3C),
        shape = RoundedCornerShape(33.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            TagEditorTab(
                label = "Common Tags",
                icon = Icons.Outlined.Info,
                selected = true,
                onClick = onCommonTags,
                modifier = Modifier.weight(1f)
            )
            TagEditorTab(
                label = "All Tags",
                icon = Icons.Outlined.Folder,
                selected = false,
                onClick = onAllTags,
                modifier = Modifier.weight(1f)
            )
            TagEditorTab(
                label = "Attributes",
                icon = Icons.Outlined.Visibility,
                selected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TagFieldRow(
    label: String,
    key: String,
    draft: MutableMap<String, String>,
    placeholder: String = "Unknown",
    trailingIcon: ImageVector? = null,
    trailingText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.46f)
        )
        BasicTextField(
            value = draft[key].orEmpty(),
            onValueChange = { value ->
                if (value.isBlank()) draft.remove(key) else draft[key] = value
            },
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFFAEAEB2), fontSize = 15.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.weight(0.54f),
            decorationBox = { innerTextField ->
                Box {
                    if (draft[key].isNullOrEmpty()) {
                        Text(placeholder, color = Color(0xFF5D5D62), fontSize = 15.sp, maxLines = 1)
                    }
                    innerTextField()
                }
            }
        )
        trailingText?.let {
            Surface(color = Color(0xFF48484A), shape = RoundedCornerShape(22.dp)) {
                Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
            }
        }
        trailingIcon?.let {
            Icon(it, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(25.dp))
        }
    }
    HorizontalDivider(color = Color(0xFF3A3A3C))
}

@Composable
private fun TagSectionGap() {
    Spacer(Modifier.height(28.dp))
    HorizontalDivider(color = Color(0xFF3A3A3C))
}

@Composable
private fun CustomTagEntry(
    key: String,
    value: String,
    onKeyChanged: (String) -> Unit,
    onValueChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomTagTextBox(key, "key", onKeyChanged, Modifier.weight(1f))
        CustomTagTextBox(value, "value", onValueChanged, Modifier.weight(1.6f))
        Icon(Icons.Outlined.Info, contentDescription = "Custom tag help", tint = Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CustomTagTextBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier.background(Color.Black).padding(horizontal = 9.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isBlank()) Text(placeholder, color = Color(0xFF55555A), fontSize = 16.sp)
                innerTextField()
            }
        }
    )
}

@Composable
private fun EditorLink(label: String) {
    Text(
        label,
        color = Color(0xFF0A84FF),
        fontSize = 17.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 16.dp)
    )
    HorizontalDivider(color = Color(0xFF3A3A3C))
}

@Composable
private fun TagEditorTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(205.dp).height(56.dp),
        color = Color(0xFF3A3A3C),
        shape = RoundedCornerShape(31.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            TagEditorTab(
                label = "Common Tags",
                icon = Icons.Outlined.Info,
                selected = selectedTab == "common",
                onClick = { onTabSelected("common") },
                modifier = Modifier.weight(1f)
            )
            TagEditorTab(
                label = "All Tags",
                icon = Icons.Outlined.Folder,
                selected = selectedTab == "all",
                onClick = { onTabSelected("all") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TagEditorTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) Color(0xFF242426) else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color(0xFF0A84FF) else Color.White, modifier = Modifier.size(25.dp))
        Text(
            label,
            color = if (selected) Color(0xFF0A84FF) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditHistoryControls(
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUpload: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.width(98.dp).height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = Glass,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.32f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = "Undo last edit",
                        tint = Color.White.copy(alpha = if (canUndo) 1f else 0.24f),
                        modifier = Modifier.size(27.dp)
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.20f))
                )
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = "Redo last edit",
                        tint = Color.White.copy(alpha = if (canRedo) 1f else 0.24f),
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
        MapControl(
            icon = Icons.Outlined.CloudUpload,
            description = "Upload changes",
            enabled = canUndo,
            onClick = onUpload
        )
    }
}

@Composable
private fun SelectionActionBar(modifier: Modifier = Modifier, onTags: () -> Unit) {
    Surface(modifier = modifier, color = Color.Black, shape = RoundedCornerShape(9.dp), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionAction("Tags", onClick = onTags)
            SelectionAction("Paste", enabled = false)
            SelectionAction("Delete")
            SelectionAction("More…")
        }
    }
}

@Composable
private fun SelectionAction(label: String, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Text(
        label,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.24f),
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    )
}

@SuppressLint("MissingPermission")
private fun syncLocationComponent(
    context: Context,
    map: MapLibreMap,
    style: Style,
    enabled: Boolean
) {
    val locationComponent = map.locationComponent
    if (!enabled) {
        if (locationComponent.isLocationComponentActivated) {
            locationComponent.isLocationComponentEnabled = false
        }
        return
    }

    val blue = android.graphics.Color.rgb(10, 132, 255)
    val options = LocationComponentOptions.builder(context)
        .foregroundTintColor(blue)
        .backgroundTintColor(android.graphics.Color.WHITE)
        .bearingTintColor(blue)
        .accuracyColor(blue)
        .accuracyAlpha(0.20f)
        .pulseEnabled(true)
        .pulseColor(blue)
        .compassAnimationEnabled(true)
        .build()

    if (!locationComponent.isLocationComponentActivated) {
        val request = LocationEngineRequest.Builder(1_000L)
            .setFastestInterval(500L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        val activation = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(options)
            .locationEngineRequest(request)
            .useDefaultLocationEngine(true)
            .build()
        locationComponent.activateLocationComponent(activation)
    } else {
        locationComponent.applyStyle(options)
    }
    locationComponent.isLocationComponentEnabled = true
    locationComponent.renderMode = RenderMode.COMPASS
    bestLastKnownLocation(context)?.let(locationComponent::forceLocationUpdate)
}

@SuppressLint("MissingPermission")
private fun bestLastKnownLocation(context: Context): android.location.Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return manager.getProviders(true)
        .asSequence()
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { location -> location.elapsedRealtimeNanos }
}

private fun installOverlayLayers(style: Style) {
    if (style.getSource(DraftNodeSourceId) == null) style.addSource(GeoJsonSource(DraftNodeSourceId, emptyFeatureCollection()))
    if (style.getSource(DownloadedNodesSourceId) == null) style.addSource(GeoJsonSource(DownloadedNodesSourceId, emptyFeatureCollection()))
    if (style.getSource(DownloadedWaysSourceId) == null) style.addSource(GeoJsonSource(DownloadedWaysSourceId, emptyFeatureCollection()))
    if (style.getSource(DownloadedAreasSourceId) == null) style.addSource(GeoJsonSource(DownloadedAreasSourceId, emptyFeatureCollection()))
    if (style.getSource(DownloadedLabelsSourceId) == null) style.addSource(GeoJsonSource(DownloadedLabelsSourceId, emptyFeatureCollection()))
    if (style.getSource(SelectedGeometrySourceId) == null) style.addSource(GeoJsonSource(SelectedGeometrySourceId, emptyFeatureCollection()))
    if (style.getSource(SelectedVerticesSourceId) == null) style.addSource(GeoJsonSource(SelectedVerticesSourceId, emptyFeatureCollection()))

    style.addLayerIfMissing(
        FillLayer(DownloadedAreasLayerId, DownloadedAreasSourceId)
            .withProperties(fillColor("#B86F65"), fillOpacity(0.20f), fillOutlineColor("#FF9A9F"))
    )
    style.addLayerIfMissing(
        LineLayer(DownloadedWaysLayerId, DownloadedWaysSourceId)
            .withProperties(lineColor("#FF9A9F"), lineWidth(2.2f), lineOpacity(0.98f))
            .apply { setFilter(Expression.neq(Expression.get("feature_class"), Expression.literal("road"))) }
    )
    style.addLayerIfMissing(
        LineLayer(DownloadedRoadCasingLayerId, DownloadedWaysSourceId)
            .withProperties(lineColor("#FFB0B4"), lineWidth(6.0f), lineOpacity(0.82f))
            .apply { setFilter(classFilter("road")) }
    )
    style.addLayerIfMissing(
        LineLayer(DownloadedRoadsLayerId, DownloadedWaysSourceId)
            .withProperties(lineColor("#FFFFFF"), lineWidth(3.8f), lineOpacity(0.96f))
            .apply { setFilter(classFilter("road")) }
    )
    style.addLayerIfMissing(
        CircleLayer(DownloadedNodeHaloLayerId, DownloadedNodesSourceId)
            .withProperties(circleColor("#FFFFFF"), circleRadius(5.5f), circleStrokeColor("#4A4A4A"), circleStrokeWidth(0.8f))
    )
    style.addLayerIfMissing(
        CircleLayer(DownloadedNodesLayerId, DownloadedNodesSourceId)
            .withProperties(circleColor(Expression.get("poi_color")), circleRadius(2.5f))
    )
    style.addLayerIfMissing(
        SymbolLayer(DownloadedLabelsLayerId, DownloadedLabelsSourceId)
            .withProperties(
                textField(Expression.get("label")),
                textSize(14f),
                textColor("#FFFFFF"),
                textHaloColor("#3A3030"),
                textHaloWidth(1.6f),
                textAllowOverlap(false)
            )
            .apply { setFilter(Expression.has("label")) }
    )
    style.addLayerIfMissing(
        FillLayer(SelectedFillLayerId, SelectedGeometrySourceId)
            .withProperties(fillColor("#20F275"), fillOpacity(0.12f), fillOutlineColor("#20F275"))
    )
    style.addLayerIfMissing(
        LineLayer(SelectedLineLayerId, SelectedGeometrySourceId)
            .withProperties(lineColor("#20F275"), lineWidth(4.5f), lineOpacity(1.0f))
    )
    style.addLayerIfMissing(
        SymbolLayer(SelectedDirectionLayerId, SelectedGeometrySourceId)
            .withProperties(
                symbolPlacement("line"),
                symbolSpacing(60f),
                textField("▶"),
                textSize(9f),
                textColor("#17291E"),
                textHaloColor("#20F275"),
                textHaloWidth(1f),
                textKeepUpright(true),
                textRotationAlignment("map")
            )
            .apply { setFilter(Expression.neq(Expression.get("selection_type"), Expression.literal("point"))) }
    )
    style.addLayerIfMissing(
        CircleLayer(SelectedVerticesLayerId, SelectedVerticesSourceId)
            .withProperties(
                circleColor("#20F275"),
                circleRadius(5.5f),
                circleStrokeColor("#00C9F5"),
                circleStrokeWidth(2.5f)
            )
            .apply { setFilter(Expression.neq(Expression.get("selection_type"), Expression.literal("point"))) }
    )
    style.addLayerIfMissing(
        SymbolLayer(SelectedPointLayerId, SelectedVerticesSourceId)
            .withProperties(
                textField("□"),
                textSize(30f),
                textColor("#20F275"),
                textHaloColor("#00C9F5"),
                textHaloWidth(1f)
            )
            .apply { setFilter(Expression.eq(Expression.get("selection_type"), Expression.literal("point"))) }
    )
    style.addLayerIfMissing(
        CircleLayer(DraftNodeLayerId, DraftNodeSourceId).withProperties(
            circleColor("#36B56A"), circleRadius(8f), circleStrokeWidth(3f), circleStrokeColor("#FFFFFF")
        )
    )
}

private fun classFilter(value: String): Expression =
    Expression.eq(Expression.get("feature_class"), Expression.literal(value))

private fun Style.addLayerIfMissing(layer: org.maplibre.android.style.layers.Layer) {
    if (getLayer(layer.id) == null) addLayer(layer)
}

private fun syncDraftNode(style: Style, state: MapUiState) {
    val source = style.getSourceAs<GeoJsonSource>(DraftNodeSourceId) ?: return
    source.setGeoJson(
        state.draftNode?.let { draft ->
            FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(draft.coordinate.longitude, draft.coordinate.latitude)))
        } ?: emptyFeatureCollection()
    )
}

private fun syncSelection(style: Style, selected: SelectedFeature?) {
    val geometrySource = style.getSourceAs<GeoJsonSource>(SelectedGeometrySourceId) ?: return
    val verticesSource = style.getSourceAs<GeoJsonSource>(SelectedVerticesSourceId) ?: return
    val selection = selected?.geometry
    if (selection == null || selection.coordinates.isEmpty()) {
        geometrySource.setGeoJson(emptyFeatureCollection())
        verticesSource.setGeoJson(emptyFeatureCollection())
        return
    }
    val points = selection.coordinates.map { Point.fromLngLat(it.longitude, it.latitude) }
    val geometry = when (selection.type) {
        "point" -> points.first()
        "area" -> Polygon.fromLngLats(listOf(points))
        else -> LineString.fromLngLats(points)
    }
    geometrySource.setGeoJson(
        FeatureCollection.fromFeature(
            Feature.fromGeometry(geometry).apply {
                addStringProperty("selection_type", selection.type)
            }
        )
    )
    val vertexPoints = if (selection.type == "area" && points.size > 1) points.dropLast(1) else points
    verticesSource.setGeoJson(
        FeatureCollection.fromFeatures(
            vertexPoints.map { point ->
                Feature.fromGeometry(point).apply {
                    addStringProperty("selection_type", selection.type)
                }
            }
        )
    )
}

private fun syncDownloadedData(style: Style, data: OsmMapData) {
    style.getSourceAs<GeoJsonSource>(DownloadedNodesSourceId)?.setGeoJson(
        FeatureCollection.fromFeatures(
            data.nodes.filter { isVisiblePoi(it.tags) }.map { node ->
                Feature.fromGeometry(Point.fromLngLat(node.coordinate.longitude, node.coordinate.latitude)).apply {
                    addStringProperty("element_kind", "node")
                    addStringProperty("element_id", node.id)
                    addStringProperty("title", node.displayName)
                    addStringProperty("subtitle", "Node ${node.id}")
                    addStringProperty("poi_color", nodePoiColor(node.tags))
                    node.tags.forEach { (key, value) -> addStringProperty("tag_$key", value) }
                }
            }
        )
    )
    style.getSourceAs<GeoJsonSource>(DownloadedWaysSourceId)?.setGeoJson(
        FeatureCollection.fromFeatures(data.ways.filter { it.nodes.size >= 2 }.map(::wayFeature))
    )
    val closedAreas = data.ways.filter(::isClosedArea)
    style.getSourceAs<GeoJsonSource>(DownloadedAreasSourceId)?.setGeoJson(
        FeatureCollection.fromFeatures(closedAreas.map(::areaFeature))
    )
    style.getSourceAs<GeoJsonSource>(DownloadedLabelsSourceId)?.setGeoJson(
        FeatureCollection.fromFeatures(
            data.ways.mapNotNull(::labelFeature) + data.nodes.mapNotNull(::addressNodeLabelFeature)
        )
    )
}

private fun wayFeature(way: OsmWay): Feature {
    val points = way.nodes.map { Point.fromLngLat(it.longitude, it.latitude) }
    val isClosed = way.nodeRefs.size >= 4 && way.nodeRefs.firstOrNull() == way.nodeRefs.lastOrNull()
    val featureClass = when {
        "highway" in way.tags -> "road"
        "building" in way.tags -> "building"
        isClosed && isArea(way.tags) -> "area"
        else -> "line"
    }
    return Feature.fromGeometry(LineString.fromLngLats(points)).apply {
        addStringProperty("element_kind", "way")
        addStringProperty("element_id", way.id)
        addStringProperty("title", way.displayName)
        addStringProperty("subtitle", "Way ${way.id}")
        addStringProperty("feature_class", featureClass)
        wayLabel(way.tags)?.let { addStringProperty("label", it) }
        way.tags.forEach { (key, value) -> addStringProperty("tag_$key", value) }
    }
}

private fun isClosedArea(way: OsmWay): Boolean =
    way.nodes.size >= 4 &&
        way.nodeRefs.firstOrNull() == way.nodeRefs.lastOrNull() &&
        ("building" in way.tags || isArea(way.tags))

private fun areaFeature(way: OsmWay): Feature {
    val points = way.nodes.map { Point.fromLngLat(it.longitude, it.latitude) }
    return Feature.fromGeometry(Polygon.fromLngLats(listOf(points))).apply {
        addStringProperty("element_kind", "way")
        addStringProperty("element_id", way.id)
        addStringProperty("title", way.displayName)
        addStringProperty("subtitle", "Way ${way.id}")
        addStringProperty("feature_class", if ("building" in way.tags) "building" else "area")
        way.tags.forEach { (key, value) -> addStringProperty("tag_$key", value) }
    }
}

private fun labelFeature(way: OsmWay): Feature? {
    val label = wayLabel(way.tags) ?: return null
    if (way.nodes.isEmpty()) return null
    val coordinates = if (way.nodes.size > 1 && way.nodes.first() == way.nodes.last()) way.nodes.dropLast(1) else way.nodes
    val latitude = coordinates.map { it.latitude }.average()
    val longitude = coordinates.map { it.longitude }.average()
    return Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).apply {
        addStringProperty("element_kind", "way")
        addStringProperty("element_id", way.id)
        addStringProperty("title", way.displayName)
        addStringProperty("subtitle", "Way ${way.id}")
        addStringProperty("label", label)
        way.tags.forEach { (key, value) -> addStringProperty("tag_$key", value) }
    }
}

private fun addressNodeLabelFeature(node: org.gomap.android.osm.OsmNode): Feature? {
    val label = node.tags["addr:housenumber"] ?: return null
    return Feature.fromGeometry(Point.fromLngLat(node.coordinate.longitude, node.coordinate.latitude)).apply {
        addStringProperty("element_kind", "node")
        addStringProperty("element_id", node.id)
        addStringProperty("title", node.displayName)
        addStringProperty("subtitle", "Node ${node.id}")
        addStringProperty("label", label)
        node.tags.forEach { (key, value) -> addStringProperty("tag_$key", value) }
    }
}

private fun isArea(tags: Map<String, String>): Boolean =
    tags["area"] == "yes" || listOf("landuse", "leisure", "amenity", "place", "shop", "tourism").any(tags::containsKey)

private fun wayLabel(tags: Map<String, String>): String? =
    tags["addr:housenumber"] ?: tags["name"] ?: tags["ref"]

private fun nodePoiColor(tags: Map<String, String>): String = when {
    tags["natural"] == "tree" -> "#1E9B55"
    "amenity" in tags -> "#8A541E"
    "shop" in tags -> "#B34D6E"
    else -> "#4A4A4A"
}

private fun isVisiblePoi(tags: Map<String, String>): Boolean {
    if (tags["natural"] == "tree") return true
    if (tags["power"] == "pole" || tags["man_made"] == "utility_pole") return true
    if (tags.keys.any { it in setOf("shop", "tourism", "barrier", "historic") }) return true
    if (tags["amenity"] in setOf("bench", "waste_basket", "drinking_water", "bicycle_parking", "parking", "fire_hydrant")) return true
    if (tags["highway"] in setOf("crossing", "traffic_signals", "stop", "give_way")) return true
    return false
}

private fun queryFeatureAtTap(
    map: org.maplibre.android.maps.MapLibreMap,
    latLng: LatLng,
    hitRadiusPx: Float
): SelectedFeature? {
    val screenPoint: PointF = map.projection.toScreenLocation(latLng)
    val hitBox = RectF(
        screenPoint.x - hitRadiusPx,
        screenPoint.y - hitRadiusPx,
        screenPoint.x + hitRadiusPx,
        screenPoint.y + hitRadiusPx
    )
    val match = map.queryRenderedFeatures(hitBox, DownloadedNodesLayerId).firstOrNull()
        ?: map.queryRenderedFeatures(hitBox, DownloadedAreasLayerId)
            .firstOrNull(::hasSelectableTitle)
        ?: map.queryRenderedFeatures(hitBox, DownloadedRoadsLayerId)
            .firstOrNull(::isSelectableLine)
        ?: map.queryRenderedFeatures(hitBox, DownloadedWaysLayerId)
            .firstOrNull(::isSelectableLine)
        ?: map.queryRenderedFeatures(hitBox, DownloadedLabelsLayerId)
            .firstOrNull(::hasSelectableTitle)
        ?: return null
    val tags = linkedMapOf<String, String>()
    match.properties()?.entrySet()?.forEach { (key, value) ->
        if (key.startsWith("tag_")) tags[key.removePrefix("tag_")] = value.asFlatString()
    }
    val matchedGeometry = match.geometry()
    val coordinates = when (matchedGeometry) {
        is Point -> listOf(LatLon(matchedGeometry.latitude(), matchedGeometry.longitude()))
        is LineString -> matchedGeometry.coordinates().map { point -> LatLon(point.latitude(), point.longitude()) }
        is Polygon -> matchedGeometry.coordinates().firstOrNull().orEmpty().map { point -> LatLon(point.latitude(), point.longitude()) }
        else -> emptyList()
    }
    val featureClass = if (match.hasProperty("feature_class")) match.getStringProperty("feature_class") else ""
    val selectionType = when {
        matchedGeometry is Point -> "point"
        matchedGeometry is Polygon || featureClass == "building" || featureClass == "area" -> "area"
        else -> "line"
    }
    return SelectedFeature(
        kind = match.getStringProperty("element_kind"),
        id = match.getStringProperty("element_id"),
        title = match.getStringProperty("title"),
        subtitle = match.getStringProperty("subtitle"),
        tags = tags,
        geometry = coordinates.takeIf { it.isNotEmpty() }?.let { SelectionGeometry(selectionType, it) }
    )
}

private fun hasSelectableTitle(feature: Feature): Boolean {
    val title = feature.getStringProperty("title")
    return title.isNotBlank() && !title.startsWith("Way ")
}

private fun isSelectableLine(feature: Feature): Boolean {
    if (feature.geometry() !is LineString) return false
    if (feature.hasProperty("feature_class") && feature.getStringProperty("feature_class") == "road") {
        return true
    }
    if (!hasSelectableTitle(feature)) return false
    return feature.hasProperty("tag_highway") ||
        feature.hasProperty("tag_name") ||
        feature.hasProperty("tag_barrier") ||
        feature.hasProperty("tag_waterway") ||
        feature.hasProperty("tag_railway") ||
        feature.hasProperty("tag_power") ||
        feature.hasProperty("tag_man_made") ||
        feature.hasProperty("tag_boundary")
}

private fun emptyFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(arrayOf())

private fun LatLngBounds.toBoundingBox(): BoundingBox = BoundingBox(
    minLatitude = latitudeSouth,
    minLongitude = longitudeWest,
    maxLatitude = latitudeNorth,
    maxLongitude = longitudeEast
)

private fun JsonElement.asFlatString(): String = if (isJsonNull) "" else asString
