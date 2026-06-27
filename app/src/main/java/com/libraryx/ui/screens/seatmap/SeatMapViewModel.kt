package com.libraryx.ui.screens.seatmap

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
import javax.inject.Inject

/** Mirrors src/pages/SeatMap.tsx's `seatMap` memo + payment recording from the seat detail panel. */
@HiltViewModel
class SeatMapViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    data class UiState(val students: List<Student> = emptyList(), val settings: AppSettings = AppSettings.Default)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    val repository: StateFlow<StudyLabRepository?> get() = sessionHolder.repository

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(Pair(emptyList<Student>(), AppSettings.Default))
                else combine(repo.students, repo.settings) { s, st -> Pair(s, st) }
            }.collectLatest { (students, settings) -> _state.value = UiState(students, settings) }
        }
    }

    fun seatMap(): Map<Int, Student> = _state.value.students
        .filter { it.status == StudentStatus.Active && it.seat != null }
        .associateBy { it.seat!! }

    fun statusFor(student: Student): PaymentStatus =
        sessionHolder.repository.value?.getStatus(student, _state.value.settings) ?: PaymentStatus.Unpaid

    fun getLateFee(student: Student): Double =
        sessionHolder.repository.value?.getLateFee(student, _state.value.settings) ?: 0.0

    fun recordPayment(student: Student, date: String, amount: Double, mode: PaymentMode, onDone: () -> Unit) {
        val repo = sessionHolder.repository.value ?: return
        viewModelScope.launch {
            val lateFee = getLateFee(student)
            repo.addPayment(student.id, date, amount, mode, "", lateFee)
            onDone()
        }
    }
}
