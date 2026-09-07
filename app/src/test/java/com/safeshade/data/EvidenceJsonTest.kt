package com.safeshade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `safeshade_evidence` codec.
 *
 * The cases that matter are not the happy round trip: they are a blob written
 * by an older build (fields absent), a row that cannot name a file, and a
 * corrupt blob - because all three arrive inside a DataStore `map`, where a
 * throw kills every collector of the flow rather than failing one read.
 */
class EvidenceJsonTest {

    private val clip = EvidenceClip(
        id = "clip-1",
        file = "abc.m4a",
        capturedAt = 1_700_000_000_000L,
        durationMs = 30_000,
        byteSize = 121_344L,
        alertId = "trip-9",
        upload = EvidenceUploadState.QUEUED,
        uploadReason = null
    )

    @Test
    fun `a clip survives the round trip unchanged`() {
        val back = EvidenceJson.decodeClips(EvidenceJson.encodeClips(listOf(clip)))
        assertEquals(listOf(clip), back)
    }

    @Test
    fun `a failure keeps its reason`() {
        val failed = clip.copy(
            upload = EvidenceUploadState.FAILED,
            uploadReason = "No network when the upload was attempted"
        )
        val back = EvidenceJson.decodeClips(EvidenceJson.encodeClips(listOf(failed)))
        assertEquals("No network when the upload was attempted", back.single().uploadReason)
        assertEquals(EvidenceUploadState.FAILED, back.single().upload)
    }

    @Test
    fun `order is preserved so the cap drops the oldest`() {
        val many = (1..5).map { clip.copy(id = "clip-$it", file = "$it.m4a") }
        assertEquals(
            listOf("clip-1", "clip-2", "clip-3", "clip-4", "clip-5"),
            EvidenceJson.decodeClips(EvidenceJson.encodeClips(many)).map { it.id }
        )
    }

    @Test
    fun `an empty or absent blob is an empty list`() {
        assertTrue(EvidenceJson.decodeClips(null).isEmpty())
        assertTrue(EvidenceJson.decodeClips("").isEmpty())
        assertTrue(EvidenceJson.decodeClips("   ").isEmpty())
    }

    @Test
    fun `a corrupt blob is an empty list, not an exception`() {
        assertTrue(EvidenceJson.decodeClips("{not json at all").isEmpty())
        assertTrue(EvidenceJson.decodeClips("\"a bare string\"").isEmpty())
    }

    @Test
    fun `a row that names no file is dropped rather than resurrected`() {
        val json = """[{"id":"a"},{"file":"b.m4a"},{"id":"c","file":"c.m4a"}]"""
        val back = EvidenceJson.decodeClips(json)
        assertEquals(listOf("c"), back.map { it.id })
    }

    @Test
    fun `missing fields take honest defaults`() {
        val json = """[{"id":"a","file":"a.m4a"}]"""
        val only = EvidenceJson.decodeClips(json).single()
        assertEquals(0L, only.capturedAt)
        assertEquals(0, only.durationMs)
        assertEquals(0L, only.byteSize)
        assertNull(only.alertId)
        assertEquals(EvidenceClip.KIND_AUDIO, only.kind)
        // The only claim safe to make about a row that does not say: it is here
        // and nowhere else.
        assertEquals(EvidenceUploadState.LOCAL_ONLY, only.upload)
    }

    @Test
    fun `an unknown upload state reads as local only`() {
        val json = """[{"id":"a","file":"a.m4a","upload":"HALFWAY_THERE"}]"""
        assertEquals(
            EvidenceUploadState.LOCAL_ONLY,
            EvidenceJson.decodeClips(json).single().upload
        )
    }

    @Test
    fun `settings round trip and clamp an impossible duration`() {
        val settings = EvidenceSettings(
            recordOnSos = true,
            recordOnFall = true,
            durationSeconds = 45,
            uploadToCloud = true
        )
        assertEquals(settings, EvidenceJson.decodeSettings(EvidenceJson.encodeSettings(settings)))

        assertEquals(
            MAX_EVIDENCE_SECONDS,
            EvidenceJson.decodeSettings("""{"durationSeconds":9000}""").durationSeconds
        )
        assertEquals(
            MIN_EVIDENCE_SECONDS,
            EvidenceJson.decodeSettings("""{"durationSeconds":1}""").durationSeconds
        )
    }

    @Test
    fun `absent settings are the off-by-default ones`() {
        val defaults = EvidenceJson.decodeSettings(null)
        assertEquals(false, defaults.recordOnSos)
        assertEquals(false, defaults.recordOnFall)
        assertEquals(false, defaults.uploadToCloud)
        assertEquals(30, defaults.durationSeconds)
        assertEquals(defaults, EvidenceJson.decodeSettings("not json"))
    }
}
