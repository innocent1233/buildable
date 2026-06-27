package com.libraryx.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.Payment
import com.libraryx.data.model.SaasPayment
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudyLabStats
import com.libraryx.data.model.SubAdminDoc
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Replaces src/lib/pdf.ts and src/lib/saas/saasPdf.ts (jsPDF + jspdf-autotable). Uses
 * Android's built-in `android.graphics.pdf.PdfDocument` + `Canvas` to draw the same
 * receipt/report layouts, then hands the bytes to [FileSaver] — the native equivalent of
 * the original's `savePdf()` call at the end of each generator function.
 *
 * Page sizes are in PostScript points (1/72 inch), matching jsPDF's `{ unit: "mm" }` pages
 * converted to points: A5 \u2248 420x595pt, A4 = 595x842pt.
 */
object PdfReceiptGenerator {

    private const val A5_WIDTH = 420f
    private const val A5_HEIGHT = 595f
    private const val A4_WIDTH = 595f
    private const val A4_HEIGHT = 842f

    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val monthYearFmt = DateTimeFormatter.ofPattern("MMMM yyyy")

    /** Mirrors `generateReceipt` from src/lib/pdf.ts. */
    fun generateReceipt(context: Context, student: Student, payment: Payment?, settings: AppSettings): Uri? {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(A5_WIDTH.toInt(), A5_HEIGHT.toInt(), 1).create())
        val canvas = page.canvas
        val w = A5_WIDTH
        var y = 60f

        val title = Paint().apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val bold = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD }
        val normal = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT }
        val italic = Paint().apply { textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); textAlign = Paint.Align.CENTER }
        val line = Paint().apply { color = 0xFF2980B9.toInt(); strokeWidth = 1.5f }

        canvas.drawText("${settings.labName.uppercase()} RECEIPT", w / 2, y, title)
        y += 30f
        canvas.drawLine(40f, y, w - 40f, y, line)
        y += 26f

        val today = LocalDate.now()
        val rows = listOf(
            "Student Name" to student.name,
            "Phone" to student.phone,
            "Receipt Date" to today.format(dateFmt),
            "Membership Fee" to "Rs. ${student.monthlyFee.toInt()}",
            "Payment Mode" to (payment?.mode?.name ?: "N/A"),
            "Amount Paid" to (payment?.let { "Rs. ${it.amount.toInt()}" } ?: "N/A"),
            "Month Covered" to today.format(monthYearFmt),
            "Status" to (if (payment != null) "PAID" else "UNPAID")
        )
        rows.forEach { (label, value) ->
            canvas.drawText("$label:", 50f, y, bold)
            canvas.drawText(value, 195f, y, normal)
            y += 22f
        }

        y += 14f
        canvas.drawLine(40f, y, w - 40f, y, line)
        y += 26f
        val footer = if (payment != null) "Thank you for your payment!" else "Pending Payment Reminder - Please pay at the earliest."
        canvas.drawText(footer, w / 2, y, italic)

        doc.finishPage(page)
        val safeName = student.name.replace(Regex("\\s+"), "_")
        val month = today.format(DateTimeFormatter.ofPattern("MM_yyyy"))
        return savePdfDocument(context, doc, "${safeName}_Receipt_${month}.pdf")
    }

    /** Mirrors `generateMonthlyReport` from src/lib/pdf.ts. */
    fun generateMonthlyReport(context: Context, settings: AppSettings, stats: StudyLabStats): Uri? {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH.toInt(), A4_HEIGHT.toInt(), 1).create())
        val canvas = page.canvas
        val w = A4_WIDTH
        var y = 60f

        val title = Paint().apply { textSize = 24f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val subtitle = Paint().apply { textSize = 15f; textAlign = Paint.Align.CENTER }
        val bold = Paint().apply { textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
        val normal = Paint().apply { textSize = 14f; typeface = Typeface.DEFAULT }
        val small = Paint().apply { textSize = 11f; textAlign = Paint.Align.CENTER }
        val line = Paint().apply { color = 0xFF2980B9.toInt(); strokeWidth = 1.5f }

        val today = LocalDate.now()
        canvas.drawText("${settings.labName} - Monthly Report", w / 2, y, title)
        y += 26f
        canvas.drawText(today.format(monthYearFmt), w / 2, y, subtitle)
        y += 40f
        canvas.drawLine(40f, y, w - 40f, y, line)
        y += 26f

        val defaultRate = if (stats.totalStudents > 0) "%.1f".format(stats.overdueCount * 100.0 / stats.totalStudents) else "0"
        val rows = listOf(
            "Total Students" to "${stats.totalStudents}",
            "Students Paid" to "${stats.paidCount}",
            "Students Unpaid" to "${stats.unpaidCount}",
            "Overdue Students" to "${stats.overdueCount}",
            "Total Revenue" to "Rs. ${stats.totalRevenue.toInt()}",
            "Pending Revenue" to "Rs. ${stats.pendingRevenue.toInt()}",
            "Default Rate" to "$defaultRate%"
        )
        rows.forEach { (label, value) ->
            canvas.drawText("$label:", 70f, y, bold)
            canvas.drawText(value, 300f, y, normal)
            y += 28f
        }

        y += 24f
        canvas.drawLine(40f, y, w - 40f, y, line)
        y += 22f
        val generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        canvas.drawText("Generated on $generated", w / 2, y, small)

        doc.finishPage(page)
        val month = today.format(DateTimeFormatter.ofPattern("MM_yyyy"))
        return savePdfDocument(context, doc, "StudyLab_Report_$month.pdf")
    }

    /** Mirrors `generateSaasReceipt` from src/lib/saas/saasPdf.ts. */
    fun generateSaasReceipt(context: Context, student: SaasStudent, payment: SaasPayment?, lab: SubAdminDoc): Uri? {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(A5_WIDTH.toInt(), A5_HEIGHT.toInt(), 1).create())
        val canvas = page.canvas
        val w = A5_WIDTH
        var y = 50f

        val title = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val subtitle = Paint().apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        val bold = Paint().apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val normal = Paint().apply { textSize = 11f; typeface = Typeface.DEFAULT }
        val italic = Paint().apply { textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); textAlign = Paint.Align.CENTER }
        val line = Paint().apply { color = 0xFF3C3C3C.toInt(); strokeWidth = 1f }

        canvas.drawText(lab.labName.uppercase(), w / 2, y, title)
        y += 18f
        canvas.drawText("Membership Payment Receipt", w / 2, y, subtitle)
        y += 18f
        canvas.drawLine(40f, y, w - 40f, y, line)
        y += 22f

        val paidDate = payment?.paidDate?.let { dateOf(it) } ?: LocalDate.now()
        val today = LocalDate.now()
        val rows = listOf(
            "Receipt ID" to (payment?.receiptId?.ifBlank { null } ?: "REC_${System.currentTimeMillis()}"),
            "Date" to paidDate.format(dateFmt),
            "Student" to student.name,
            "Phone" to student.phone,
            "Seat" to (student.seatNumber?.toString() ?: "-"),
            "Month" to (payment?.month ?: today.format(DateTimeFormatter.ofPattern("yyyy-MM"))),
            "Original Fee" to "Rs. ${(payment?.originalFee ?: student.monthlyFee).toInt()}",
            "Late Fee" to "Rs. ${(payment?.lateFee ?: 0.0).toInt()}",
            "Total Paid" to "Rs. ${(payment?.amount ?: student.monthlyFee).toInt()}",
            "Mode" to (payment?.mode?.name ?: "\u2014"),
            "Status" to (if (payment != null) "PAID" else "UNPAID")
        )
        rows.forEach { (label, value) ->
            canvas.drawText("$label:", 44f, y, bold)
            canvas.drawText(value, 165f, y, normal)
            y += 18f
        }

        y += 12f
        canvas.drawLine(40f, y, w - 40f, y, line)
        y += 18f
        canvas.drawText(if (payment != null) "Thank you for your payment!" else "Pending \u2014 please pay soon.", w / 2, y, italic)
        y += 16f
        canvas.drawText("Owner: ${lab.ownerName}  \u2022  ${lab.phone}", w / 2, y, italic)

        doc.finishPage(page)
        val safeName = student.name.replace(Regex("\\s+"), "_")
        val month = (payment?.month ?: today.format(DateTimeFormatter.ofPattern("yyyy-MM"))).replace("-", "_")
        return savePdfDocument(context, doc, "${safeName}_Receipt_$month.pdf")
    }

    private fun dateOf(date: Date): LocalDate =
        date.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate()

    private fun savePdfDocument(context: Context, doc: PdfDocument, filename: String): Uri? {
        val bytes = ByteArrayOutputStream().use { out ->
            doc.writeTo(out)
            doc.close()
            out.toByteArray()
        }
        return FileSaver.saveToDownloads(context, filename, "application/pdf", bytes)
    }
}
