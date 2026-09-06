package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [parseOverpass] against a fixture shaped like Overpass's real response: a
 * node with a phone, a way that only has a `center` (no `lat`/`lon` of its
 * own) plus `addr:*` tags, a nameless amenity that must fall back to its
 * kind's label, and a non-matching amenity that must be dropped.
 */
class OverpassTest {

    // Reference point in Bangalore. Distances below are deliberately spread
    // out so distance-ordering has no room for ambiguity.
    private val lat = 12.9716
    private val lon = 77.5946

    private val fixture = """
        {
          "elements": [
            {
              "type": "node",
              "id": 1,
              "lat": 12.9730,
              "lon": 77.5946,
              "tags": {
                "amenity": "hospital",
                "name": "City Hospital",
                "phone": "080-1111"
              }
            },
            {
              "type": "way",
              "id": 2,
              "center": { "lat": 12.9800, "lon": 77.6050 },
              "tags": {
                "amenity": "police",
                "addr:housenumber": "12",
                "addr:street": "MG Road",
                "addr:city": "Bengaluru"
              }
            },
            {
              "type": "node",
              "id": 3,
              "lat": 12.9717,
              "lon": 77.5947,
              "tags": {
                "amenity": "pharmacy"
              }
            },
            {
              "type": "node",
              "id": 4,
              "lat": 12.9716,
              "lon": 77.5946,
              "tags": {
                "amenity": "restaurant",
                "name": "Not An Emergency Service"
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses hospital with phone`() {
        val services = parseOverpass(fixture, lat, lon)
        val hospital = services.first { it.kind == ServiceKind.HOSPITAL }
        assertEquals("City Hospital", hospital.name)
        assertEquals("080-1111", hospital.phone)
    }

    @Test
    fun `resolves a way's location from its center`() {
        val services = parseOverpass(fixture, lat, lon)
        val police = services.first { it.kind == ServiceKind.POLICE }
        assertEquals(12.9800, police.lat, 0.0001)
        assertEquals(77.6050, police.lon, 0.0001)
    }

    @Test
    fun `assembles address from addr tags`() {
        val services = parseOverpass(fixture, lat, lon)
        val police = services.first { it.kind == ServiceKind.POLICE }
        assertEquals("12 MG Road, Bengaluru", police.address)
    }

    @Test
    fun `nameless amenity falls back to its kind label`() {
        val services = parseOverpass(fixture, lat, lon)
        val pharmacy = services.first { it.kind == ServiceKind.PHARMACY }
        assertEquals("Pharmacy", pharmacy.name)
        assertNull(pharmacy.phone)
    }

    @Test
    fun `non-matching amenity is dropped`() {
        val services = parseOverpass(fixture, lat, lon)
        assertTrue(services.none { it.name == "Not An Emergency Service" })
        assertEquals(3, services.size)
    }

    @Test
    fun `sorted nearest first`() {
        val services = parseOverpass(fixture, lat, lon)
        val order = services.map { it.kind }
        assertEquals(listOf(ServiceKind.PHARMACY, ServiceKind.HOSPITAL, ServiceKind.POLICE), order)
        assertTrue(services[0].distanceM < services[1].distanceM)
        assertTrue(services[1].distanceM < services[2].distanceM)
    }
}
