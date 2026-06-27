package com.libraryx.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.PaymentStatus
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

enum class FilterType { All, Paid, Unpaid, Overdue }
enum class SortType { Name, Date, Status }

/**
 * Mirrors all of the state and handler functions in src/pages/Students.tsx: search/filter/
 * sort, add/edit/delete, single + bulk payment recording, and selection state for bulk
 * actions. UI-only state (which dialog is open) lives in the Composable itself; this
 * ViewModel owns everything that touches the repository.
 */
@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    data class UiState(
        val students: List<Student> = emptyList(),
        val settings: AppSettings = AppSettings.Default,
        val search: String = "",
        val filter: FilterType = FilterType.All,
        val sort: SortType = SortType.Name,
        val selectedIds: Set<String> = emptySet()
    ) {
        fun statusOf(repo: StudyLabRepository?, s: Student): PaymentStatus =
            repo?.getStatus(s, settings) ?: PaymentStatus.Unpaid

        fun filtered(repo: StudyLabRepository?): List<Student> {
            var list = students.filter {
                it.name.contains(search, ignoreCase = true) || it.phone.contains(search)
            }
            if (filter != FilterType.All) {
                list = list.filter { statusOf(repo, it).name == filter.name }
            }
            list = when (sort) {
                SortType.Name -> list.sortedBy { it.name }
                SortType.Status -> list.sortedBy { statusOf(repo, it).ordinal }
                SortType.Date -> list.sortedByDescending { repo?.getLastPay(it) ?: "" }
            }
            return list
        }
    }

    private val _filterState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _filterState.asStateFlow()

    val repository: StateFlow<StudyLabRepository?> get() = sessionHolder.repository

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(Pair(emptyList<Student>(), AppSettings.Default))
                else combine(repo.students, repo.settings) { s, st -> Pair(s, st) }
            }.collectLatest { (students, settings) ->
                _filterState.value = _filterState.value.copy(students = students, settings = settings)
            }
        }
    }

    fun setSearch(value: String) { _filterState.value = _filterState.value.copy(search = value) }
    fun setFilter(value: FilterType) { _filterState.value = _filterState.value.copy(filter = value) }
    fun setSort(value: SortType) { _filterState.value = _filterState.value.copy(sort = value) }

    fun toggleSelect(id: String) {
        val current = _filterState.value.selectedIds
        _filterState.value = _filterState.value.copy(
            selectedIds = if (id in current) current - id else current + id
        )
    }

    fun toggleSelectAll(visibleIds: List<String>) {
        val current = _filterState.value.selectedIds
        _filterState.value = _filterState.value.copy(
            selectedIds = if (current.size == visibleIds.size) emptySet() else visibleIds.toSet()
        )
    }

    fun clearSelection() { _filterState.value = _filterState.value.copy(selectedIds = emptySet()) }

    fun addStudent(
        name: String, phone: String, email: String, fee: Double, joiningDate: String,
        status: StudentStatus, seat: Int?, pin: String?, onDone: (Boolean, String?) -> Unit
    ) {
        val repo = sessionHolder.repository.value ?: return onDone(false, "Not signed in")
        viewModelScope.launch {
            if (seat != null && repo.isSeatTaken(seat)) {
                onDone(false, "Seat already occupied")
                return@launch
            }
            repo.addStudent(name, phone, email, fee, joiningDate, status, seat, pin)
            onDone(true, null)
        }
    }

    fun editStudent(
        id: String, name: String, phone: String, email: String, fee: Double, joiningDate: String,
        status: StudentStatus, seat: Int?, pin: String?, onDone: (Boolean, String?) -> Unit
    ) {
        val repo = sessionHolder.repository.value ?: return onDone(false, "Not signed in")
        viewModelScope.launch {
            if (seat != null && repo.isSeatTaken(seat, excludeId = id)) {
                onDone(false, "Seat already occupied")
                return@launch
            }
            repo.updateStudent(id) {
                this.name = name; this.phone = phone; this.email = email
                this.monthlyFee = fee; this.joiningDate = joiningDate; this.status = status
                seat(seat); pin(pin)
            }
            onDone(true, null)
        }
    }

    fun deleteStudent(id: String) {
        val repo = sessionHolder.repository.value ?: return
        viewModelScope.launch { repo.deleteStudent(id) }
    }

    fun bulkDelete(ids: Set<String>) {
        val repo = sessionHolder.repository.value ?: return
        viewModelScope.launch {
            ids.forEach { repo.deleteStudent(it) }
            clearSelection()
        }
    }

    fun recordPayment(student: Student, date: String, amount: Double, mode: PaymentMode, notes: String, onDone: () -> Unit) {
        val repo = sessionHolder.repository.value ?: return
        viewModelScope.launch {
            val lateFee = repo.getLateFee(student, _filterState.value.settings)
            repo.addPayment(student.id, date, amount, mode, notes, lateFee)
            onDone()
        }
    }

    fun bulkRecordPayment(students: List<Student>, date: String, mode: PaymentMode, notes: String, onDone: () -> Unit) {
        val repo = sessionHolder.repository.value ?: return
        viewModelScope.launch {
            students.forEach { s ->
                repo.addPayment(s.id, date, s.monthlyFee, mode, notes.ifBlank { "Bulk payment" }, 0.0)
            }
            clearSelection()
            onDone()
        }
    }

    fun getLateFee(student: Student): Double {
        val repo = sessionHolder.repository.value ?: return 0.0
        return repo.getLateFee(student, _filterState.value.settings)
    }

    fun getStatus(student: Student): PaymentStatus {
        val repo = sessionHolder.repository.value ?: return PaymentStatus.Unpaid
        return repo.getStatus(student, _filterState.value.settings)
    }

    fun getOverdue(student: Student): Int = sessionHolder.repository.value?.getOverdue(student) ?: 0
    fun getLastPay(student: Student): String? = sessionHolder.repository.value?.getLastPay(student)
}
