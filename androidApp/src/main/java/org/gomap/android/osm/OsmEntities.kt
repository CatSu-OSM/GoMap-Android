package org.gomap.android.osm

import java.util.Locale

data class LatLon(
    val latitude: Double,
    val longitude: Double
)

data class BoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
) {
    init {
        require(minLatitude in -90.0..90.0 && maxLatitude in -90.0..90.0) {
            "Latitude must be between -90 and 90 degrees."
        }
        require(minLongitude in -180.0..180.0 && maxLongitude in -180.0..180.0) {
            "Longitude must be between -180 and 180 degrees."
        }
        require(minLatitude < maxLatitude && minLongitude < maxLongitude) {
            "Bounding box minimums must be smaller than maximums."
        }
    }

    fun toOsmApiString(): String {
        return String.format(
            Locale.US,
            "%.7f,%.7f,%.7f,%.7f",
            minLongitude,
            minLatitude,
            maxLongitude,
            maxLatitude
        )
    }
}

sealed interface OsmElement {
    val id: String
    val tags: Map<String, String>
    val version: Int
    val displayName: String
}

data class OsmNode(
    override val id: String,
    val coordinate: LatLon,
    override val tags: Map<String, String>,
    override val version: Int
) : OsmElement {
    override val displayName: String
        get() = tags["name"] ?: when {
            tags["power"] == "pole" || tags["man_made"] == "utility_pole" -> "Power Pole"
            tags["natural"] == "tree" -> "Tree"
            tags["highway"] == "crossing" -> "Crossing"
            tags["amenity"] == "bench" -> "Bench"
            tags["amenity"] != null -> tags.getValue("amenity").humanized()
            else -> "Node $id"
        }
}

data class OsmWay(
    override val id: String,
    val nodeRefs: List<String>,
    val nodes: List<LatLon>,
    override val tags: Map<String, String>,
    override val version: Int
) : OsmElement {
    override val displayName: String
        get() = tags["name"] ?: when {
            tags["building"] == "apartments" -> "Apartment Building"
            tags["building"] != null -> "Building"
            tags["highway"] == "service" && tags["service"] == "alley" -> "Alley"
            tags["highway"] != null -> tags.getValue("highway").humanized()
            else -> "Way $id"
        }
}

data class OsmRelationMember(
    val ref: String,
    val role: String,
    val type: String
)

data class OsmRelation(
    override val id: String,
    val members: List<OsmRelationMember>,
    override val tags: Map<String, String>,
    override val version: Int
) : OsmElement {
    override val displayName: String
        get() = tags["name"] ?: tags["type"] ?: "Relation $id"
}

data class OsmMapData(
    val nodes: List<OsmNode> = emptyList(),
    val ways: List<OsmWay> = emptyList(),
    val relations: List<OsmRelation> = emptyList()
)

data class SelectedFeature(
    val kind: String,
    val id: String,
    val title: String,
    val subtitle: String,
    val tags: Map<String, String>,
    val geometry: SelectionGeometry? = null
)

data class SelectionGeometry(
    val type: String,
    val coordinates: List<LatLon>
) {
    val anchor: LatLon
        get() = LatLon(
            latitude = coordinates.map { it.latitude }.average(),
            longitude = coordinates.map { it.longitude }.average()
        )
}

private fun String.humanized(): String =
    split('_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
