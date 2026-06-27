package com.libraryx.ui.screens.modeselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.libraryx.ui.theme.StudyLabColors

/**
 * Port of src/pages/ModeSelect.tsx — the mode selection screen (Solo vs SaaS).
 * NOTE: In the current routing (src/App.tsx), "/" immediately redirects to "/saas/login"
 * so this screen is unreachable unless the root redirect is changed. It is preserved
 * for completeness.
 */
@Composable
fun ModeSelectScreen(
    onSelectSaas: () -> Unit,
    onSelectSolo: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("LIBRARY X", style = MaterialTheme.typography.headlineLarge, color = StudyLabColors.NeonGreen, textAlign = TextAlign.Center)
            Text("Choose how you'd like to use the app", color = StudyLabColors.TextMuted, textAlign = TextAlign.Center)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cloud / SaaS Mode", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonGreen)
                    Text("Multi-device sync, Firebase backend, team access.", color = StudyLabColors.TextMuted)
                    Button(onClick = onSelectSaas, modifier = Modifier.fillMaxWidth()) { Text("Use Cloud Mode") }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Solo / Local Mode", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonCyan)
                    Text("Offline-only, stored on this device. No account needed.", color = StudyLabColors.TextMuted)
                    OutlinedButton(onClick = onSelectSolo, modifier = Modifier.fillMaxWidth()) { Text("Use Solo Mode") }
                }
            }
        }
    }
}
