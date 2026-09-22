package com.saferescue.app.core.report

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfIncidentReportGenerator(private val context: Context) {
    fun generate(report: IncidentReport): File {
        val safeId = report.incidentId.filter { it.isLetterOrDigit() || it == '-' }.take(48).ifBlank { "incident" }
        val file = File(context.filesDir, "reports/incident-$safeId.pdf").apply { parentFile?.mkdirs() }
        val document = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 20f }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 13f }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 48f
        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            y = 48f
        }
        fun line(text: String, paint: Paint = body, gap: Float = 15f) {
            if (y > 790f) newPage()
            canvas.drawText(text.take(105), 42f, y, paint)
            y += gap
        }
        fun wrapped(text: String, paint: Paint = body) {
            var remaining = text
            while (remaining.isNotEmpty()) {
                if (y > 790f) newPage()
                var end = minOf(105, remaining.length)
                if (end < remaining.length) {
                    val space = remaining.lastIndexOf(' ', end)
                    if (space > 20) end = space
                }
                line(remaining.substring(0, end).trim(), paint)
                remaining = remaining.substring(end).trimStart()
            }
        }

        line("SafeRescue Incident Report", title, 28f)
        line("Generated: ${format(report.generatedAtMillis)}", small)
        line("Incident ID: ${report.incidentId}", small)
        y += 8f
        line("Incident summary", heading, 19f)
        line("Final risk: ${report.finalRiskScore}/100 • ${report.finalRiskSeverity}")
        line("Timeline events: ${report.timeline.size}")
        line("Encrypted evidence items: ${report.evidenceCount}")
        line("Location telemetry available: ${if (report.locationAvailable) "Yes" else "No"}")
        line("Victim voice monitoring used: ${if (report.voiceMonitoringUsed) "Yes" else "No"}")
        y += 8f
        line("Chronological timeline", heading, 19f)
        if (report.timeline.isEmpty()) line("No timeline events were recorded.")
        report.timeline.sortedBy { it.timestampMillis }.forEach { event ->
            line("${format(event.timestampMillis)}  ${event.title}", heading, 14f)
            wrapped(event.detail)
            event.riskScore?.let { line("Risk score: $it/100", small, 13f) }
            y += 3f
        }
        y += 8f
        line("Security & privacy notes", heading, 19f)
        wrapped("This report is generated locally from SafeRescue incident metadata. It does not claim that police, emergency services, or trusted contacts received a notification unless a backend delivery confirmation exists.")
        wrapped("Sensitive evidence remains protected by the application's encrypted evidence lifecycle. This PDF contains metadata and timeline information, not raw camera or voice recordings.")
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun format(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
}
