package com.libraryx.ui.screens.studentportal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.SubAdminDoc
import com.libraryx.data.remote.firebase.SubAdminService
import com.libraryx.domain.Passcode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Mirrors src/pages/saas/SaasStudentPortal.tsx: a student authenticates with
 * phone+PIN (looked up against every `students` doc sharing that phone, like the
 * original's `for (const d of snap.docs)` loop), then sees their own membership card
 * and payment history.
 */
@HiltViewModel
class StudentPortalViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val subAdminService: SubAdminService
) : ViewModel() {

    data class UiState(
        val busy: Boolean = false,
        val error: String? = null,
        val student: SaasStudent? = null,
        val lab: SubAdminDoc? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun login(phone: String, pin: String) {
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val snap = db.collection("students").whereEqualTo("phone", phone.trim()).get().await()
                if (snap.isEmpty) {
                    _state.value = _state.value.copy(busy = false, error = "Student not found")
                    return@launch
                }
                var found: SaasStudent? = null
                for (doc in snap.documents) {
                    val data = doc.toObject(SaasStudent::class.java)?.copy(uid = doc.id) ?: continue
                    val hash = data.pinHash ?: continue
                    if (Passcode.verifyPasscode(pin.trim(), hash)) {
                        found = data
                        break
                    }
                }
                if (found == null) {
                    _state.value = _state.value.copy(busy = false, error = "Invalid PIN, or no PIN set. Contact your lab owner.")
                    return@launch
                }
                val lab = subAdminService.getSubAdminDoc(found.subAdminUid)
                if (lab == null) {
                    _state.value = _state.value.copy(busy = false, error = "Lab not found")
                    return@launch
                }
                _state.value = UiState(busy = false, student = found, lab = lab)
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, error = e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        _state.value = UiState()
    }
}
