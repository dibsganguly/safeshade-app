package com.safeshade.device

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.CRC32

/**
 * Sending a firmware image to the wearable, and knowing whether it landed.
 *
 * The wire form is the one named in `DeviceCapabilities.awaitingFirmware`:
 * `OTA:<seq>/<total>,<crc32>,<base64>`, one chunk per EXT write.
 *
 * ### Why a bare acknowledgement proves nothing here
 *
 * The shipped firmware acknowledges *every* `EXT` tag it receives, including
 * tags it has never heard of, with a bare `ACK:<tag>`. So an `ACK:OTA` says
 * only that a write arrived — not that the bytes were stored, not that the CRC
 * matched, not that anything was flashed. A progress bar driven by counting
 * acks would run smoothly to 100% against firmware with no OTA support at all.
 *
 * The only success criterion is therefore external to the transfer: after the
 * last chunk, query `EXT VER` and require the answer to be the version of the
 * release that was just sent. See [afterVersionReply]. A bare `ACK:VER` with no
 * version — the same universal acknowledgement again — is not success and is
 * reported as a failure in those words.
 *
 * ### Why the per-chunk CRC is still worth carrying
 *
 * It is the only thing the receiving side can check for itself. The ATT layer
 * truncates an over-long write instead of rejecting it, exactly as it does for
 * the text payloads in [DeviceProtocol], so a chunk can arrive short and
 * well-formed. A CRC32 over the raw bytes turns that silent corruption into a
 * chunk the device can refuse.
 *
 * Pure Kotlin with no Android imports, so all of it is unit testable —
 * `java.util.Base64` rather than `android.util.Base64` for that reason.
 */
object OtaProtocol {

    /** ATT overhead: 3 bytes of opcode + handle on every write, as in [DeviceProtocol]. */
    private const val ATT_OVERHEAD = 3

    /**
     * The tag `BleManager.sendExtCommand` prepends, counted against the budget.
     *
     * It writes `"$tag:$payload"`, so the four characters of `OTA:` are on the
     * wire whether or not [encodeOtaChunk] emits them. Leaving them out of the
     * budget would put every chunk four bytes over the MTU, and the loss would
     * be a silently truncated base64 tail rather than an error.
     */
    private const val EXT_TAG_BYTES = 4

    /** `<seq>` `/` `<total>` `,` `<crc32hex>` `,` — the fixed punctuation and the 8 hex digits. */
    private const val FIXED_HEADER_BYTES = 1 + 1 + 8 + 1

    /** One chunk of the image: [seq] of [total], carrying raw (not yet base64) [bytes]. */
    data class OtaChunk(val seq: Int, val total: Int, val bytes: ByteArray)

    /**
     * Splits [image] into chunks that each fit one write at [mtu].
     *
     * Budgeting runs to a fixed point because the header width is circular:
     * the number of digits in `total` depends on how many chunks there are,
     * which depends on how much room the header leaves. Two or three passes
     * settle it; the loop is bounded regardless.
     *
     * Base64 inflates by 4/3, so the raw bytes per chunk are the base64 budget
     * rounded down to a whole 4-character group and multiplied back by 3.
     * Unlike `DeviceProtocol.chunkVoice`, `total` is not capped at 255 — a
     * firmware image is far larger than a push-to-talk clip and `seq`/`total`
     * are decimal text here rather than single header bytes, so there is
     * nothing to overflow.
     *
     * Returns an empty list for an empty image, and also for an [mtu] so small
     * that no whole base64 group fits after the header — there is no chunking
     * of that image at that MTU, and returning empty says so rather than
     * emitting frames that would be truncated on the way out.
     */
    fun chunkFirmware(image: ByteArray, mtu: Int): List<OtaChunk> {
        if (image.isEmpty()) return emptyList()

        val budget = mtu - ATT_OVERHEAD
        var digits = 1
        var rawPerChunk = 0
        var total = 0

        repeat(6) {
            val header = EXT_TAG_BYTES + FIXED_HEADER_BYTES + digits * 2
            val base64Budget = budget - header
            rawPerChunk = (base64Budget / 4) * 3
            if (rawPerChunk <= 0) return emptyList()
            total = (image.size + rawPerChunk - 1) / rawPerChunk
            val settled = total.toString().length
            if (settled == digits) return@repeat
            digits = settled
        }

        // One last recompute at the settled width, in case the loop's final
        // pass widened the digits without re-budgeting.
        val header = EXT_TAG_BYTES + FIXED_HEADER_BYTES + total.toString().length * 2
        rawPerChunk = ((budget - header) / 4) * 3
        if (rawPerChunk <= 0) return emptyList()
        total = (image.size + rawPerChunk - 1) / rawPerChunk

        val perChunk = rawPerChunk
        return (0 until total).map { seq ->
            val start = seq * perChunk
            val end = minOf(start + perChunk, image.size)
            OtaChunk(seq, total, image.copyOfRange(start, end))
        }
    }

    /**
     * `<seq>/<total>,<crc32hex>,<base64>` — the payload half, without the
     * `OTA:` tag that `sendExtCommand` prepends.
     *
     * The CRC32 is over the raw bytes, not over the base64, so it verifies the
     * firmware image rather than its transport encoding. It is rendered as
     * eight lower-case hex digits, zero-padded, so the field width is fixed and
     * the budget in [chunkFirmware] can rely on it.
     */
    fun encodeOtaChunk(chunk: OtaChunk): String {
        require(chunk.seq >= 0) { "seq must not be negative: ${chunk.seq}" }
        require(chunk.total >= 1) { "total must be at least 1: ${chunk.total}" }
        return "${chunk.seq}/${chunk.total},${crc32Hex(chunk.bytes)},${Base64.getEncoder().encodeToString(chunk.bytes)}"
    }

    /**
     * Inverse of [encodeOtaChunk]. Null on anything malformed, and — the point
     * of the field — null when the CRC does not match the decoded bytes.
     *
     * Split order matters: standard base64 contains `/` and `+`, so the text is
     * cut into three comma-separated parts *first* and only the first part is
     * then split on `/`. Splitting on `/` up front would shred the payload of
     * most chunks. A leading `OTA:` is tolerated so that a captured wire line
     * can be fed in unchanged.
     */
    fun decodeOtaChunk(text: String): OtaChunk? {
        val body = text.trim().removePrefix("OTA:")
        val parts = body.split(",", limit = 3)
        if (parts.size < 3) return null

        val counters = parts[0].split("/")
        if (counters.size != 2) return null
        val seq = counters[0].trim().toIntOrNull() ?: return null
        val total = counters[1].trim().toIntOrNull() ?: return null
        if (seq < 0 || total < 1 || seq >= total) return null

        val bytes = try {
            Base64.getDecoder().decode(parts[2].trim())
        } catch (e: IllegalArgumentException) {
            return null
        }

        if (!parts[1].trim().equals(crc32Hex(bytes), ignoreCase = true)) return null
        return OtaChunk(seq, total, bytes)
    }

    /**
     * Reassembles chunks in `seq` order, or null when the set is incomplete or
     * inconsistent — a missing `seq`, a duplicate, or chunks disagreeing on
     * `total`. The same rule as `DeviceProtocol.reassembleVoice`, and for a
     * stronger reason: a partial firmware image that was written anyway would
     * brick the device.
     */
    fun reassemble(chunks: List<OtaChunk>): ByteArray? {
        if (chunks.isEmpty()) return null
        val total = chunks[0].total
        if (chunks.any { it.total != total } || chunks.size != total) return null
        val bySeq = chunks.associateBy { it.seq }
        if (bySeq.size != total) return null

        val out = ByteArrayOutputStream()
        for (seq in 0 until total) {
            val chunk = bySeq[seq] ?: return null
            out.write(chunk.bytes)
        }
        return out.toByteArray()
    }

    /** Lower-case, zero-padded to eight hex digits so the field width never varies. */
    private fun crc32Hex(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return "%08x".format(crc.value)
    }

    /**
     * SHA-256 of [bytes] as lower-case hex.
     *
     * The per-chunk CRC32 catches a truncated or corrupted write; this checks
     * that the whole image is the one the release actually published, which is
     * a different question and the one that matters before anything is sent.
     */
    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    /** Case-insensitive, because a digest copied from a release manifest may be upper-case. */
    fun verifySha256(bytes: ByteArray, expectedHex: String): Boolean =
        sha256Hex(bytes).equals(expectedHex.trim(), ignoreCase = true)

    // ============================================
    // EXT VER - what the device answered, and what it means
    // ============================================

    /**
     * The three outcomes of asking the wearable its firmware version.
     *
     * [AcknowledgedNoVersion] is a distinct state rather than a flavour of
     * failure because it is the *expected* answer from the shipped firmware:
     * every EXT tag is acknowledged, known or not, so a bare `ACK:VER` means
     * the write arrived and nothing more. Collapsing it into [Timeout] would
     * lose the one fact it carries — the link is alive — and collapsing it into
     * [Version] would invent a version that was never reported.
     */
    sealed interface VersionReply {
        data class Version(val semver: String) : VersionReply
        data object AcknowledgedNoVersion : VersionReply
        data object Timeout : VersionReply
    }

    /**
     * Maps an acknowledgement tag — already stripped of its `ACK:` prefix by
     * `BleManager`, or null when the wait timed out — onto [VersionReply].
     *
     * Anything that is not a well-formed `VER:<semver>` is
     * [VersionReply.AcknowledgedNoVersion]: the device said something, but not
     * a version. See `DeviceProtocol.parseVersionAck` for what counts as
     * well-formed and why the bar is set there.
     */
    fun versionReply(ackTag: String?): VersionReply {
        if (ackTag == null) return VersionReply.Timeout
        val semver = DeviceProtocol.parseVersionAck(ackTag)
        return if (semver != null) VersionReply.Version(semver) else VersionReply.AcknowledgedNoVersion
    }

    // ============================================
    // The state a screen would show
    // ============================================

    /**
     * Where an update has got to.
     *
     * The order of the steps is the order of the guarantees. Nothing reaches
     * [Sending] until the downloaded image has been checked against the
     * release's SHA-256, and nothing reaches [Installed] on the strength of the
     * transfer alone — [AwaitingDeviceVersion] exists precisely because the
     * chunk acks prove nothing.
     */
    sealed interface OtaStep {
        data object Idle : OtaStep
        data class Downloading(val progress: Float) : OtaStep
        data object Verifying : OtaStep
        data class VerifyFailed(val reason: String) : OtaStep
        data class Sending(val sent: Int, val total: Int) : OtaStep
        data object AwaitingDeviceVersion : OtaStep
        data class Installed(val version: String) : OtaStep
        data class Failed(val reason: String) : OtaStep
    }

    /**
     * Turns the post-install version query into the final step.
     *
     * This is the only place an update is allowed to be declared successful,
     * and the rule is narrow on purpose: the device must report the exact
     * version that was sent. A different version means the old firmware is
     * still running, a bare acknowledgement means the device answered without
     * saying what it is running, and silence means it never answered — none of
     * the three is an installed update, and none of them may look like one.
     */
    fun afterVersionReply(expectedVersion: String, reply: VersionReply): OtaStep = when (reply) {
        is VersionReply.Version ->
            if (reply.semver == expectedVersion) {
                OtaStep.Installed(reply.semver)
            } else {
                OtaStep.Failed("The device is running ${reply.semver}, not $expectedVersion")
            }

        VersionReply.AcknowledgedNoVersion ->
            OtaStep.Failed("The device acknowledged the update but reported no version")

        VersionReply.Timeout ->
            OtaStep.Failed("The device did not report its version after the update")
    }
}
