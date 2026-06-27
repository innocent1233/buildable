package com.libraryx.ui.screens.monthlydues

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.ui.components.AppDropdown
import com.libraryx.ui.components.MonthStatusBadge
import com.libraryx.ui.theme.StudyLabColors

/**
 * Compose port of src/pages/MonthlyDues.tsx — a per-student row showing a Paid/Unpaid/
 * Overdue/N-A badge for every month of the selected year, scrollable horizontally exactly
 * like the original's wide table.
 *
 * Year picker uses [AppDropdown] — no ExposedDropdownMenuBox inline here.
 */
@Composable
fun MonthlyDuesScreen(viewModel: MonthlyDuesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val months = viewModel.months()
    val students = viewModel.activeStudents()

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Monthly Dues",
                style = MaterialTheme.typography.headlineMedium,
                color = StudyLabColors.NeonGreen
            )
            AppDropdown(
                label = "Year",
                selected = state.selectedYear.toString(),
                options = viewModel.years().map { it.toString() },
                onSelect = { viewModel.setYear(it.toInt()) },
                modifier = Modifier.width(140.dp)
            )
        }

        if (students.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "No active students",
                    modifier = Modifier.padding(24.dp),
                    color = StudyLabColors.TextMuted
                )
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // Header row
                    Row {
                        Text(
                            "Student",
                            Modifier.width(140.dp).padding(6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StudyLabColors.TextMuted
                        )
                        months.forEach { (_, label) ->
                            Text(
                                label,
                                Modifier.width(56.dp).padding(6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudyLabColors.TextMuted
                            )
                        }
                    }
                    // One row per active student
                    students.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                s.name,
                                Modifier.width(140.dp).padding(6.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            months.forEach { (m, _) ->
                                Row(Modifier.width(56.dp).padding(6.dp)) {
                                    MonthStatusBadge(viewModel.statusFor(s, m))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
