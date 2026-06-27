package com.libraryx.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.ui.components.StatCard
import com.libraryx.ui.theme.StudyLabColors
import java.text.NumberFormat
import java.util.Locale

private data class DashCard(val title: String, val value: String, val accent: androidx.compose.ui.graphics.Color)

/**
 * Compose port of src/pages/Dashboard.tsx. The "Print" button's `printOrPdf` call (which on
 * native mobile just toasted "Open Reports to download a PDF") is replaced by a direct
 * navigation hint, since this app has no browser print dialog to fall back to.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAddStudent: () -> Unit,
    onViewStudents: () -> Unit,
    onViewReports: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val inr = NumberFormat.getNumberInstance(Locale("en", "IN"))

    val cards = buildList {
        add(DashCard("Total Active Students", "${state.stats.totalStudents}", StudyLabColors.NeonCyan))
        add(DashCard("Paid This Month", "${state.stats.paidCount}", StudyLabColors.NeonGreen))
        add(DashCard("Unpaid This Month", "${state.stats.unpaidCount}", StudyLabColors.NeonOrange))
        add(DashCard("Overdue (${state.settings.overdueDays}+ days)", "${state.stats.overdueCount}", StudyLabColors.NeonPink))
        add(DashCard("Revenue Collected", "\u20B9${inr.format(state.stats.totalRevenue.toInt())}", StudyLabColors.NeonGreen))
        add(DashCard("Pending Revenue", "\u20B9${inr.format(state.stats.pendingRevenue.toInt())}", StudyLabColors.NeonPink))
        add(DashCard("Seats Occupied", "${state.stats.occupiedSeats}/${state.settings.totalSeats}", StudyLabColors.NeonCyan))
        val occupancy = if (state.settings.totalSeats > 0) (state.stats.occupiedSeats * 100.0 / state.settings.totalSeats) else 0.0
        add(DashCard("Occupancy Rate", "${occupancy.toInt()}%", StudyLabColors.NeonOrange))
        if (state.settings.lateFee.enabled) {
            add(DashCard("Pending Late Fees", "\u20B9${inr.format(state.stats.totalLateFees.toInt())}", StudyLabColors.NeonPink))
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)
            Text(
                "${state.settings.labName} \u00B7 ${state.settings.totalSeats} seats capacity",
                style = MaterialTheme.typography.bodyMedium,
                color = StudyLabColors.TextMuted
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            items(cards) { c ->
                StatCard(title = c.title, value = c.value, accent = c.accent, modifier = Modifier.fillMaxWidth())
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddStudent) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Add Student")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onViewStudents) {
                Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("View Students")
            }
            OutlinedButton(onClick = onViewReports) {
                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Reports")
            }
        }
    }
}
