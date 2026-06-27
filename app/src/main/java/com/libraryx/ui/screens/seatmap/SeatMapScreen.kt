package com.libraryx.ui.screens.seatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.ui.components.StatusBadge
import com.libraryx.ui.theme.StudyLabColors
import java.time.LocalDate

/** Compose port of src/pages/SeatMap.tsx — a grid of seat tiles colored by occupant payment status. */
@Composable
fun SeatMapScreen(viewModel: SeatMapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val seatMap = viewModel.seatMap()
    var selectedSeat by remember { mutableStateOf<Int?>(null) }
    val selectedStudent = selectedSeat?.let { seatMap[it] }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Seat Map", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)
        Text(
            "${seatMap.size}/${state.settings.totalSeats} seats occupied",
            color = StudyLabColors.TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items((1..state.settings.totalSeats).toList()) { seat ->
                val student = seatMap[seat]
                val color = when {
                    student == null -> StudyLabColors.Border
                    viewModel.statusFor(student) == PaymentStatus.Paid -> StudyLabColors.NeonGreen
                    viewModel.statusFor(student) == PaymentStatus.Overdue -> StudyLabColors.NeonPink
                    else -> StudyLabColors.NeonOrange
                }
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
                    onClick = { selectedSeat = seat }
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$seat", style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted, modifier = Modifier.padding(top = 6.dp))
                        if (student != null) {
                            Text(
                                student.name.take(8),
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSeat?.let { seat ->
        SeatDetailDialog(
            seat = seat,
            student = selectedStudent,
            statusOf = viewModel::statusFor,
            lateFeeOf = viewModel::getLateFee,
            onRecordPayment = { s, date, amount, mode -> viewModel.recordPayment(s, date, amount, mode) { selectedSeat = null } },
            onDismiss = { selectedSeat = null }
        )
    }
}

@Composable
private fun SeatDetailDialog(
    seat: Int,
    student: Student?,
    statusOf: (Student) -> PaymentStatus,
    lateFeeOf: (Student) -> Double,
    onRecordPayment: (Student, String, Double, PaymentMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seat $seat", color = StudyLabColors.NeonCyan) },
        text = {
            if (student == null) {
                Text("Empty \u2014 assign a student to this seat from the Students page.", color = StudyLabColors.TextMuted)
            } else {
                val status = statusOf(student)
                val lateFee = lateFeeOf(student)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(student.name, style = MaterialTheme.typography.titleMedium)
                    Text(student.phone, color = StudyLabColors.TextMuted)
                    Row { Text("Fee: "); Text("\u20B9${student.monthlyFee.toInt()}", color = StudyLabColors.NeonCyan) }
                    if (lateFee > 0) Text("Late fee: \u20B9${lateFee.toInt()}", color = StudyLabColors.NeonPink)
                    StatusBadge(status)
                    if (status != PaymentStatus.Paid) {
                        var amount by remember { mutableStateOf((student.monthlyFee + lateFee).toInt().toString()) }
                        OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Amount") })
                        Button(onClick = {
                            onRecordPayment(student, LocalDate.now().toString(), amount.toDoubleOrNull() ?: 0.0, PaymentMode.Cash)
                        }) { Text("Mark Paid") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
