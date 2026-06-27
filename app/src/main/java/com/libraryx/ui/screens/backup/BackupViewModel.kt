package com.libraryx.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libraryx.data.local.BackupSerializer
import com.libraryx.data.model.Student
import com.libraryx.ui.navigation.SaasSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mirrors src/pages/Backup.tsx (Solo mode export/import) and src/pages/saas/SaasBackup.tsx
 * (Firestore-scoped export). Both produce/consume the same [BackupSerializer] JSON shape,
 * so a backup exported in either mode can be inspected the same way; only [importJson]
 * (which calls `repository.importData`) is a no-op in SaaS mode, matching the original's
 * `FirebaseStudyLabRepository.importData` deliberately not supporting bulk import.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    data class UiState(val students: List<Student> = emptyList())

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(emptyList()) else repo.students
            }.collectLatest { _state.value = UiState(it) }
        }
    }

    fun exportJson(onResult: (String?) -> Unit) {
        val repo = sessionHolder.repository.value ?: return onResult(null)
        viewModelScope.launch {
            val students = repo.students.first()
            val settings = repo.settings.first()
            onResult(BackupSerializer.export(students, settings))
        }
    }

    fun importJson(rawJson: String, onResult: (Boolean, Int, String?) -> Unit) {
        val repo = sessionHolder.repository.value ?: return onResult(false, 0, "Not signed in")
        viewModelScope.launch {
            try {
                val result = BackupSerializer.import(rawJson)
                repo.importData(result.students, result.settings)
                onResult(true, result.students.size, null)
            } catch (e: Exception) {
                onResult(false, 0, "Invalid backup file")
            }
        }
    }
}
