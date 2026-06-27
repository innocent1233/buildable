package com.libraryx.ui.screens.studentportal

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.data.model.SaasPayment
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.SubAdminDoc
import com.libraryx.ui.theme.StudyLabColors
import com.libraryx.util.PdfReceiptGenerator
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Compose port of src/pages/saas/SaasStudentPortal.tsx — phone + PIN login,
 * then a membership card showing seat, fee, payment status, and payment history
 * with per-payment receipt generation.
 */
@Composable
fun StudentPortalScreen(
    viewModel: StudentPortalViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    if (state.student == null || state.lab == null) {
        StudentPortalLoginForm(
            busy = state.busy,
            error = state.error,
            onLogin = { phone, pin -> viewModel.login(phone, pin) },
            onNavigateBack = onNavigateBack
        )
    } else {
        StudentPortalDashboard(
            student = state.student!!,
            lab = state.lab!!,
            onLogout = { viewModel.logout() },
            onGenerateReceipt = { payment ->
                val uri = PdfReceiptGenerator.generateSaasReceipt(context, state.student!!, payment, state.lab!!)
                if (uri == null) Toast.makeText(context, "Failed to save receipt", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun StudentPortalLoginForm(
    busy: Boolean,
    error: String?,
    onLogin: (phone: String, pin: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onNavigateBack) {
                Text("\u2190 Back to login", color = StudyLabColors.NeonCyan)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = StudyLabColors.NeonCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "STUDENT PORTAL",
                        style = MaterialTheme.typography.titleLarge,
                        color = StudyLabColors.NeonCyan,
                        textAlign = TextAlign.Center
                    )

                    error?.let { msg ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = StudyLabColors.NeonPink)
                            Text(msg, color = StudyLabColors.NeonPink, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { v -> phone = v.filter { it.isDigit() }.take(10) },
                        label = { Text("Phone (10 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
                        label = { Text("4-digit PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    if (busy) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = { onLogin(phone, pin) },
                            enabled = phone.length == 10 && pin.length == 4,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Login") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentPortalDashboard(
    student: SaasStudent,
    lab: SubAdminDoc,
    onLogout: () -> Unit,
    onGenerateReceipt: (SaasPayment) -> Unit
) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val sorted = student.payments.sortedByDescending { it.paidDate }

    LazyColumn(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Hi, ${student.name.split(" ").first()}!", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)
                    Text(lab.labName, color = StudyLabColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onLogout) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Logout")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Membership", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonCyan)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MembershipField("Seat", student.seatNumber?.toString() ?: "\u2014")
                        MembershipField("Fee", "\u20B9${student.monthlyFee.toInt()}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MembershipField("Phone", student.phone)
                        Column {
                            Text("Status", style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
                            val (statusText, statusColor) = when {
                                student.isLocked -> "\uD83D\uDD12 Locked" to StudyLabColors.NeonPink
                                student.currentMonthPaid -> "Paid" to StudyLabColors.NeonGreen
                                else -> "Unpaid" to StudyLabColors.NeonOrange
                            }
                            Text(statusText, color = statusColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Payment History", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonGreen)
                if (sorted.isNotEmpty()) {
                    TextButton(onClick = { onGenerateReceipt(sorted.first()) }) {
                        Text("Latest Receipt", color = StudyLabColors.NeonCyan)
                    }
                }
            }
        }

        if (sorted.isEmpty()) {
            item {
                Text("No payments yet", color = StudyLabColors.TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        } else {
            items(sorted) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(p.month, style = MaterialTheme.typography.bodyMedium)
                            val detail = buildString {
                                append(p.paidDate?.let { dateFmt.format(it) } ?: "")
                                append(" \u2022 ${p.mode.name}")
                                if (p.lateFee > 0) append(" \u2022 +\u20B9${p.lateFee.toInt()} late")
                            }
                            Text(detail, style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("\u20B9${p.amount.toInt()}", color = StudyLabColors.NeonGreen, style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { onGenerateReceipt(p) }) { Text("Receipt", color = StudyLabColors.NeonCyan) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MembershipField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
