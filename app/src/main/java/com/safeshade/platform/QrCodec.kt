package com.safeshade.platform

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.safeshade.data.MedicalId

/**
 * The QR emergency card.
 *
 * The deck promises "NFC + QR Emergency Access". QR is the half that works on
 * any phone anyone happens to be carrying, needs no account, no network and no
 * app — which is exactly the situation this is for: a stranger, a paramedic, or
 * a passer-by looking at an unconscious person.
 *
 * Two decisions follow from that, and both matter more than they look:
 *
 *  1. **The payload is plain readable text, not a URL and not a custom scheme.**
 *     Every phone camera shows the decoded text of a plain-text QR inline. A
 *     URL would need a server that does not exist, would fail with no signal,
 *     and would put a stranger's tap between the responder and the blood type.
 *  2. **Error correction is set to H (~30%).** This code will be printed,
 *     folded into a wallet, photographed off a screen at an angle, and read in
 *     bad light. Redundancy is worth more here than a smaller symbol.
 */
object QrCodec {

    /**
     * Encodes [text] as a QR bitmap.
     *
     * Call this off the main thread — encoding a dense symbol at 640px is tens
     * of milliseconds, which is a dropped frame if it runs in composition.
     */
    fun encode(
        text: String,
        sizePx: Int = 640,
        foreground: Int = AndroidColor.BLACK,
        background: Int = AndroidColor.WHITE
    ): ImageBitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            // A quiet zone of 1 module. The spec asks for 4, but the card draws
            // its own generous white margin around the symbol, so baking in
            // more padding just shrinks the modules for no readability gain.
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val row = y * sizePx
            for (x in 0 until sizePx) {
                pixels[row + x] = if (matrix[x, y]) foreground else background
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            .apply { setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx) }
            .asImageBitmap()
    }.getOrNull()

    /**
     * The card's text.
     *
     * Ordered the way a responder reads: who, then what would change treatment,
     * then who to call. Blank fields are omitted entirely rather than printed
     * as "Allergies: —", because an empty label invites the reading that the
     * answer is "none" when it actually means "nobody filled this in".
     *
     * ZXing's version selection grows the symbol with the payload, so a long
     * card still encodes; it just gets denser. The medical editor caps the
     * individual fields, which keeps this comfortably scannable in practice.
     */
    fun medicalCardText(medicalId: MedicalId, wearerName: String): String = buildString {
        appendLine("SAFESHADE EMERGENCY CARD")
        if (wearerName.isNotBlank()) appendLine("Name: $wearerName")
        if (medicalId.age > 0) appendLine("Age: ${medicalId.age}")
        if (medicalId.bloodType.isNotBlank()) appendLine("Blood type: ${medicalId.bloodType}")
        if (medicalId.allergies.isNotBlank()) appendLine("Allergies: ${medicalId.allergies}")
        if (medicalId.conditions.isNotBlank()) appendLine("Conditions: ${medicalId.conditions}")
        if (medicalId.medications.isNotBlank()) appendLine("Medications: ${medicalId.medications}")
        if (medicalId.organDonor) appendLine("Organ donor: Yes")
        if (medicalId.emergencyContact.isNotBlank()) {
            val name = medicalId.contactName.ifBlank { "Emergency contact" }
            appendLine("Call: $name ${medicalId.emergencyContact}")
        }
        if (medicalId.secondaryContact.isNotBlank()) {
            val name = medicalId.secondaryContactName.ifBlank { "Second contact" }
            appendLine("Also: $name ${medicalId.secondaryContact}")
        }
        if (medicalId.notes.isNotBlank()) appendLine("Notes: ${medicalId.notes}")
    }.trim()
}
