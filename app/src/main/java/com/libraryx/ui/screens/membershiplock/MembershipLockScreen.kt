package com.libraryx.ui.screens.membershiplock

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.remote.firebase.MembershipService
import com.libraryx.ui.theme.StudyLabColors
import kotlinx.coroutines.launch

/**
 * Compose port of src/components/MembershipLockScreen.tsx — full-screen overlay shown when
 * a student's `isLocked = true`. The lab owner enters their numeric passcode to unlock.
 * `attempts` is capped at 3 before the form locks itself, matching the original's
 * `blocked = attempts >= 3` guard.
 */
@Composable
fun MembershipLockScreen(
    student: SaasStudent,
    membershipService: MembershipService,
    onUnlock: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val blocked = attempts >= 3

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudyLabColors.NeonPink.copy(alpha = 0.4f))
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = StudyLabColors.NeonPink, modifier = Modifier.size(48.dp))

            Text(
                "MEMBERSHIP EXPIRED",
                style = MaterialTheme.typography.titleLarge,
                color = StudyLabColors.NeonPink,
                textAlign = TextAlign.Center
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LockRow("Student", student.name)
                    LockRow("Phone", student.phone)
                    LockRow("Days Overdue", "${student.daysOverdue} days", highlight = true)
                }
            }

            error?.let { msg ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = StudyLabColors.NeonPink)
                    Text(msg, color = StudyLabColors.NeonPink, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = passcode,
                onValueChange = { v -> passcode = v.filter { it.isDigit() }.take(6) },
                label = { Text("Lab Owner Passcode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !blocked && !busy
            )

            Button(
                onClick = {
                    if (blocked) return@Button
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            val ok = membershipService.verifySubAdminPasscode(student.subAdminUid, passcode.trim())
                            if (!ok) {
                                val next = attempts + 1
                                attempts = next
                                error = if (next >= 3) "Too many attempts. Refresh to try again."
                                        else "Invalid passcode (${3 - next} left)"
                            } else {
                                membershipService.unlockStudent(student.uid)
                                Toast.makeText(context, "Membership unlocked", Toast.LENGTH_SHORT).show()
                                onUnlock()
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Unlock failed"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && !blocked,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Verifying..." else if (blocked) "Locked" else "Unlock Access") }

            if (onClose != null) {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }

            Text(
                "\uD83D\uDCDE Contact your lab owner for the passcode",
                style = MaterialTheme.typography.labelSmall,
                color = StudyLabColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LockRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
        Text(value, color = if (highlight) StudyLabColors.NeonPink else MaterialTheme.colorScheme.onSurface)
    }
}
