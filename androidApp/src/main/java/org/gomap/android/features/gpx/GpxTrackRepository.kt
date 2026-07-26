package org.gomap.android.features.gpx

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class GpxTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val timestamp: Long
)

data class GpxTrack(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val points: List<GpxTrackPoint>
) {
    val distanceMeters: Double
        get() = points.zipWithNext().sumOf { (first, second) ->
            haversineMeters(
                first.latitude,
                first.longitude,
                second.latitude,
                second.longitude
            )
        }

    val durationMillis: Long
        get() = ((endedAt ?: points.lastOrNull()?.timestamp ?: startedAt) - startedAt)
            .coerceAtLeast(0L)
}

enum class GpxRetention(
    val key: String,
    val displayName: String,
    val ageMillis: Long?
) {
    Never("never", "Never", null),
    OneDay("one_day", "1 Day", ChronoUnit.DAYS.duration.toMillis()),
    OneWeek("one_week", "1 Week", ChronoUnit.DAYS.duration.toMillis() * 7),
    OneMonth("one_month", "1 Month", ChronoUnit.DAYS.duration.toMillis() * 30),
    OneYear("one_year", "1 Year", ChronoUnit.DAYS.duration.toMillis() * 365);

    companion object {
        fun fromKey(key: String?): GpxRetention = entries.firstOrNull { it.key == key } ?: Never
    }
}

data class GpxTrackState(
    val activeTrack: GpxTrack? = null,
    val previousTracks: List<GpxTrack> = emptyList(),
    val retention: GpxRetention = GpxRetention.Never,
    val isCollecting: Boolean = false
)

class GpxTrackRepository private constructor(context: Context) : LocationListener {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val storageFile = File(appContext.filesDir, "gpx_tracks.json")
    private val preferences =
        appContext.getSharedPreferences("gpx_track_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<GpxTrackState> = _state.asStateFlow()

    init {
        pruneExpiredTracks()
    }

    @Synchronized
    @SuppressLint("MissingPermission")
    fun startRecording(hasLocationPermission: Boolean): Boolean {
        if (!hasLocationPermission) return false
        if (_state.value.activeTrack == null) {
            _state.value = _state.value.copy(
                activeTrack = GpxTrack(
                    id = UUID.randomUUID().toString(),
                    startedAt = System.currentTimeMillis(),
                    endedAt = null,
                    points = emptyList()
                )
            )
            persist()
        }
        resumeForegroundCollection(hasLocationPermission)
        return true
    }

    @Synchronized
    @SuppressLint("MissingPermission")
    fun resumeForegroundCollection(hasLocationPermission: Boolean) {
        if (!hasLocationPermission || _state.value.activeTrack == null || _state.value.isCollecting) {
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter(locationManager::isProviderEnabled)
        providers.forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    1_000L,
                    1f,
                    this,
                    Looper.getMainLooper()
                )
            }
        }
        _state.value = _state.value.copy(isCollecting = providers.isNotEmpty())
        providers.asSequence()
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { location -> System.currentTimeMillis() - location.time <= 120_000L }
            .maxByOrNull(Location::getTime)
            ?.let(::onLocationChanged)
    }

    @Synchronized
    fun pauseForegroundCollection() {
        runCatching { locationManager.removeUpdates(this) }
        if (_state.value.isCollecting) {
            _state.value = _state.value.copy(isCollecting = false)
        }
    }

    @Synchronized
    fun stopRecording(): GpxTrack? {
        val active = _state.value.activeTrack ?: return null
        pauseForegroundCollection()
        val completed = active.copy(endedAt = System.currentTimeMillis())
        _state.value = _state.value.copy(
            activeTrack = null,
            previousTracks = if (completed.points.isEmpty()) {
                _state.value.previousTracks
            } else {
                listOf(completed) + _state.value.previousTracks
            },
            isCollecting = false
        )
        persist()
        pruneExpiredTracks()
        return completed.takeIf { it.points.isNotEmpty() }
    }

    @Synchronized
    fun deleteTrack(trackId: String) {
        _state.value = _state.value.copy(
            previousTracks = _state.value.previousTracks.filterNot { it.id == trackId }
        )
        persist()
    }

    @Synchronized
    fun setRetention(retention: GpxRetention) {
        preferences.edit().putString(RetentionKey, retention.key).apply()
        _state.value = _state.value.copy(retention = retention)
        pruneExpiredTracks()
    }

    override fun onLocationChanged(location: Location) {
        if (location.hasAccuracy() && location.accuracy > 100f) return
        val current = _state.value.activeTrack ?: return
        val point = GpxTrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            elevation = location.altitude.takeIf { location.hasAltitude() },
            timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        val previous = current.points.lastOrNull()
        if (previous != null) {
            val distance = haversineMeters(
                previous.latitude,
                previous.longitude,
                point.latitude,
                point.longitude
            )
            if (distance < 1.5 && point.timestamp - previous.timestamp < 5_000L) return
        }
        _state.value = _state.value.copy(
            activeTrack = current.copy(points = current.points + point)
        )
        persist()
    }

    fun exportTrack(track: GpxTrack): File {
        val exportDirectory = File(appContext.cacheDir, "shared_gpx").apply { mkdirs() }
        return File(exportDirectory, "gomap-${track.startedAt}.gpx").apply {
            writeText(gpxTrackToXml(track))
        }
    }

    @Synchronized
    private fun pruneExpiredTracks() {
        val retention = GpxRetention.fromKey(
            preferences.getString(RetentionKey, GpxRetention.Never.key)
        )
        val cutoff = retention.ageMillis?.let { System.currentTimeMillis() - it }
        val retained = cutoff?.let { timestamp ->
            _state.value.previousTracks.filter { track ->
                (track.endedAt ?: track.startedAt) >= timestamp
            }
        } ?: _state.value.previousTracks
        val changed = retained.size != _state.value.previousTracks.size ||
            retention != _state.value.retention
        _state.value = _state.value.copy(previousTracks = retained, retention = retention)
        if (changed) persist()
    }

    private fun loadState(): GpxTrackState {
        val retention = GpxRetention.fromKey(
            preferences.getString(RetentionKey, GpxRetention.Never.key)
        )
        if (!storageFile.exists()) return GpxTrackState(retention = retention)
        return runCatching {
            val root = JSONObject(storageFile.readText())
            val active = root.optJSONObject("active")?.toTrack()
            val previousArray = root.optJSONArray("previous") ?: JSONArray()
            val previous = buildList {
                for (index in 0 until previousArray.length()) {
                    previousArray.optJSONObject(index)?.toTrack()?.let(::add)
                }
            }
            GpxTrackState(
                activeTrack = active,
                previousTracks = previous.sortedByDescending(GpxTrack::startedAt),
                retention = retention
            )
        }.getOrElse { GpxTrackState(retention = retention) }
    }

    @Synchronized
    private fun persist() {
        val root = JSONObject().apply {
            put("active", _state.value.activeTrack?.toJson() ?: JSONObject.NULL)
            put(
                "previous",
                JSONArray().apply {
                    _state.value.previousTracks.forEach { put(it.toJson()) }
                }
            )
        }
        storageFile.writeText(root.toString())
    }

    companion object {
        private const val RetentionKey = "retention"

        @Volatile
        private var instance: GpxTrackRepository? = null

        fun get(context: Context): GpxTrackRepository =
            instance ?: synchronized(this) {
                instance ?: GpxTrackRepository(context).also { instance = it }
            }
    }
}

private fun GpxTrack.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("startedAt", startedAt)
    put("endedAt", endedAt ?: JSONObject.NULL)
    put(
        "points",
        JSONArray().apply {
            points.forEach { point ->
                put(
                    JSONObject().apply {
                        put("latitude", point.latitude)
                        put("longitude", point.longitude)
                        put("elevation", point.elevation ?: JSONObject.NULL)
                        put("timestamp", point.timestamp)
                    }
                )
            }
        }
    )
}

private fun JSONObject.toTrack(): GpxTrack? {
    val id = optString("id").takeIf(String::isNotBlank) ?: return null
    val pointsArray = optJSONArray("points") ?: JSONArray()
    val points = buildList {
        for (index in 0 until pointsArray.length()) {
            val point = pointsArray.optJSONObject(index) ?: continue
            add(
                GpxTrackPoint(
                    latitude = point.optDouble("latitude"),
                    longitude = point.optDouble("longitude"),
                    elevation = if (point.isNull("elevation")) null else point.optDouble("elevation"),
                    timestamp = point.optLong("timestamp")
                )
            )
        }
    }
    return GpxTrack(
        id = id,
        startedAt = optLong("startedAt"),
        endedAt = if (isNull("endedAt")) null else optLong("endedAt"),
        points = points
    )
}

internal fun haversineMeters(
    firstLatitude: Double,
    firstLongitude: Double,
    secondLatitude: Double,
    secondLongitude: Double
): Double {
    val earthRadius = 6_371_000.0
    val latitudeDelta = Math.toRadians(secondLatitude - firstLatitude)
    val longitudeDelta = Math.toRadians(secondLongitude - firstLongitude)
    val firstLatitudeRadians = Math.toRadians(firstLatitude)
    val secondLatitudeRadians = Math.toRadians(secondLatitude)
    val value = sin(latitudeDelta / 2).pow(2) +
        cos(firstLatitudeRadians) * cos(secondLatitudeRadians) *
        sin(longitudeDelta / 2).pow(2)
    return earthRadius * 2 * asin(sqrt(value))
}

internal fun gpxTrackToXml(track: GpxTrack): String = buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    append("<gpx version=\"1.1\" creator=\"GoMap Android\" ")
    append("xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
    append("  <trk><name>GoMap ")
    append(Instant.ofEpochMilli(track.startedAt).toString().xmlEscaped())
    append("</name><trkseg>\n")
    track.points.forEach { point ->
        append("    <trkpt lat=\"")
        append(point.latitude)
        append("\" lon=\"")
        append(point.longitude)
        append("\">")
        point.elevation?.let { elevation ->
            append("<ele>")
            append(elevation)
            append("</ele>")
        }
        append("<time>")
        append(Instant.ofEpochMilli(point.timestamp))
        append("</time></trkpt>\n")
    }
    append("  </trkseg></trk>\n</gpx>\n")
}

private fun String.xmlEscaped(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
