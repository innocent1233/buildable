package com.libraryx.ui.screens.saaslogin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libraryx.ui.navigation.SaasAuthViewModel
import com.libraryx.ui.theme.StudyLabColors

/**
 * Compose port of src/pages/saas/SaasLogin.tsx.
 *
 * The web app's `configured` flag (whether `lib/firebase.ts` has real config values) has
 * no Android equivalent — `google-services.json` is required at build time, so the
 * "Firebase not configured" branch is omitted (see migration report).
 *
 * Google Sign-In here is driven by Credential Manager in the calling Activity; this
 * Composable receives an already-fetched ID token via [onGoogleSignInRequested] so it stays
 * platform-API-free, matching how the original component only calls `loginWithGoogle()` from
 * SaasAuthContext without touching `signInWithPopup` directly.
 */
@Composable
fun SaasLoginScreen(
    viewModel: SaasAuthViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit,
    onGoogleSignInRequested: (onToken: (String?) -> Unit) -> Unit,
    onNavigateToStudentPortal: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignup by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Mirrors `useEffect(() => { if (!loading && subAdmin) navigate(...) }, [...])`
    if (!state.loading && state.subAdmin != null) {
        onAuthenticated()
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(StudyLabColors.NeonGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("LX", style = MaterialTheme.typography.headlineMedium, color = StudyLabColors.NeonGreen)
                        }
                        Text(
                            "LIBRARY X",
                            style = MaterialTheme.typography.headlineMedium,
                            color = StudyLabColors.NeonGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "STUDY LAB MANAGEMENT SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudyLabColors.NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }

                    error?.let { msg ->
                        Card(colors = CardDefaults.cardColors(containerColor = StudyLabColors.NeonPink.copy(alpha = 0.12f))) {
                            Row_ErrorRow(msg)
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = {
                            error = null
                            busy = true
                            val trimmedEmail = email.trim()
                            if (isSignup) {
                                viewModel.signup(trimmedEmail, password) { ok, err ->
                                    busy = false
                                    if (!ok) error = err ?: "Authentication failed"
                                }
                            } else {
                                viewModel.login(trimmedEmail, password) { ok, err ->
                                    busy = false
                                    if (!ok) error = err ?: "Authentication failed"
                                }
                            }
                        },
                        enabled = !busy && email.isNotBlank() && password.length >= 6,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "Please wait..." else if (isSignup) "Create account" else "Sign in")
                    }

                    TextButton(onClick = { isSignup = !isSignup }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (isSignup) "Already have an account? Sign in" else "First time? Create your lab account",
                            color = StudyLabColors.NeonCyan
                        )
                    }

                    Text(
                        "OR CONTINUE WITH",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudyLabColors.TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    OutlinedButton(
                        onClick = {
                            error = null
                            busy = true
                            onGoogleSignInRequested { idToken ->
                                if (idToken == null) {
                                    busy = false
                                    error = "Google sign-in failed"
                                } else {
                                    viewModel.loginWithGoogleIdToken(idToken) { ok, err ->
                                        busy = false
                                        if (!ok) error = err ?: "Google sign-in failed"
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "Please wait..." else "Sign in with Google")
                    }
                }
            }

            TextButton(onClick = onNavigateToStudentPortal, modifier = Modifier.fillMaxWidth()) {
                Text("Student Portal \u2192", color = StudyLabColors.TextMuted)
            }
        }
    }
}

@Composable
private fun Row_ErrorRow(message: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = StudyLabColors.NeonPink)
        Text(message, color = StudyLabColors.NeonPink, style = MaterialTheme.typography.bodyMedium)
    }
}
