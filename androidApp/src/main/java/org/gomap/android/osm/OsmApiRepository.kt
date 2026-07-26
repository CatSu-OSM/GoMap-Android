package org.gomap.android.osm

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element

class OsmApiRepository {
    private companion object {
        const val MAX_OSM_RESPONSE_BYTES = 25 * 1024 * 1024
    }

    suspend fun fetchMapData(bounds: BoundingBox): OsmMapData = withContext(Dispatchers.IO) {
        val url = URL("https://api.openstreetmap.org/api/0.6/map?bbox=${bounds.toOsmApiString()}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/xml")
            setRequestProperty("User-Agent", "GoMapAndroid/0.1")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("OSM API returned HTTP $responseCode")
            }
            connection.inputStream.use { input ->
                parseOsmMapData(BufferedInputStream(input))
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseOsmMapData(input: InputStream): OsmMapData {
        val xmlBytes = input.readLimitedBytes(MAX_OSM_RESPONSE_BYTES)
        require(!xmlBytes.toString(Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)) {
            "OSM XML must not contain a document type declaration."
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            trySetFeature("http://xml.org/sax/features/external-general-entities", false)
            trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        document.documentElement.normalize()

        val nodeMap = linkedMapOf<String, OsmNode>()
        val nodes = document.getElementsByTagName("node")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            val id = element.getAttribute("id")
            val latitude = element.getAttribute("lat").toDoubleOrNull() ?: continue
            val longitude = element.getAttribute("lon").toDoubleOrNull() ?: continue
            val node = OsmNode(
                id = id,
                coordinate = LatLon(latitude = latitude, longitude = longitude),
                tags = parseTags(element),
                version = element.getAttribute("version").toIntOrNull() ?: 1
            )
            nodeMap[id] = node
        }

        val ways = buildList {
            val wayNodes = document.getElementsByTagName("way")
            for (index in 0 until wayNodes.length) {
                val element = wayNodes.item(index) as? Element ?: continue
                val refs = mutableListOf<String>()
                val coordinates = mutableListOf<LatLon>()
                val children = element.childNodes
                for (childIndex in 0 until children.length) {
                    val child = children.item(childIndex) as? Element ?: continue
                    if (child.tagName != "nd") continue
                    val ref = child.getAttribute("ref")
                    if (ref.isBlank()) continue
                    refs += ref
                    nodeMap[ref]?.coordinate?.let(coordinates::add)
                }
                add(
                    OsmWay(
                        id = element.getAttribute("id"),
                        nodeRefs = refs,
                        nodes = coordinates,
                        tags = parseTags(element),
                        version = element.getAttribute("version").toIntOrNull() ?: 1
                    )
                )
            }
        }

        val relations = buildList {
            val relationNodes = document.getElementsByTagName("relation")
            for (index in 0 until relationNodes.length) {
                val element = relationNodes.item(index) as? Element ?: continue
                val members = mutableListOf<OsmRelationMember>()
                val children = element.childNodes
                for (childIndex in 0 until children.length) {
                    val child = children.item(childIndex) as? Element ?: continue
                    if (child.tagName != "member") continue
                    members += OsmRelationMember(
                        ref = child.getAttribute("ref"),
                        role = child.getAttribute("role"),
                        type = child.getAttribute("type")
                    )
                }
                add(
                    OsmRelation(
                        id = element.getAttribute("id"),
                        members = members,
                        tags = parseTags(element),
                        version = element.getAttribute("version").toIntOrNull() ?: 1
                    )
                )
            }
        }

        return OsmMapData(
            nodes = nodeMap.values.toList(),
            ways = ways,
            relations = relations
        )
    }

    private fun DocumentBuilderFactory.trySetFeature(name: String, enabled: Boolean) {
        runCatching { setFeature(name, enabled) }
    }

    private fun InputStream.readLimitedBytes(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "OSM response exceeded ${maxBytes / (1024 * 1024)} MB." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun parseTags(element: Element): Map<String, String> {
        val tags = linkedMapOf<String, String>()
        val children = element.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index) as? Element ?: continue
            if (child.tagName != "tag") continue
            val key = child.getAttribute("k")
            if (key.isBlank()) continue
            tags[key] = child.getAttribute("v")
        }
        return tags
    }
}
