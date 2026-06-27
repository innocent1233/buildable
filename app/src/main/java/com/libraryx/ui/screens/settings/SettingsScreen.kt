package com.libraryx.ui.screens.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.libraryx.ui.components.AppDropdown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.data.model.NotifyFor
import com.libraryx.ui.theme.StudyLabColors

/**
 * Compose port of src/pages/Settings.tsx. `requestNotificationPermission`/
 * `getNotificationPermission` from src/lib/notifications.ts become the standard Android
 * runtime permission flow (`POST_NOTIFICATIONS`, API 33+) via `rememberLauncherForActivityResult`.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var labName by remember(settings) { mutableStateOf(settings.labName) }
    var totalSeats by remember(settings) { mutableStateOf(settings.totalSeats.toString()) }
    var overdueDays by remember(settings) { mutableStateOf(settings.overdueDays.toString()) }
    var dueDate by remember(settings) { mutableStateOf(settings.dueDate.toString()) }

    var lateFeeEnabled by remember(settings) { mutableStateOf(settings.lateFee.enabled) }
    var lateFeeAmount by remember(settings) { mutableStateOf(settings.lateFee.amount.toString()) }
    var lateFeeAfterDays by remember(settings) { mutableStateOf(settings.lateFee.afterDays.toString()) }
    var lateFeeCompound by remember(settings) { mutableStateOf(settings.lateFee.compound) }

    var notifEnabled by remember(settings) { mutableStateOf(settings.notifications.enabled) }
    var notifTime by remember(settings) { mutableStateOf(settings.notifications.time) }
    var notifFor by remember(settings) { mutableStateOf(settings.notifications.notifyFor) }
    var notifSound by remember(settings) { mutableStateOf(settings.notifications.sound) }

    var portalEnabled by remember(settings) { mutableStateOf(settings.studentPortalEnabled) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) notifEnabled = true else Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)

        SettingsSection(title = "Lab Details", color = StudyLabColors.NeonGreen) {
            OutlinedTextField(value = labName, onValueChange = { labName = it }, label = { Text("Lab Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = totalSeats, onValueChange = { totalSeats = it.filter { c -> c.isDigit() } }, label = { Text("Total Seats") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dueDate, onValueChange = { dueDate = it.filter { c -> c.isDigit() } }, label = { Text("Default Due Day of Month") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = overdueDays, onValueChange = { overdueDays = it.filter { c -> c.isDigit() } }, label = { Text("Overdue After (days)") }, modifier = Modifier.fillMaxWidth())
        }

        SettingsSection(title = "Late Fee", color = StudyLabColors.NeonOrange) {
            SwitchRow("Enable Late Fee", lateFeeEnabled) { lateFeeEnabled = it }
            if (lateFeeEnabled) {
                OutlinedTextField(value = lateFeeAmount, onValueChange = { lateFeeAmount = it.filter { c -> c.isDigit() } }, label = { Text("Late Fee Amount (\u20B9)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lateFeeAfterDays, onValueChange = { lateFeeAfterDays = it.filter { c -> c.isDigit() } }, label = { Text("Apply After (days)") }, modifier = Modifier.fillMaxWidth())
                SwitchRow("Compound Weekly", lateFeeCompound) { lateFeeCompound = it }
            }
        }

        SettingsSection(title = "Notifications", color = StudyLabColors.NeonCyan) {
            SwitchRow("Enable Notifications", notifEnabled) { checked ->
                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notifEnabled = checked
                }
            }
            if (notifEnabled) {
                OutlinedTextField(value = notifTime, onValueChange = { notifTime = it }, label = { Text("Notification Time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                AppDropdown(
                    label = "Notify For",
                    selected = notifFor.name,
                    options = NotifyFor.entries.map { it.name },
                    onSelect = { notifFor = NotifyFor.valueOf(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                SwitchRow("Notification Sound", notifSound) { notifSound = it }
            }
        }

        SettingsSection(title = "Student Portal", color = StudyLabColors.NeonGreen) {
            SwitchRow("Enable Student Self-Service Portal", portalEnabled) { portalEnabled = it }
        }

        Button(onClick = {
            viewModel.save(
                labName, totalSeats.toIntOrNull() ?: 60, overdueDays.toIntOrNull() ?: 5, dueDate.toIntOrNull() ?: 1,
                lateFeeEnabled, lateFeeAmount.toDoubleOrNull() ?: 50.0, lateFeeAfterDays.toIntOrNull() ?: 5, lateFeeCompound,
                notifEnabled, notifTime, notifFor, notifSound, portalEnabled
            ) {
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Save Settings")
        }
    }
}

@Composable
private fun SettingsSection(title: String, color: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// NotifyForDropdown removed — replaced by shared AppDropdown component
