package com.libraryx.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.libraryx.data.model.SubAdminDoc
import com.libraryx.data.remote.firebase.AuthResult
import com.libraryx.data.remote.firebase.AuthService
import com.libraryx.data.remote.firebase.SubAdminService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mirrors src/context/SaasAuthContext.tsx — exposes `firebaseUser` / `subAdmin` / `loading`
 * and login/signup/logout actions to every SaaS screen, exactly like the original React
 * Context did via `useSaasAuth()`. Scoped to the nav graph (not a screen) so it survives
 * navigation, matching `<SaasAuthProvider>` wrapping the whole `<Routes>` tree in App.tsx.
 */
@HiltViewModel
class SaasAuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val subAdminService: SubAdminService
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val firebaseUser: FirebaseUser? = null,
        val subAdmin: SubAdminDoc? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // Mirrors the `onAuthStateChanged` useEffect in SaasAuthContext.tsx.
        viewModelScope.launch {
            authService.watchAuthState().collectLatest { user ->
                if (user == null) {
                    _state.value = UiState(loading = false, firebaseUser = null, subAdmin = null)
                } else {
                    val sa = runCatching {
                        subAdminService.ensureSubAdminFromAuth(user.uid, user.email ?: "", user.displayName)
                    }.getOrNull()
                    _state.value = UiState(loading = false, firebaseUser = user, subAdmin = sa)
                }
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = authService.loginUser(email, password)) {
                is AuthResult.Success -> onResult(true, null)
                is AuthResult.Failure -> onResult(false, result.error)
            }
        }
    }

    fun signup(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = authService.signupUser(email, password)) {
                is AuthResult.Success -> onResult(true, null)
                is AuthResult.Failure -> onResult(false, result.error)
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = authService.signInWithGoogleIdToken(idToken)) {
                is AuthResult.Success -> onResult(true, null)
                is AuthResult.Failure -> onResult(false, result.error)
            }
        }
    }

    fun logout() {
        authService.logoutUser()
    }
}
