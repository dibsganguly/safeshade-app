package com.safeshade.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.safeshade.data.EmergencyContact
import com.safeshade.data.MedicalId
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A one-shot, printable record of a single incident: what happened, when, who
 * was contacted, and the wearer's medical ID — the thing a guardian hands to a
 * responder, or keeps for their own record, after a fall or an SOS.
 *
 * Built on [PdfDocument] directly rather than a PDF library, since the layout
 * is a handful of text blocks on an A4 page and a real dependency would be a
 * lot of weight for that. The layout math itself — what text goes on which
 * line, how many pages that becomes — lives in [layoutLines] and [pagesFor],
 * both plain functions with no Android types in their signature, so the
 * layout can be unit-tested on the JVM; [render] is the thin pass that walks
 * that output with a [Canvas].
 */
object IncidentPdf {

    private const val PAGE_WIDTH_PT = 595f
    private const val PAGE_HEIGHT_PT = 842f
    private const val MARGIN_PT = 48f
    private const val BODY_TEXT_SIZE_PT = 11f
    private const val LINE_HEIGHT_PT = 16f
    private const val TITLE_TEXT_SIZE_PT = 18f

    private val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH)

    /** How many lines of [BODY_TEXT_SIZE_PT] body text fit one A4 page's usable height. */
    private val linesPerPage: Int =
        (((PAGE_HEIGHT_PT - MARGIN_PT * 2) / LINE_HEIGHT_PT).toInt()).coerceAtLeast(1)

    fun render(context: Context, incident: IncidentReport): IncidentPdfResult {
        return try {
            val usableWidth = PAGE_WIDTH_PT - MARGIN_PT * 2
            val lines = layoutLines(incident, usableWidth)
            val pages = pagesFor(lines.size, linesPerPage)

            val document = PdfDocument()
            val bodyPaint = Paint().apply {
                textSize = BODY_TEXT_SIZE_PT
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                textSize = TITLE_TEXT_SIZE_PT
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }

            for (pageIndex in 0 until pages) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH_PT.toInt(),
                    PAGE_HEIGHT_PT.toInt(),
                    pageIndex + 1,
                ).create()
                val page = document.startPage(pageInfo)
                drawPage(page.canvas, lines, pageIndex, bodyPaint, titlePaint)
                document.finishPage(page)
            }

            val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
            val outFile = File(reportsDir, "incident-${incident.id}.pdf")
            outFile.outputStream().use { document.writeTo(it) }
            document.close()

            IncidentPdfResult.Written(outFile, pages, outFile.length())
        } catch (e: IOException) {
            IncidentPdfResult.Failed(e.message ?: "Could not write the PDF")
        } catch (e: SecurityException) {
            IncidentPdfResult.Failed(e.message ?: "Not allowed to write the PDF")
        }
    }

    private fun drawPage(
        canvas: Canvas,
        lines: List<String>,
        pageIndex: Int,
        bodyPaint: Paint,
        titlePaint: Paint,
    ) {
        val start = pageIndex * linesPerPage
        val end = (start + linesPerPage).coerceAtMost(lines.size)
        var y = MARGIN_PT + LINE_HEIGHT_PT

        for (i in start until end) {
            val line = lines[i]
            val paint = if (i == 0 && pageIndex == 0) titlePaint else bodyPaint
            canvas.drawText(line, MARGIN_PT, y, paint)
            y += LINE_HEIGHT_PT
        }
    }

    /**
     * Lays the whole report out as a flat list of text lines, top to bottom,
     * as they will be drawn on the page(s). Pure — no [Canvas], no file I/O —
     * so this is directly testable.
     */
    fun layoutLines(report: IncidentReport, width: Float): List<String> {
        val lines = mutableListOf<String>()

        lines += "SafeShade incident report"
        lines += ""
        lines += "Wearer: ${dash(report.wearerName)}"
        lines += "Type: ${dash(report.kind)}"
        lines += "Time: ${formatDate(report.at)}"
        lines += "Outcome: ${dash(report.outcome)}"
        lines += "Emergency contacted: ${if (report.contacted) "Yes" else "No"}"
        lines += "Location: ${formatLatLon(report.lat, report.lon)}"
        lines += ""

        lines += "Medical ID"
        val med = report.medicalId
        if (med == null) {
            lines += "No medical ID on file: —"
        } else {
            lines += "Blood type: ${dash(med.bloodType)}"
            lines.addWrapped("Allergies: ${dash(med.allergies)}", width)
            lines.addWrapped("Conditions: ${dash(med.conditions)}", width)
            lines.addWrapped("Medications: ${dash(med.medications)}", width)
        }
        lines += ""

        lines += "Emergency contacts"
        if (report.contacts.isEmpty()) {
            lines += "—"
        } else {
            for (contact in report.contacts) {
                lines.addWrapped(formatContact(contact), width)
            }
        }
        lines += ""

        lines += "Timeline"
        if (report.timeline.isEmpty()) {
            lines += "—"
        } else {
            for ((at, event) in report.timeline) {
                lines.addWrapped("${formatDate(at)} — $event", width)
            }
        }

        if (!report.evidenceNote.isNullOrBlank()) {
            lines += ""
            lines.addWrapped("Evidence: ${report.evidenceNote}", width)
        }

        lines += ""
        lines += "Generated by SafeShade on ${formatDate(System.currentTimeMillis())}"

        return lines
    }

    /** How many pages [lineCount] lines need at [linesPerPage] lines each. Never fewer than 1. */
    fun pagesFor(lineCount: Int, linesPerPage: Int): Int {
        if (linesPerPage <= 0) return 1
        if (lineCount <= 0) return 1
        return (lineCount + linesPerPage - 1) / linesPerPage
    }

    private fun dash(value: String?): String = if (value.isNullOrBlank()) "—" else value

    private fun formatDate(at: Long): String = if (at <= 0) "—" else dateFormat.format(Date(at))

    private fun formatLatLon(lat: Double?, lon: Double?): String =
        if (lat == null || lon == null) "—" else String.format(Locale.ENGLISH, "%.5f, %.5f", lat, lon)

    private fun formatContact(contact: EmergencyContact): String {
        val name = dash(contact.name)
        val phone = dash(contact.phone)
        val relationship = if (contact.relationship.isNotBlank()) " (${contact.relationship})" else ""
        val primary = if (contact.isPrimary) " [primary]" else ""
        return "$name — $phone$relationship$primary"
    }

    /** Estimated characters that fit in [width] points at [BODY_TEXT_SIZE_PT], for pure word-wrap. */
    private fun charsPerLine(width: Float): Int {
        val approxCharWidthPt = BODY_TEXT_SIZE_PT * 0.5f
        return (width / approxCharWidthPt).toInt().coerceAtLeast(20)
    }

    private fun MutableList<String>.addWrapped(text: String, width: Float) {
        val maxChars = charsPerLine(width)
        if (text.length <= maxChars) {
            this += text
            return
        }

        val words = text.split(" ")
        var current = StringBuilder()
        for (word in words) {
            val candidateLength = if (current.isEmpty()) word.length else current.length + 1 + word.length
            if (candidateLength > maxChars && current.isNotEmpty()) {
                this += current.toString()
                current = StringBuilder(word)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            }
        }
        if (current.isNotEmpty()) this += current.toString()
    }
}

sealed interface IncidentPdfResult {
    data class Written(val file: File, val pages: Int, val bytes: Long) : IncidentPdfResult
    data class Failed(val reason: String) : IncidentPdfResult
}

data class IncidentReport(
    val id: String,
    val wearerName: String,
    val kind: String,
    val at: Long,
    val outcome: String,
    val contacted: Boolean,
    val lat: Double?,
    val lon: Double?,
    val medicalId: MedicalId?,
    val contacts: List<EmergencyContact>,
    val timeline: List<Pair<Long, String>>,
    val evidenceNote: String?,
)
