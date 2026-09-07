package com.safeshade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The codec has one job beyond the round trip: never throw.
 *
 * It is decoded inside a DataStore `map`, and a throw there kills every
 * collector of the flow rather than producing an error state.
 */
class FirmwareJsonTest {

    private val release = FirmwareRelease(
        id = "rel-1",
        model = "s1",
        version = "1.2.3",
        versionCode = 3,
        storagePath = "firmware/rel-1.bin",
        sha256 = "ab".repeat(32),
        byteSize = 900L,
        releaseNotes = "Fixes the thing",
        mandatory = true,
        publishedAt = 1_700_000_000_000L
    )

    @Test
    fun `a cache survives the round trip whole`() {
        val cache = FirmwareCache(
            releasesByModel = mapOf("s1" to listOf(release)),
            lastCheckedAt = 1_700_000_001_000L,
            installedVersionByAddress = mapOf("AA:BB:CC:DD:EE:FF" to "1.0.0")
        )

        assertEquals(cache, FirmwareJson.decode(FirmwareJson.encode(cache)))
    }

    @Test
    fun `a corrupt blob decodes to the empty cache instead of throwing`() {
        assertEquals(FirmwareCache(), FirmwareJson.decode("{not json at all"))
        assertEquals(FirmwareCache(), FirmwareJson.decode(null))
        assertEquals(FirmwareCache(), FirmwareJson.decode("   "))
    }

    @Test
    fun `a release with nothing to download is dropped rather than resurrected`() {
        val json = """
            {"releasesByModel":{"s1":[
              {"id":"ok","model":"s1","version":"1.0.0","storagePath":"p","sha256":"aa"},
              {"id":"no-path","model":"s1","version":"1.0.0","sha256":"aa"},
              {"model":"s1","version":"1.0.0","storagePath":"p","sha256":"aa"}
            ]}}
        """.trimIndent()

        val decoded = FirmwareJson.decode(json)

        assertEquals(listOf("ok"), decoded.releasesByModel["s1"]?.map { it.id })
        assertEquals(0, decoded.releasesByModel["s1"]?.single()?.versionCode ?: -1)
        assertNull(decoded.lastCheckedAt)
    }

    @Test
    fun `an installed version recorded as blank is not a version`() {
        val decoded = FirmwareJson.decode("""{"installedVersionByAddress":{"AA":"","BB":"1.0.0"}}""")

        assertEquals(mapOf("BB" to "1.0.0"), decoded.installedVersionByAddress)
    }

    @Test
    fun `an empty cache encodes and decodes to itself`() {
        val encoded = FirmwareJson.encode(FirmwareCache())
        assertTrue(encoded.isNotBlank())
        assertEquals(FirmwareCache(), FirmwareJson.decode(encoded))
    }
}
