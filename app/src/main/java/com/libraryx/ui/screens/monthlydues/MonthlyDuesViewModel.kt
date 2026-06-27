package com.libraryx.ui.screens.monthlydues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.data.repository.StudyLabRepository
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
import java.time.LocalDate
import javax.inject.Inject

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Mirrors src/pages/MonthlyDues.tsx's state and the `months`/`years` memoized computations. */
@HiltViewModel
class MonthlyDuesViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    data class UiState(
        val students: List<Student> = emptyList(),
        val settings: AppSettings = AppSettings.Default,
        val selectedYear: Int = LocalDate.now().year
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val repository: StateFlow<StudyLabRepository?> get() = sessionHolder.repository

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(Pair(emptyList<Student>(), AppSettings.Default))
                else combine(repo.students, repo.settings) { s, st -> Pair(s, st) }
            }.collectLatest { (students, settings) ->
                _state.value = _state.value.copy(students = students, settings = settings)
            }
        }
    }

    fun setYear(year: Int) { _state.value = _state.value.copy(selectedYear = year) }

    fun years(): List<Int> {
        val now = LocalDate.now().year
        return listOf(now, now - 1)
    }

    /** Mirrors the `months` useMemo: Jan..currentMonth for the current year, full year otherwise. */
    fun months(): List<Pair<Int, String>> {
        val now = LocalDate.now()
        val result = mutableListOf<Pair<Int, String>>()
        for (m in 0 until 12) {
            if (_state.value.selectedYear == now.year && m > now.monthValue - 1) break
            result.add(m to MONTH_NAMES[m])
        }
        return result
    }

    fun activeStudents(): List<Student> = _state.value.students.filter { it.status == StudentStatus.Active }

    fun statusFor(student: Student, month: Int): MonthPaymentStatus {
        val repo = sessionHolder.repository.value ?: return MonthPaymentStatus.NotApplicable
        return repo.getMonthStatus(student, _state.value.selectedYear, month, _state.value.settings)
    }
}
