package org.gomap.android.osm

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OsmApiRepositoryTest {
    private val repository = OsmApiRepository()

    @Test
    fun parsesNodesWaysRelationsAndTags() {
        val xml = """
            <osm version="0.6">
              <node id="1" lat="38.1" lon="-90.2" version="3">
                <tag k="amenity" v="bench"/>
              </node>
              <node id="2" lat="38.2" lon="-90.3" version="1"/>
              <way id="10" version="2">
                <nd ref="1"/><nd ref="2"/>
                <tag k="highway" v="service"/>
              </way>
              <relation id="20" version="4">
                <member type="way" ref="10" role="outer"/>
                <tag k="type" v="multipolygon"/>
              </relation>
            </osm>
        """.trimIndent()

        val result = repository.parseOsmMapData(ByteArrayInputStream(xml.toByteArray()))

        assertEquals(2, result.nodes.size)
        assertEquals("bench", result.nodes.first().tags["amenity"])
        assertEquals(listOf("1", "2"), result.ways.single().nodeRefs)
        assertEquals(2, result.ways.single().nodes.size)
        assertEquals("outer", result.relations.single().members.single().role)
    }

    @Test
    fun rejectsDoctypeDeclarations() {
        val xml = """<!DOCTYPE osm [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><osm>&xxe;</osm>"""

        assertThrows(Exception::class.java) {
            repository.parseOsmMapData(ByteArrayInputStream(xml.toByteArray()))
        }
    }

    @Test
    fun boundingBoxUsesOsmLongitudeLatitudeOrder() {
        val bounds = BoundingBox(38.0, -91.0, 39.0, -90.0)

        assertEquals("-91.0000000,38.0000000,-90.0000000,39.0000000", bounds.toOsmApiString())
    }

    @Test
    fun rejectsInvertedBoundingBox() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundingBox(39.0, -90.0, 38.0, -91.0)
        }
    }
}
