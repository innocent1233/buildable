package com.libraryx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.libraryx.ui.navigation.SaasSessionHolder
import com.libraryx.ui.navigation.StudyLabNavGraph
import com.libraryx.ui.theme.StudyLabTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity entry point. Hosts the Compose content tree:
 *   StudyLabTheme → StudyLabNavGraph
 *
 * Google Sign-In is handled here (not in a ViewModel or Composable) because
 * `CredentialManager.getCredential()` requires an `Activity` context — the Android
 * equivalent of `signInWithPopup(auth, new GoogleAuthProvider())` in
 * `src/services/auth.service.ts`.
 *
 * The retrieved ID token is passed back to [StudyLabNavGraph] via the
 * `onGoogleSignInRequested` callback and forwarded to [SaasAuthViewModel.loginWithGoogleIdToken].
 *
 * Replace `YOUR_WEB_CLIENT_ID` below with the OAuth 2.0 Web Client ID from your Firebase
 * project's Google Cloud Console (the same client ID used for the web app's Firebase config).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionHolder: SaasSessionHolder

    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyLabTheme {
                StudyLabNavGraph(
                    onGoogleSignInRequested = ::launchGoogleSignIn,
                    sessionHolder = sessionHolder
                )
            }
        }
    }

    /**
     * Launches the Credential Manager bottom-sheet picker and returns an ID token
     * string to [onToken], or `null` on failure.
     *
     * Replace `"YOUR_WEB_CLIENT_ID"` with the actual value from:
     * Firebase Console → Project Settings → General → Your apps → Web client ID
     */
    private fun launchGoogleSignIn(onToken: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("YOUR_WEB_CLIENT_ID") // ← replace with real value
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    onToken(googleCred.idToken)
                } else {
                    onToken(null)
                }
            } catch (e: GetCredentialException) {
                onToken(null)
            }
        }
    }
}
