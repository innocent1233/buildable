package com.libraryx.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Failure(val error: String) : AuthResult()
}

data class LogoutResult(val success: Boolean, val error: String? = null)

/**
 * Mirrors src/services/auth.service.ts. Email/password and Google sign-in flows funnel
 * through here exactly as in the original `loginUser`/`signupUser`/`loginWithGoogle`/
 * `logoutUser`/`watchAuthState`/`getCurrentUser`.
 *
 * Google Sign-In on Android is driven by a Google ID token obtained via Credential Manager
 * in the UI layer (see SaasLoginScreen) and exchanged here via [signInWithGoogleIdToken] —
 * this replaces the web's `signInWithPopup(auth, googleProvider)`.
 */
@Singleton
class AuthService @Inject constructor(private val auth: FirebaseAuth) {

    /** Mirrors `friendlyAuthError`. */
    private fun friendlyAuthError(code: String): String = when (code) {
        "ERROR_INVALID_EMAIL" -> "Invalid email address."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> "Incorrect email or password."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
        "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Google sign-in is not enabled. Enable it in the Firebase Console \u2192 Authentication \u2192 Sign-in method."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with this email using a different sign-in method."
        else -> "Authentication failed. Please try again."
    }

    private fun toErrorMessage(err: Throwable): String =
        (err as? FirebaseAuthException)?.errorCode?.let { friendlyAuthError(it) } ?: (err.message ?: "Unknown error")

    suspend fun loginUser(email: String, password: String): AuthResult = try {
        val cred = auth.signInWithEmailAndPassword(email.trim(), password).await()
        AuthResult.Success(cred.user!!)
    } catch (e: Exception) {
        AuthResult.Failure(toErrorMessage(e))
    }

    suspend fun signupUser(email: String, password: String): AuthResult = try {
        val cred = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        AuthResult.Success(cred.user!!)
    } catch (e: Exception) {
        AuthResult.Failure(toErrorMessage(e))
    }

    /** Exchanges a Google ID token (obtained via Credential Manager) for a Firebase session. */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val cred = auth.signInWithCredential(credential).await()
        AuthResult.Success(cred.user!!)
    } catch (e: Exception) {
        AuthResult.Failure(toErrorMessage(e))
    }

    fun logoutUser(): LogoutResult = try {
        auth.signOut()
        LogoutResult(success = true)
    } catch (e: Exception) {
        LogoutResult(success = false, error = e.message)
    }

    /** Mirrors `watchAuthState` as a cold Flow instead of an unsubscribe callback. */
    fun watchAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}
