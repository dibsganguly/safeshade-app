package com.safeshade.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.provider.Settings
import java.io.IOException

/**
 * Writing the medical card to an NFC tag.
 *
 * ### The test phone has no NFC
 *
 * The Redmi Note 10S this app is verified on carries no NFC radio at all —
 * `NfcAdapter.getDefaultAdapter` returns `null` on it, full stop. [NoNfc] has
 * to be the state that photograph, and reaching it must never touch the
 * adapter beyond that one null check: no `isEnabled`, no reader-mode call,
 * nothing that could throw or hang on hardware that is not there.
 *
 * ### Reader mode is the Activity's job
 *
 * Enabling `NfcAdapter.enableReaderMode` needs a live Activity (it is bound
 * to the foreground window), so it does not belong in this file. MainActivity
 * must call it from `onResume` whenever a screen has armed a "write mode"
 * flag, and `disableReaderMode` from `onPause` unconditionally; a discovered
 * `Tag` is handed to [write] with a message built by [payloadFor].
 */
sealed interface NfcAvailability {
    /** An adapter exists and is switched on — a write can be attempted. */
    data object Available : NfcAvailability

    /** No NFC radio on this device. The state the unsupported test phone reaches. */
    data object NoNfc : NfcAvailability

    /** An adapter exists but the user has NFC turned off; [settingsIntent] opens the toggle. */
    data class Disabled(val settingsIntent: Intent) : NfcAvailability
}

/** Reads the current [NfcAvailability] without ever calling anything beyond the null check on a phone with no adapter. */
fun nfcAvailability(context: Context): NfcAvailability {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcAvailability.NoNfc
    return if (adapter.isEnabled) {
        NfcAvailability.Available
    } else {
        NfcAvailability.Disabled(Intent(Settings.ACTION_NFC_SETTINGS))
    }
}

/**
 * The result of one [NfcTagWriter.write] attempt.
 *
 * A tag can fail in ways that are the *tag's* fault, not the app's or the
 * radio's, and the UI needs to tell those apart to say something useful:
 * "this card is full" reads completely differently from "this card is
 * locked" or "hold still".
 */
sealed interface NfcWriteResult {
    /** Wrote successfully. [bytes] is the size of the message that was written. */
    data class Written(val bytes: Int) : NfcWriteResult

    /** The tag is NDEF-capable but too small for this message. */
    data class TooSmall(val capacity: Int, val needed: Int) : NfcWriteResult

    /** The tag (or one of its records) is locked against further writes. */
    data object ReadOnly : NfcWriteResult

    /** Anything else — lost mid-write, not NDEF-formatable, an IOException. */
    data class Failed(val reason: String) : NfcWriteResult
}

/**
 * The card's payload, kept apart from [NdefRecord] construction so its one
 * real decision — how much of the medical summary fits — is testable on the
 * JVM. `NdefRecord`/`NdefMessage` are Android classes with no local-JVM
 * fake, which is why the round-trip through them is not unit-tested here.
 */
object NfcPayload {
    /**
     * A physical card has no scroll bar. 200 characters is roughly what a
     * small card's reader app shows in full without truncating a second time
     * itself — this is the one truncation, and it is length-based rather than
     * word-based so the guarantee ("never over the limit") holds regardless
     * of the text's language or punctuation.
     */
    const val MAX_SUMMARY_LENGTH = 200

    /** The medical-summary text record body, trimmed and capped at [MAX_SUMMARY_LENGTH]. */
    fun textFor(medicalSummary: String, maxLength: Int = MAX_SUMMARY_LENGTH): String {
        val trimmed = medicalSummary.trim()
        return if (trimmed.length <= maxLength) trimmed else trimmed.take(maxLength)
    }
}

/** Builds and writes the two-record NDEF message a SafeShade card carries. */
object NfcTagWriter {

    /**
     * A URI record pointing at the in-app card (`safeshade://card?...`), plus
     * a plain-text record of the medical summary so a phone with no SafeShade
     * installed can still read *something* off the card in its own NFC app.
     *
     * Fields are `Uri.encode`d individually into the query string — unlike the
     * BLE payloads elsewhere in the app, this is not comma-delimited, so
     * commas in [wearerName] are not a parser hazard here and are left alone.
     */
    fun payloadFor(wearerName: String, medicalSummary: String, sosNumber: String?): NdefMessage {
        val query = buildString {
            append("name=").append(Uri.encode(wearerName))
            if (!sosNumber.isNullOrBlank()) {
                append("&sos=").append(Uri.encode(sosNumber))
            }
        }
        val uriRecord = NdefRecord.createUri("safeshade://card?$query")
        val textRecord = NdefRecord.createTextRecord("en", NfcPayload.textFor(medicalSummary))
        return NdefMessage(arrayOf(uriRecord, textRecord))
    }

    /**
     * Writes [message] to [tag].
     *
     * Two paths: a tag already NDEF-formatted (`Ndef`) is written in place; a
     * blank tag (`NdefFormatable`) is formatted with the message directly,
     * which is the one call that both formats and writes in one pass. A tag
     * that is neither — most non-NDEF transit/access cards — fails with a
     * named reason rather than a generic exception message.
     */
    fun write(tag: Tag, message: NdefMessage): NfcWriteResult {
        val needed = message.toByteArray().size

        Ndef.get(tag)?.let { ndef ->
            return try {
                ndef.connect()
                when {
                    !ndef.isWritable -> NfcWriteResult.ReadOnly
                    ndef.maxSize < needed -> NfcWriteResult.TooSmall(ndef.maxSize, needed)
                    else -> {
                        ndef.writeNdefMessage(message)
                        NfcWriteResult.Written(needed)
                    }
                }
            } catch (e: TagLostException) {
                NfcWriteResult.Failed("Tag moved away before the write finished")
            } catch (e: IOException) {
                NfcWriteResult.Failed(e.message ?: "I/O error writing the tag")
            } catch (e: FormatException) {
                NfcWriteResult.Failed(e.message ?: "Tag rejected the message format")
            } finally {
                runCatching { ndef.close() }
            }
        }

        val formatable = NdefFormatable.get(tag)
            ?: return NfcWriteResult.Failed("This tag does not support NDEF")
        return try {
            formatable.connect()
            formatable.format(message)
            NfcWriteResult.Written(needed)
        } catch (e: TagLostException) {
            NfcWriteResult.Failed("Tag moved away before the write finished")
        } catch (e: IOException) {
            NfcWriteResult.Failed(e.message ?: "I/O error formatting the tag")
        } catch (e: FormatException) {
            NfcWriteResult.Failed(e.message ?: "Tag could not be formatted")
        } finally {
            runCatching { formatable.close() }
        }
    }
}
