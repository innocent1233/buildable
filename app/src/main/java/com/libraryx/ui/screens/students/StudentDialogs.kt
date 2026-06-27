package com.libraryx.ui.screens.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import com.libraryx.ui.components.AppDropdown
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.libraryx.data.model.Payment
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.ui.theme.StudyLabColors
import java.time.LocalDate

@Composable
fun StudentFormDialog(
    title: String,
    totalSeats: Int,
    initial: Student?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, email: String, fee: Double, joining: String, status: StudentStatus, seat: Int?, pin: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var fee by remember { mutableStateOf(initial?.monthlyFee?.toInt()?.toString() ?: "") }
    var joining by remember { mutableStateOf(initial?.joiningDate ?: LocalDate.now().toString()) }
    var status by remember { mutableStateOf(initial?.status ?: StudentStatus.Active) }
    var seat by remember { mutableStateOf(initial?.seat?.toString() ?: "") }
    var pin by remember { mutableStateOf(initial?.pin ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = StudyLabColors.NeonGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = fee, onValueChange = { fee = it.filter { c -> c.isDigit() } },
                    label = { Text("Monthly Fee (\u20B9)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(value = joining, onValueChange = { joining = it }, label = { Text("Joining Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = seat, onValueChange = { seat = it.filter { c -> c.isDigit() } },
                    label = { Text("Seat (1-$totalSeats, optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("Student Portal PIN (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = status == StudentStatus.Active, onClick = { status = StudentStatus.Active })
                    Text("Active", modifier = Modifier.padding(end = 12.dp))
                    RadioButton(selected = status == StudentStatus.Inactive, onClick = { status = StudentStatus.Inactive })
                    Text("Inactive")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(
                    name.trim(), phone.trim(), email.trim(),
                    fee.toDoubleOrNull() ?: 0.0, joining, status,
                    seat.toIntOrNull(), pin.ifBlank { null }
                )
            }) { Text(if (initial == null) "Add Student" else "Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PaymentDialog(
    student: Student,
    lateFee: Double,
    onDismiss: () -> Unit,
    onConfirm: (date: String, amount: Double, mode: PaymentMode, notes: String) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var amount by remember { mutableStateOf((student.monthlyFee + lateFee).toInt().toString()) }
    var mode by remember { mutableStateOf(PaymentMode.Cash) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment \u2014 ${student.name}", color = StudyLabColors.NeonGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lateFee > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = StudyLabColors.NeonPink.copy(alpha = 0.12f))) {
                        Text(
                            "LATE FEE APPLIED\nOriginal: \u20B9${student.monthlyFee.toInt()} + Late: \u20B9${lateFee.toInt()} = \u20B9${(student.monthlyFee + lateFee).toInt()}",
                            modifier = Modifier.padding(10.dp), color = StudyLabColors.NeonPink
                        )
                    }
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Payment Date") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (\u20B9)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                AppDropdown(
                    label = "Payment Mode",
                    selected = mode.name,
                    options = PaymentMode.entries.map { it.name },
                    onSelect = { mode = PaymentMode.valueOf(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(date, amount.toDoubleOrNull() ?: 0.0, mode, notes) }) { Text("Record Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BulkPaymentDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onConfirm: (date: String, mode: PaymentMode, notes: String) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var mode by remember { mutableStateOf(PaymentMode.Cash) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Payment \u2014 ${students.size} students", color = StudyLabColors.NeonGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.height(140.dp)) {
                    items(students, key = { it.id }) { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.name)
                            Text("\u20B9${s.monthlyFee.toInt()}", color = StudyLabColors.NeonCyan)
                        }
                    }
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                AppDropdown(
                    label = "Payment Mode",
                    selected = mode.name,
                    options = PaymentMode.entries.map { it.name },
                    onSelect = { mode = PaymentMode.valueOf(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(date, mode, notes) }) { Text("Mark All as Paid") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PaymentHistoryDialog(student: Student, onDismiss: () -> Unit) {
    val sorted = student.payments.sortedByDescending { it.date }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment History \u2014 ${student.name}", color = StudyLabColors.NeonCyan) },
        text = {
            if (sorted.isEmpty()) {
                Text("No payments recorded", color = StudyLabColors.TextMuted)
            } else {
                LazyColumn(Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sorted) { p: Payment ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(p.date, style = MaterialTheme.typography.bodyMedium)
                                    val detail = buildString {
                                        append(p.mode.name)
                                        if (p.notes.isNotBlank()) append(" \u2022 ${p.notes}")
                                        if (p.lateFee > 0) append(" \u2022 Late fee: \u20B9${p.lateFee.toInt()}")
                                    }
                                    Text(detail, style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
                                }
                                Text("\u20B9${p.amount.toInt()}", color = StudyLabColors.NeonGreen)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// PaymentModeSelector removed — replaced by shared AppDropdown component
