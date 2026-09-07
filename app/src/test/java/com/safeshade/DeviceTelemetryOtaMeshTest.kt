package com.safeshade

import com.safeshade.data.DeviceModel
import com.safeshade.data.VitalsSource
import com.safeshade.device.DeviceProtocol
import com.safeshade.device.MeshAdvert
import com.safeshade.device.MeshAdvertCodec
import com.safeshade.device.MeshRole
import com.safeshade.device.OtaProtocol
import com.safeshade.device.OtaProtocol.OtaChunk
import com.safeshade.device.OtaProtocol.OtaStep
import com.safeshade.device.OtaProtocol.VersionReply
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the inbound TELEMETRY parse and for the three wire formats
 * designed ahead of firmware: `EXT VER`, the `OTA` chunk path, and the `MESH`
 * advertisement.
 *
 * The recurring theme is that the failure modes here are all *quiet*. A
 * six-field telemetry payload that stopped parsing would show a frozen sensor
 * page, not an error. An implausible heart rate rendered as a fact looks
 * exactly like a real one. A bare `ACK:VER` looks like a successful update. An
 * over-budget OTA chunk is truncated by the ATT layer rather than rejected.
 * None of those raise anything at runtime, so they are pinned here instead.
 */
class DeviceTelemetryOtaMeshTest {

    // ============================================
    // TELEMETRY - the six fields that already ship
    // ============================================

    @Test
    fun `six field telemetry parses exactly as before and leaves vitals null`() {
        val data = DeviceProtocol.parseTelemetry("0.10,-0.20,9.81,27.5,412,88")

        assertNotNull(data)
        assertEquals(0.10f, data!!.accelX, 0.001f)
        assertEquals(-0.20f, data.accelY, 0.001f)
        assertEquals(9.81f, data.accelZ, 0.001f)
        assertEquals(27.5f, data.temperature, 0.001f)
        assertEquals(412, data.lightLevel)
        assertEquals(88, data.batteryLevel)
        assertTrue(data.isRealData)
        assertNull(data.heartRateBpm)
        assertNull(data.spo2Percent)
        assertNull(data.skinTempC)
        assertNull(data.vitalsSource)
        assertFalse(data.hasVitals)
    }

    @Test
    fun `fewer than six fields is not telemetry`() {
        assertNull(DeviceProtocol.parseTelemetry("0.1,0.2,9.8,27.5,412"))
        assertNull(DeviceProtocol.parseTelemetry(""))
        assertNull(DeviceProtocol.parseTelemetry("nonsense"))
    }

    @Test
    fun `unparseable numeric fields fall back to zero rather than dropping the sample`() {
        val data = DeviceProtocol.parseTelemetry("x,y,z,q,w,e")

        assertNotNull(data)
        assertEquals(0f, data!!.accelX, 0.001f)
        assertEquals(0, data.batteryLevel)
        assertTrue(data.isRealData)
    }

    // ============================================
    // TELEMETRY - vitals in fields 7-9
    // ============================================

    @Test
    fun `vitals in fields seven to nine parse with a device source`() {
        val data = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,72,97,33.4")

        assertNotNull(data)
        assertEquals(72, data!!.heartRateBpm)
        assertEquals(97, data.spo2Percent)
        assertEquals(33.4f, data.skinTempC!!, 0.001f)
        assertEquals(VitalsSource.DEVICE, data.vitalsSource)
        assertTrue(data.hasVitals)
    }

    @Test
    fun `a dash and an empty field both mean no reading`() {
        val dashes = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,-,-,-")
        val empties = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,,,")

        for (data in listOf(dashes, empties)) {
            assertNotNull(data)
            assertNull(data!!.heartRateBpm)
            assertNull(data.spo2Percent)
            assertNull(data.skinTempC)
            assertNull(data.vitalsSource)
        }
    }

    @Test
    fun `one vital present is enough to mark the source`() {
        val data = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,-,97,-")

        assertNull(data!!.heartRateBpm)
        assertEquals(97, data.spo2Percent)
        assertEquals(VitalsSource.DEVICE, data.vitalsSource)
    }

    @Test
    fun `implausible vitals become no reading, never a clamped number`() {
        val data = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,4,12,80.0")

        assertNull(data!!.heartRateBpm)
        assertNull(data.spo2Percent)
        assertNull(data.skinTempC)
        // All three unbelievable is indistinguishable from no vitals at all.
        assertNull(data.vitalsSource)
    }

    @Test
    fun `vitals at the edges of the plausible ranges are kept`() {
        val low = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,25,50,20.0")!!
        val high = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,250,100,45.0")!!

        assertEquals(25, low.heartRateBpm)
        assertEquals(50, low.spo2Percent)
        assertEquals(20.0f, low.skinTempC!!, 0.001f)
        assertEquals(250, high.heartRateBpm)
        assertEquals(100, high.spo2Percent)
        assertEquals(45.0f, high.skinTempC!!, 0.001f)
    }

    @Test
    fun `a fractional heart rate is not an integer reading`() {
        val data = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,72.5,97,33.4")

        assertNull(data!!.heartRateBpm)
        assertEquals(97, data.spo2Percent)
    }

    @Test
    fun `extra fields beyond the ninth are ignored`() {
        val data = DeviceProtocol.parseTelemetry("0,0,9.8,27.5,412,88,72,97,33.4,something,else")

        assertEquals(72, data!!.heartRateBpm)
        assertEquals(97, data.spo2Percent)
        assertEquals(33.4f, data.skinTempC!!, 0.001f)
    }

    @Test
    fun `telemetry round trips through encode and parse`() {
        val payload = DeviceProtocol.encodeTelemetry(
            accelX = 0.12f, accelY = -0.34f, accelZ = 9.81f,
            temperature = 27.5f, lightLevel = 412, batteryLevel = 88,
            heartRateBpm = 72, spo2Percent = 97, skinTempC = 33.4f
        )

        assertEquals(9, payload.split(",").size)
        val data = DeviceProtocol.parseTelemetry(payload)!!
        assertEquals(0.12f, data.accelX, 0.001f)
        assertEquals(72, data.heartRateBpm)
        assertEquals(97, data.spo2Percent)
        assertEquals(33.4f, data.skinTempC!!, 0.001f)
    }

    @Test
    fun `encoding without vitals still emits nine fields`() {
        val payload = DeviceProtocol.encodeTelemetry(0f, 0f, 9.8f, 27.5f, 412, 88)

        assertEquals(9, payload.split(",").size)
        assertNull(DeviceProtocol.parseTelemetry(payload)!!.vitalsSource)
    }

    // ============================================
    // EXT VER
    // ============================================

    @Test
    fun `the ACK prefix strip leaves a colon payload intact`() {
        // Exactly what BleManager does before publishing to the acks flow.
        assertEquals("VER:1.2.3", "ACK:VER:1.2.3".removePrefix("ACK:"))
        assertEquals("1.2.3", DeviceProtocol.parseVersionAck("ACK:VER:1.2.3".removePrefix("ACK:")))
    }

    @Test
    fun `version query carries no payload`() {
        assertEquals("", DeviceProtocol.versionQuery())
    }

    @Test
    fun `parseVersionAck reads a semver and rejects everything else`() {
        assertEquals("1.2.3", DeviceProtocol.parseVersionAck("VER:1.2.3"))
        assertEquals("2.0.0-rc1", DeviceProtocol.parseVersionAck("VER:2.0.0-rc1"))
        assertNull(DeviceProtocol.parseVersionAck("VER"))
        assertNull(DeviceProtocol.parseVersionAck("VER:"))
        assertNull(DeviceProtocol.parseVersionAck("VER:OK"))
        assertNull(DeviceProtocol.parseVersionAck("SETTINGS"))
        assertNull(DeviceProtocol.parseVersionAck("VERBOSE:1.2.3"))
    }

    @Test
    fun `a bare ack is acknowledged-without-a-version, and silence is a timeout`() {
        assertEquals(VersionReply.AcknowledgedNoVersion, OtaProtocol.versionReply("VER"))
        assertEquals(VersionReply.AcknowledgedNoVersion, OtaProtocol.versionReply("VER:OK"))
        assertEquals(VersionReply.Timeout, OtaProtocol.versionReply(null))
        assertEquals(VersionReply.Version("1.2.3"), OtaProtocol.versionReply("VER:1.2.3"))
    }

    // ============================================
    // OTA - chunking against the MTU
    // ============================================

    @Test
    fun `every chunk fits the write budget at every realistic MTU`() {
        val sizes = listOf(1, 2, 3, 128, 3_000, 65_536)
        for (mtu in listOf(185, 247, 517)) {
            for (size in sizes) {
                val image = ByteArray(size) { (it * 31 and 0xFF).toByte() }
                val chunks = OtaProtocol.chunkFirmware(image, mtu)

                assertTrue("no chunks for size=$size mtu=$mtu", chunks.isNotEmpty())
                assertEquals(chunks.size, chunks[0].total)
                assertEquals(chunks.indices.toList(), chunks.map { it.seq })
                for (chunk in chunks) {
                    val onTheWire = "OTA:" + OtaProtocol.encodeOtaChunk(chunk)
                    assertTrue(
                        "size=$size mtu=$mtu chunk=${chunk.seq} was ${onTheWire.length} bytes",
                        onTheWire.toByteArray(Charsets.UTF_8).size <= mtu - 3
                    )
                }
            }
        }
    }

    @Test
    fun `an empty image produces no chunks`() {
        assertTrue(OtaProtocol.chunkFirmware(ByteArray(0), 247).isEmpty())
    }

    @Test
    fun `an MTU with no room for a base64 group produces no chunks`() {
        assertTrue(OtaProtocol.chunkFirmware(ByteArray(64), 23).isEmpty())
    }

    @Test
    fun `chunking an image large enough to need more than 255 chunks is not capped`() {
        val image = ByteArray(40_000) { it.toByte() }
        val chunks = OtaProtocol.chunkFirmware(image, 185)

        assertTrue("only ${chunks.size} chunks", chunks.size > 255)
        assertEquals(chunks.size, chunks[0].total)
    }

    // ============================================
    // OTA - encode, decode, CRC
    // ============================================

    @Test
    fun `a chunk round trips through encode and decode`() {
        val bytes = ByteArray(90) { (it * 7).toByte() }
        val encoded = OtaProtocol.encodeOtaChunk(OtaChunk(3, 10, bytes))

        val decoded = OtaProtocol.decodeOtaChunk(encoded)

        assertNotNull(decoded)
        assertEquals(3, decoded!!.seq)
        assertEquals(10, decoded.total)
        assertArrayEquals(bytes, decoded.bytes)
    }

    @Test
    fun `base64 slashes and pluses survive the decode split`() {
        // 0xFF 0xFF 0xFF encodes to "////" and 0xFB 0xEF 0xBE to "++++"; both
        // would be shredded by splitting the text on '/' before the commas.
        val bytes = byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFB.toByte(), 0xEF.toByte(), 0xBE.toByte()
        )
        val encoded = OtaProtocol.encodeOtaChunk(OtaChunk(0, 1, bytes))

        assertTrue(encoded.contains('/'))
        assertArrayEquals(bytes, OtaProtocol.decodeOtaChunk(encoded)!!.bytes)
    }

    @Test
    fun `a wrong CRC is refused rather than accepted`() {
        val encoded = OtaProtocol.encodeOtaChunk(OtaChunk(0, 1, byteArrayOf(1, 2, 3, 4)))
        val parts = encoded.split(",", limit = 3)
        val corrupted = "${parts[0]},deadbeef,${parts[2]}"

        assertNull(OtaProtocol.decodeOtaChunk(corrupted))
    }

    @Test
    fun `the CRC is compared case-insensitively`() {
        val encoded = OtaProtocol.encodeOtaChunk(OtaChunk(0, 1, byteArrayOf(9, 8, 7)))
        val parts = encoded.split(",", limit = 3)
        val upper = "${parts[0]},${parts[1].uppercase()},${parts[2]}"

        assertNotNull(OtaProtocol.decodeOtaChunk(upper))
    }

    @Test
    fun `the crc field is always eight hex digits`() {
        // Fixed field width is what chunkFirmware budgets against.
        for (size in listOf(1, 5, 40, 300)) {
            val encoded = OtaProtocol.encodeOtaChunk(OtaChunk(0, 1, ByteArray(size) { it.toByte() }))
            assertEquals(8, encoded.split(",")[1].length)
        }
    }

    @Test
    fun `malformed chunk text decodes to null instead of throwing`() {
        assertNull(OtaProtocol.decodeOtaChunk(""))
        assertNull(OtaProtocol.decodeOtaChunk("0/1,deadbeef"))
        assertNull(OtaProtocol.decodeOtaChunk("0-1,deadbeef,AAAA"))
        assertNull(OtaProtocol.decodeOtaChunk("a/1,deadbeef,AAAA"))
        assertNull(OtaProtocol.decodeOtaChunk("5/2,deadbeef,AAAA"))
        assertNull(OtaProtocol.decodeOtaChunk("0/1,deadbeef,not base64!"))
    }

    @Test
    fun `a captured wire line with its OTA tag still decodes`() {
        val encoded = OtaProtocol.encodeOtaChunk(OtaChunk(0, 1, byteArrayOf(4, 5, 6)))

        assertArrayEquals(byteArrayOf(4, 5, 6), OtaProtocol.decodeOtaChunk("OTA:$encoded")!!.bytes)
    }

    // ============================================
    // OTA - reassembly
    // ============================================

    @Test
    fun `chunking then reassembling returns the original image`() {
        val image = ByteArray(5_000) { (it * 13 and 0xFF).toByte() }
        val chunks = OtaProtocol.chunkFirmware(image, 247)

        assertArrayEquals(image, OtaProtocol.reassemble(chunks))
    }

    @Test
    fun `reassembly survives an out of order arrival`() {
        val image = ByteArray(900) { it.toByte() }
        val chunks = OtaProtocol.chunkFirmware(image, 185)

        assertArrayEquals(image, OtaProtocol.reassemble(chunks.reversed()))
    }

    @Test
    fun `an incomplete or inconsistent set never reassembles`() {
        val image = ByteArray(900) { it.toByte() }
        val chunks = OtaProtocol.chunkFirmware(image, 185)

        assertNull(OtaProtocol.reassemble(emptyList()))
        assertNull("a missing chunk must not become a short image", OtaProtocol.reassemble(chunks.drop(1)))
        assertNull(
            "a duplicate must not stand in for the missing one",
            OtaProtocol.reassemble(chunks.dropLast(1) + chunks[0])
        )
        assertNull(
            "chunks disagreeing on total are not one image",
            OtaProtocol.reassemble(chunks.mapIndexed { i, c -> if (i == 0) c.copy(total = c.total + 1) else c })
        )
    }

    // ============================================
    // OTA - image identity
    // ============================================

    @Test
    fun `sha256 matches the published vector for abc`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            OtaProtocol.sha256Hex("abc".toByteArray(Charsets.UTF_8))
        )
    }

    @Test
    fun `sha256 matches the published vector for the empty input`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            OtaProtocol.sha256Hex(ByteArray(0))
        )
    }

    @Test
    fun `verifySha256 ignores case and rejects a different image`() {
        val bytes = "abc".toByteArray(Charsets.UTF_8)
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

        assertTrue(OtaProtocol.verifySha256(bytes, expected))
        assertTrue(OtaProtocol.verifySha256(bytes, expected.uppercase()))
        assertFalse(OtaProtocol.verifySha256("abd".toByteArray(Charsets.UTF_8), expected))
    }

    // ============================================
    // OTA - what counts as installed
    // ============================================

    @Test
    fun `only a matching version reply counts as installed`() {
        assertEquals(
            OtaStep.Installed("1.4.0"),
            OtaProtocol.afterVersionReply("1.4.0", VersionReply.Version("1.4.0"))
        )
    }

    @Test
    fun `a bare acknowledgement after install is a failure in those exact words`() {
        val step = OtaProtocol.afterVersionReply("1.4.0", VersionReply.AcknowledgedNoVersion)

        assertEquals(
            OtaStep.Failed("The device acknowledged the update but reported no version"),
            step
        )
    }

    @Test
    fun `a different version and a silent device are both failures`() {
        val mismatch = OtaProtocol.afterVersionReply("1.4.0", VersionReply.Version("1.3.9"))
        val timeout = OtaProtocol.afterVersionReply("1.4.0", VersionReply.Timeout)

        assertTrue(mismatch is OtaStep.Failed)
        assertTrue((mismatch as OtaStep.Failed).reason.contains("1.3.9"))
        assertTrue(mismatch.reason.contains("1.4.0"))
        assertTrue(timeout is OtaStep.Failed)
    }

    // ============================================
    // MESH advertisement
    // ============================================

    @Test
    fun `a mesh advert round trips`() {
        val advert = MeshAdvert("aa11bb22", MeshRole.WEARER, 74, 120)

        assertEquals("aa11bb22,wearer,74,120", MeshAdvertCodec.encode(advert))
        assertEquals(advert, MeshAdvertCodec.parse(MeshAdvertCodec.encode(advert)))
    }

    @Test
    fun `null numbers encode as empty fields and parse back as null`() {
        val advert = MeshAdvert("aa11bb22", MeshRole.RELAY, null, null)

        assertEquals("aa11bb22,relay,,", MeshAdvertCodec.encode(advert))
        val parsed = MeshAdvertCodec.parse("aa11bb22,relay,,")!!
        assertNull(parsed.batteryPercent)
        assertNull(parsed.lastFixAgeSeconds)
        assertEquals(MeshRole.RELAY, parsed.role)
    }

    @Test
    fun `a comma in the device id cannot shift the later fields`() {
        val encoded = MeshAdvertCodec.encode(MeshAdvert("aa,11", MeshRole.WEARER, 50, 10))

        assertEquals("aa 11,wearer,50,10", encoded)
        assertEquals(50, MeshAdvertCodec.parse(encoded)!!.batteryPercent)
    }

    @Test
    fun `an unknown role is still a device that is there`() {
        val parsed = MeshAdvertCodec.parse("aa11bb22,beacon,50,10")!!

        assertEquals(MeshRole.UNKNOWN, parsed.role)
        assertEquals("aa11bb22", parsed.deviceId)
    }

    @Test
    fun `non-numeric and out of range numbers become no reading`() {
        val parsed = MeshAdvertCodec.parse("aa11bb22,wearer,plenty,-5")!!

        assertNull(parsed.batteryPercent)
        assertNull(parsed.lastFixAgeSeconds)
    }

    @Test
    fun `missing trailing fields parse as null rather than failing`() {
        val parsed = MeshAdvertCodec.parse("aa11bb22,wearer")!!

        assertEquals(MeshRole.WEARER, parsed.role)
        assertNull(parsed.batteryPercent)
        assertNull(parsed.lastFixAgeSeconds)
    }

    @Test
    fun `a frame with no identity is rejected`() {
        assertNull(MeshAdvertCodec.parse(""))
        assertNull(MeshAdvertCodec.parse("aa11bb22"))
        assertNull(MeshAdvertCodec.parse(",wearer,50,10"))
    }

    @Test
    fun `manufacturer data decodes the same text`() {
        val bytes = "aa11bb22,relay,60,30".toByteArray(Charsets.UTF_8)

        val parsed = MeshAdvertCodec.parseManufacturerData(bytes)!!

        assertEquals("aa11bb22", parsed.deviceId)
        assertEquals(MeshRole.RELAY, parsed.role)
        assertEquals(60, parsed.batteryPercent)
        assertEquals(30, parsed.lastFixAgeSeconds)
    }

    @Test
    fun `empty or foreign manufacturer data is not a SafeShade`() {
        assertNull(MeshAdvertCodec.parseManufacturerData(ByteArray(0)))
        assertNull(MeshAdvertCodec.parseManufacturerData(byteArrayOf(0x01, 0x02, 0x03)))
    }

    // ============================================
    // DeviceModel
    // ============================================

    @Test
    fun `the wire names are the three the firmware_releases check constraint accepts`() {
        assertEquals(
            listOf("s1", "5g", "spark"),
            DeviceModel.entries.map { it.wireName }
        )
    }

    @Test
    fun `fromWire is case-insensitive and null for anything unknown`() {
        assertEquals(DeviceModel.S1, DeviceModel.fromWire("s1"))
        assertEquals(DeviceModel.FIVE_G, DeviceModel.fromWire("5G"))
        assertEquals(DeviceModel.SPARK, DeviceModel.fromWire(" spark "))
        assertNull(DeviceModel.fromWire("s2"))
        assertNull(DeviceModel.fromWire(null))
        assertNull(DeviceModel.fromWire(""))
    }

    @Test
    fun `fromAdvertisedName reads the model out of the name, defaulting to S1`() {
        assertEquals(DeviceModel.SPARK, DeviceModel.fromAdvertisedName("SafeShade Spark"))
        assertEquals(DeviceModel.SPARK, DeviceModel.fromAdvertisedName("safeshade spark 02"))
        assertEquals(DeviceModel.FIVE_G, DeviceModel.fromAdvertisedName("SafeShade 5G"))
        assertEquals(DeviceModel.S1, DeviceModel.fromAdvertisedName("SafeShade S1"))
        assertEquals(DeviceModel.S1, DeviceModel.fromAdvertisedName("SafeShade"))
    }
}
