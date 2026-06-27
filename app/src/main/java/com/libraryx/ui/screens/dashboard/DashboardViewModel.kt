package com.libraryx.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.data.model.StudyLabStats
import com.libraryx.ui.navigation.SaasSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mirrors src/pages/Dashboard.tsx: pulls `students`, `settings`, `stats`, and
 * `getStatus`/`getOverdue` from the active StudyLabContext (here, [SaasSessionHolder]'s
 * bound [com.libraryx.data.repository.StudyLabRepository]) and exposes a single UI state.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    data class UiState(
        val students: List<Student> = emptyList(),
        val settings: AppSettings = AppSettings.Default,
        val stats: StudyLabStats = StudyLabStats(),
        val recentlyOverdue: List<Student> = emptyList()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(UiState())
                else combine(repo.students, repo.settings, repo.stats) { students, settings, stats ->
                    val active = students.filter { it.status == StudentStatus.Active }
                    val overdue = active
                        .filter { repo.getStatus(it, settings) == PaymentStatus.Overdue }
                        .sortedByDescending { repo.getOverdue(it) }
                        .take(5)
                    UiState(students = students, settings = settings, stats = stats, recentlyOverdue = overdue)
                }
            }.collectLatest { _state.value = it }
        }
    }
}
