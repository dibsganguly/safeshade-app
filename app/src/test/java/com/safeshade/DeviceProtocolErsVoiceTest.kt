package com.safeshade

import com.safeshade.device.DeviceProtocol
import com.safeshade.device.DeviceProtocol.ErsEntry
import com.safeshade.device.DeviceProtocol.VoiceChunk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the two wire formats designed ahead of firmware in
 * `DeviceCapabilities.awaitingFirmware`: `EXT ERS` and the `VOICE` chunk
 * path. Neither talks to a device — see `DeviceProtocolTest` for why that
 * matters for this file specifically.
 */
class DeviceProtocolErsVoiceTest {

    // ============================================
    // EXT ERS
    // ============================================

    @Test
    fun `ers payload respects the 180-byte budget with four long names`() {
        val entries = listOf(
            ErsEntry('H', "Saint Bartholomew's Regional Memorial Hospital and Trauma Center", "+91 98765 43210", 1200),
            ErsEntry('P', "Central Metropolitan District Police Station Number Fourteen", "+91 91234 56789", 800),
            ErsEntry('F', "Municipal Fire and Rescue Services Headquarters Station", "+91 90000 11111", 2400),
            ErsEntry('M', "Neighbourhood Community Pharmacy and Wellness Dispensary", "+91 99999 22222", 300)
        )

        val payload = DeviceProtocol.encodeErsPayload(entries)

        assertTrue(
            "payload was ${payload.toByteArray(Charsets.UTF_8).size} bytes",
            payload.toByteArray(Charsets.UTF_8).size <= 180
        )
        // All four kinds still present even after truncation.
        assertEquals(4, payload.split(";").size)
    }

    @Test
    fun `ers strips commas and semicolons from names and keeps phone digits only`() {
        val entries = listOf(
            ErsEntry('H', "City, Hospital; Annex", "+91 (987) 654-3210", 500)
        )

        val payload = DeviceProtocol.encodeErsPayload(entries)

        assertTrue(!payload.contains(','))
        val fields = payload.split(":")
        assertTrue(!fields[1].contains(';'))
        assertEquals("919876543210", fields[2])
    }

    @Test
    fun `ers entries are ordered hospital, police, fire, pharmacy regardless of input order`() {
        val entries = listOf(
            ErsEntry('M', "Pharmacy", null, 1),
            ErsEntry('F', "Fire", null, 2),
            ErsEntry('P', "Police", null, 3),
            ErsEntry('H', "Hospital", null, 4)
        )

        val payload = DeviceProtocol.encodeErsPayload(entries)
        val kinds = payload.split(";").map { it.first() }

        assertEquals(listOf('H', 'P', 'F', 'M'), kinds)
    }

    @Test
    fun `ers round-trips through parseErsPayload`() {
        val entries = listOf(
            ErsEntry('H', "General Hospital", "9876543210", 1200),
            ErsEntry('P', "Central Police Station", null, 800),
            ErsEntry('F', "Fire Station 3", "9000011111", 2400),
            ErsEntry('M', "Corner Pharmacy", "9999922222", 300)
        )

        val payload = DeviceProtocol.encodeErsPayload(entries)
        val parsed = DeviceProtocol.parseErsPayload(payload)

        assertEquals(4, parsed.size)
        assertEquals('H', parsed[0].kind)
        assertEquals("General Hospital", parsed[0].name)
        assertEquals("9876543210", parsed[0].phone)
        assertEquals(1200, parsed[0].distanceM)
        assertEquals('P', parsed[1].kind)
        assertNull(parsed[1].phone)
    }

    @Test
    fun `ers with no entries encodes and parses to empty`() {
        assertEquals("", DeviceProtocol.encodeErsPayload(emptyList()))
        assertEquals(emptyList<ErsEntry>(), DeviceProtocol.parseErsPayload(""))
    }

    // ============================================
    // VOICE chunk path
    // ============================================

    @Test
    fun `voice chunk round-trips through encode and decode`() {
        val chunk = VoiceChunk(seq = 2, total = 5, bytes = byteArrayOf(1, 2, 3, 4, 5))

        val decoded = DeviceProtocol.decodeVoiceChunk(DeviceProtocol.encodeVoiceChunk(chunk))

        assertEquals(chunk.seq, decoded?.seq)
        assertEquals(chunk.total, decoded?.total)
        assertArrayEquals(chunk.bytes, decoded?.bytes)
    }

    @Test
    fun `decodeVoiceChunk rejects a bad tag`() {
        val bytes = DeviceProtocol.encodeVoiceChunk(VoiceChunk(0, 1, byteArrayOf(9)))
        bytes[0] = 0x00

        assertNull(DeviceProtocol.decodeVoiceChunk(bytes))
    }

    @Test
    fun `decodeVoiceChunk rejects a length mismatch`() {
        val bytes = DeviceProtocol.encodeVoiceChunk(VoiceChunk(0, 1, byteArrayOf(9, 9, 9)))
        val corrupted = bytes.copyOf(bytes.size - 1)

        assertNull(DeviceProtocol.decodeVoiceChunk(corrupted))
    }

    @Test
    fun `chunkVoice at MTU 23 fits the tiny payload budget`() {
        val adpcm = ByteArray(100) { it.toByte() }

        val chunks = DeviceProtocol.chunkVoice(adpcm, mtu = 23)

        // budget = 23 - 3 (ATT) - 4 (header) = 16 bytes per chunk
        chunks.forEach { assertTrue(it.bytes.size <= 16) }
        assertEquals(chunks.size, chunks[0].total)
        assertArrayEquals(adpcm, DeviceProtocol.reassembleVoice(chunks))
    }

    @Test
    fun `chunkVoice at MTU 185 uses fewer, larger chunks`() {
        val adpcm = ByteArray(1000) { it.toByte() }

        val chunks = DeviceProtocol.chunkVoice(adpcm, mtu = 185)

        // budget = 185 - 3 - 4 = 178 bytes per chunk
        chunks.forEach { assertTrue(it.bytes.size <= 178) }
        assertArrayEquals(adpcm, DeviceProtocol.reassembleVoice(chunks))

        val chunksAt23 = DeviceProtocol.chunkVoice(adpcm, mtu = 23)
        assertTrue(chunks.size < chunksAt23.size)
    }

    @Test
    fun `reassembleVoice returns null when a seq is missing`() {
        val adpcm = ByteArray(50) { it.toByte() }
        val chunks = DeviceProtocol.chunkVoice(adpcm, mtu = 23)
        val withGap = chunks.filterIndexed { index, _ -> index != 1 }

        assertNull(DeviceProtocol.reassembleVoice(withGap))
    }

    @Test
    fun `reassembleVoice returns null for an empty chunk list`() {
        assertNull(DeviceProtocol.reassembleVoice(emptyList()))
    }
}
