package com.libraryx.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.libraryx.ui.components.AppDropdown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.ui.components.StatCard
import com.libraryx.ui.theme.StudyLabColors
import com.libraryx.util.CsvExporter
import com.libraryx.util.FileSaver
import com.libraryx.util.PdfReceiptGenerator
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Compose port of src/pages/Reports.tsx — summary cards, date-range filter, payment list, exports. */
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val inr = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    val defaultRate = if (state.stats.totalStudents > 0) "%.1f".format(state.stats.overdueCount * 100.0 / state.stats.totalStudents) else "0"

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reports", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val uri = PdfReceiptGenerator.generateMonthlyReport(context, state.settings, state.stats)
                Toast.makeText(context, if (uri != null) "PDF report downloaded" else "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }) { Text("Download PDF") }
            OutlinedButton(onClick = {
                val uri = CsvExporter.export(context, state.students, viewModel::getStatus, viewModel::getOverdue, viewModel::getLastPay)
                Toast.makeText(context, if (uri != null) "CSV report downloaded" else "Failed to save CSV", Toast.LENGTH_SHORT).show()
            }) { Text("Download CSV") }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PERIOD", style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
                AppDropdown(
                    label = "Period",
                    selected = state.preset.name,
                    options = DatePreset.entries.map { it.name },
                    onSelect = { viewModel.setPreset(DatePreset.valueOf(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.preset == DatePreset.Custom) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.customStart.toString(), onValueChange = {
                                runCatching { java.time.LocalDate.parse(it) }.getOrNull()?.let { d -> viewModel.setCustomRange(d, state.customEnd) }
                            },
                            label = { Text("From") }, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.customEnd.toString(), onValueChange = {
                                runCatching { java.time.LocalDate.parse(it) }.getOrNull()?.let { d -> viewModel.setCustomRange(state.customStart, d) }
                            },
                            label = { Text("To") }, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        val summary = listOf(
            "Total Students" to "${state.stats.totalStudents}",
            "Paid" to "${state.stats.paidCount}",
            "Unpaid" to "${state.stats.unpaidCount}",
            "Overdue" to "${state.stats.overdueCount}",
            "Revenue Collected" to "\u20B9${inr.format(state.stats.totalRevenue.toInt())}",
            "Pending Revenue" to "\u20B9${inr.format(state.stats.pendingRevenue.toInt())}",
            "Default Rate" to "$defaultRate%"
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            items(summary) { (label, value) ->
                StatCard(title = label, value = value, accent = StudyLabColors.NeonCyan, modifier = Modifier.fillMaxWidth())
            }
        }

        Text(
            "Period Revenue: \u20B9${inr.format(viewModel.filteredRevenue().toInt())}  \u00B7  Late Fees: \u20B9${inr.format(viewModel.filteredLateFees().toInt())}",
            color = StudyLabColors.NeonGreen,
            style = MaterialTheme.typography.bodyMedium
        )

        Text("Monthly Breakdown", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonCyan)
        viewModel.monthlyBreakdown().forEach { mb ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mb.label, style = MaterialTheme.typography.bodyMedium)
                    Text("${mb.paid}/${mb.total} paid \u00B7 \u20B9${inr.format(mb.revenue.toInt())}", color = StudyLabColors.NeonGreen, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Text("Payments in Period", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonCyan)
        val payments = viewModel.filteredPayments()
        if (payments.isEmpty()) {
            Text("No payments in this period", color = StudyLabColors.TextMuted)
        } else {
            payments.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(p.student, style = MaterialTheme.typography.bodyMedium)
                        Text("${p.date.format(dateFmt)} \u00B7 ${p.mode.name}", color = StudyLabColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("\u20B9${p.amount.toInt()}", color = StudyLabColors.NeonGreen)
                }
            }
        }
    }
}
