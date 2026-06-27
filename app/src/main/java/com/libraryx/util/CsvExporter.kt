package com.libraryx.util

import android.content.Context
import android.net.Uri
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Mirrors `generateCSVReport` from src/lib/pdf.ts. Builds the same
 * Name/Phone/Email/Monthly Fee/Status/Last Payment/Days Overdue CSV and saves it via
 * [FileSaver] instead of the browser's anchor-download trick.
 */
object CsvExporter {

    fun export(
        context: Context,
        students: List<Student>,
        statusOf: (Student) -> PaymentStatus,
        overdueOf: (Student) -> Int,
        lastPayOf: (Student) -> String?
    ): Uri? {
        val headers = listOf("Name", "Phone", "Email", "Monthly Fee", "Status", "Last Payment", "Days Overdue")
        val rows = students.map { s ->
            listOf(
                s.name,
                s.phone,
                s.email,
                s.monthlyFee.toInt().toString(),
                statusOf(s).name,
                lastPayOf(s) ?: "N/A",
                overdueOf(s).toString()
            )
        }
        val csv = buildString {
            appendLine(headers.joinToString(",") { "\"$it\"" })
            rows.forEach { row -> appendLine(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }) }
        }
        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("MM_yyyy"))
        return FileSaver.saveToDownloads(context, "StudyLab_Report_$month.csv", "text/csv", csv.toByteArray())
    }
}
