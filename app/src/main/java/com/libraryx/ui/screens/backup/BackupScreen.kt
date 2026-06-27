package com.libraryx.ui.screens.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.libraryx.ui.theme.StudyLabColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Compose port of src/pages/Backup.tsx / src/pages/saas/SaasBackup.tsx. The browser's
 * `<a download>` / hidden `<input type=file>` pattern is replaced with Android's Storage
 * Access Framework (`CreateDocument` / `OpenDocument`), which is the native equivalent of
 * "save a file the user picks a location for" / "let the user pick a file to read".
 */
@Composable
fun BackupScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var pendingImportCount by remember { mutableStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportJson { json ->
            if (json == null) {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            } else {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess {
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
        }.getOrNull()
        if (text == null) {
            Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
        } else {
            // Mirrors `importBackup` parsing eagerly so we can show the confirm dialog with a count.
            runCatching { com.libraryx.data.local.BackupSerializer.import(text) }
                .onSuccess { result ->
                    pendingImportJson = text
                    pendingImportCount = result.students.size
                }
                .onFailure {
                    Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Backup & Restore", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Export Backup", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonGreen)
                Text(
                    "Download all data as a JSON file. Includes ${state.students.size} students and all payment records.",
                    color = StudyLabColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    val filename = "StudyLab_Backup_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))}.json"
                    exportLauncher.launch(filename)
                }) { Text("Export Backup") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Import Backup", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonCyan)
                Text(
                    "Restore data from a previously exported JSON file. This will replace all current data.",
                    color = StudyLabColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import Backup") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auto-Save Active", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonGreen)
                Text(
                    "All changes are automatically saved on this device (or to the cloud in SaaS mode). " +
                        "We recommend exporting a backup regularly for safety.",
                    color = StudyLabColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text("Import $pendingImportCount students?") },
            text = { Text("This will replace all current data.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importJson(json) { ok, count, err ->
                        Toast.makeText(
                            context,
                            if (ok) "Imported $count students successfully" else (err ?: "Import failed"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    pendingImportJson = null
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { pendingImportJson = null }) { Text("Cancel") } }
        )
    }
}
