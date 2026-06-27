package com.libraryx.ui.screens.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.libraryx.ui.components.AppDropdown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.ui.components.StatusBadge
import com.libraryx.ui.theme.StudyLabColors
import java.time.LocalDate

/**
 * Compose port of src/pages/Students.tsx. Radix's `<Table>` becomes a `LazyColumn` of
 * per-student cards — a wide multi-column table doesn't fit a phone screen, so each row's
 * fields (phone, seat, fee, status, last payment, overdue) are stacked inside a card while
 * preserving every data point and action button from the original row.
 */
@Composable
fun StudentsScreen(viewModel: StudentsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val repo by viewModel.repository.collectAsState()
    val filtered = state.filtered(repo)

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Student?>(null) }
    var payTarget by remember { mutableStateOf<Student?>(null) }
    var historyTarget by remember { mutableStateOf<Student?>(null) }
    var showBulkPay by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Student?>(null) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Students", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Add Student")
            }
        }

        if (state.selectedIds.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${state.selectedIds.size} selected", color = StudyLabColors.NeonCyan, modifier = Modifier.padding(end = 8.dp))
                    TextButton(onClick = { showBulkPay = true }) { Text("Mark Paid") }
                    TextButton(onClick = { viewModel.bulkDelete(state.selectedIds) }) { Text("Delete", color = StudyLabColors.NeonPink) }
                }
            }
        }

        OutlinedTextField(
            value = state.search,
            onValueChange = viewModel::setSearch,
            label = { Text("Search by name or phone") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppDropdown(
                label = "Filter",
                selected = state.filter.name,
                options = FilterType.entries.map { it.name },
                onSelect = { viewModel.setFilter(FilterType.valueOf(it)) },
                modifier = Modifier.weight(1f)
            )
            AppDropdown(
                label = "Sort",
                selected = state.sort.name,
                options = SortType.entries.map { it.name },
                onSelect = { viewModel.setSort(SortType.valueOf(it)) },
                modifier = Modifier.weight(1f)
            )
        }

        if (filtered.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("No students found", style = MaterialTheme.typography.titleMedium)
                    Text("Add your first student to get started", color = StudyLabColors.TextMuted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { s ->
                    StudentRowCard(
                        student = s,
                        selected = s.id in state.selectedIds,
                        status = viewModel.getStatus(s),
                        lateFee = viewModel.getLateFee(s),
                        lastPay = viewModel.getLastPay(s),
                        overdueDays = viewModel.getOverdue(s),
                        onToggleSelect = { viewModel.toggleSelect(s.id) },
                        onMarkPaid = { payTarget = s },
                        onHistory = { historyTarget = s },
                        onEdit = { editTarget = s },
                        onDelete = { deleteTarget = s }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        StudentFormDialog(
            title = "Add New Student",
            totalSeats = state.settings.totalSeats,
            initial = null,
            onDismiss = { showAddDialog = false },
            onSubmit = { name, phone, email, fee, joining, status, seat, pin ->
                viewModel.addStudent(name, phone, email, fee, joining, status, seat, pin) { ok, _ ->
                    if (ok) showAddDialog = false
                }
            }
        )
    }

    editTarget?.let { s ->
        StudentFormDialog(
            title = "Edit Student",
            totalSeats = state.settings.totalSeats,
            initial = s,
            onDismiss = { editTarget = null },
            onSubmit = { name, phone, email, fee, joining, status, seat, pin ->
                viewModel.editStudent(s.id, name, phone, email, fee, joining, status, seat, pin) { ok, _ ->
                    if (ok) editTarget = null
                }
            }
        )
    }

    payTarget?.let { s ->
        PaymentDialog(
            student = s,
            lateFee = viewModel.getLateFee(s),
            onDismiss = { payTarget = null },
            onConfirm = { date, amount, mode, notes ->
                viewModel.recordPayment(s, date, amount, mode, notes) { payTarget = null }
            }
        )
    }

    if (showBulkPay) {
        val selectedStudents = state.students.filter { it.id in state.selectedIds }
        BulkPaymentDialog(
            students = selectedStudents,
            onDismiss = { showBulkPay = false },
            onConfirm = { date, mode, notes ->
                viewModel.bulkRecordPayment(selectedStudents, date, mode, notes) { showBulkPay = false }
            }
        )
    }

    historyTarget?.let { s ->
        PaymentHistoryDialog(student = s, onDismiss = { historyTarget = null })
    }

    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${s.name}?") },
            text = { Text("This will permanently remove the student and their payment history.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteStudent(s.id); deleteTarget = null }) {
                    Text("Delete", color = StudyLabColors.NeonPink)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StudentRowCard(
    student: Student,
    selected: Boolean,
    status: PaymentStatus,
    lateFee: Double,
    lastPay: String?,
    overdueDays: Int,
    onToggleSelect: () -> Unit,
    onMarkPaid: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when (status) {
        PaymentStatus.Overdue -> StudyLabColors.NeonPink
        PaymentStatus.Unpaid -> StudyLabColors.NeonOrange
        PaymentStatus.Paid -> StudyLabColors.NeonGreen
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(student.name, style = MaterialTheme.typography.titleMedium)
                        if (student.status == StudentStatus.Inactive) {
                            Text("  INACTIVE", style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
                        }
                    }
                    Text(student.phone, style = MaterialTheme.typography.bodyMedium, color = StudyLabColors.TextMuted)
                }
                StatusBadge(status)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Seat: ${student.seat ?: "\u2014"}", color = StudyLabColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("\u20B9${student.monthlyFee.toInt()}", color = StudyLabColors.NeonCyan)
                    if (lateFee > 0) Text("+\u20B9${lateFee.toInt()} late", color = StudyLabColors.NeonPink, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last paid: ${lastPay ?: "\u2014"}", color = StudyLabColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                if (status != PaymentStatus.Paid) {
                    Text(
                        "$overdueDays days overdue",
                        color = if (status == PaymentStatus.Overdue) StudyLabColors.NeonPink else StudyLabColors.NeonOrange,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                if (status != PaymentStatus.Paid) {
                    IconButton(onClick = onMarkPaid) { Icon(Icons.Filled.CheckCircle, contentDescription = "Mark Paid", tint = StudyLabColors.NeonGreen) }
                }
                IconButton(onClick = onHistory) { Icon(Icons.Filled.History, contentDescription = "History", tint = StudyLabColors.NeonCyan) }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = StudyLabColors.NeonCyan) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = StudyLabColors.NeonPink) }
            }
        }
    }
}

// FilterDropdown removed — replaced by shared AppDropdown component
