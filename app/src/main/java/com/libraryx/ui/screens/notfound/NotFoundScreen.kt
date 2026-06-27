package com.libraryx.ui.screens.notfound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.libraryx.ui.theme.StudyLabColors

/** Port of src/pages/NotFound.tsx — 404 fallback screen. */
@Composable
fun NotFoundScreen(onNavigateHome: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("404", style = MaterialTheme.typography.headlineLarge, color = StudyLabColors.NeonGreen)
            Text("Oops! Page not found.", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onNavigateHome) { Text("Return to Dashboard") }
        }
    }
}
