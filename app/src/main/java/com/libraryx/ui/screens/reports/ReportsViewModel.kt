package com.libraryx.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.data.model.StudyLabStats
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class DatePreset { ThisMonth, LastMonth, Last3, Last6, ThisYear, Custom }

data class ReportPayment(val student: String, val date: LocalDate, val amount: Double, val mode: PaymentMode, val lateFee: Double)
data class MonthBreakdown(val label: String, val total: Int, val paid: Int, val unpaid: Int, val revenue: Double)

/** Mirrors src/pages/Reports.tsx's `dateRange`/`filteredPayments`/`monthlyBreakdown` memos. */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    data class UiState(
        val students: List<Student> = emptyList(),
        val settings: AppSettings = AppSettings.Default,
        val stats: StudyLabStats = StudyLabStats(),
        val preset: DatePreset = DatePreset.ThisMonth,
        val customStart: LocalDate = LocalDate.now().withDayOfMonth(1),
        val customEnd: LocalDate = LocalDate.now()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    val repository: StateFlow<StudyLabRepository?> get() = sessionHolder.repository

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(Triple(emptyList<Student>(), AppSettings.Default, StudyLabStats()))
                else combine(repo.students, repo.settings, repo.stats) { s, st, stats -> Triple(s, st, stats) }
            }.collectLatest { (students, settings, stats) ->
                _state.value = _state.value.copy(students = students, settings = settings, stats = stats)
            }
        }
    }

    fun setPreset(preset: DatePreset) { _state.value = _state.value.copy(preset = preset) }
    fun setCustomRange(start: LocalDate, end: LocalDate) { _state.value = _state.value.copy(customStart = start, customEnd = end) }

    fun dateRange(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (_state.value.preset) {
            DatePreset.ThisMonth -> now.withDayOfMonth(1) to now
            DatePreset.LastMonth -> {
                val lm = now.minusMonths(1)
                lm.withDayOfMonth(1) to lm.withDayOfMonth(lm.lengthOfMonth())
            }
            DatePreset.Last3 -> now.minusMonths(2).withDayOfMonth(1) to now
            DatePreset.Last6 -> now.minusMonths(5).withDayOfMonth(1) to now
            DatePreset.ThisYear -> LocalDate.of(now.year, 1, 1) to now
            DatePreset.Custom -> _state.value.customStart to _state.value.customEnd
        }
    }

    fun filteredPayments(): List<ReportPayment> {
        val (start, end) = dateRange()
        return _state.value.students.flatMap { s ->
            s.payments.mapNotNull { p ->
                val d = runCatching { LocalDate.parse(p.date.take(10)) }.getOrNull() ?: return@mapNotNull null
                if (d in start..end) ReportPayment(s.name, d, p.amount, p.mode, p.lateFee) else null
            }
        }.sortedByDescending { it.date }
    }

    fun monthlyBreakdown(): List<MonthBreakdown> {
        val (start, end) = dateRange()
        val active = _state.value.students.filter { it.status == StudentStatus.Active }
        val result = mutableListOf<MonthBreakdown>()
        var cursor = YearMonth.of(start.year, start.monthValue)
        val endYm = YearMonth.of(end.year, end.monthValue)
        val fmt = DateTimeFormatter.ofPattern("MMM yyyy")
        while (!cursor.isAfter(endYm)) {
            var paidCount = 0
            var revenue = 0.0
            active.forEach { s ->
                val paymentsInMonth = s.payments.filter { p ->
                    val d = runCatching { LocalDate.parse(p.date.take(10)) }.getOrNull()
                    d != null && d.monthValue == cursor.monthValue && d.year == cursor.year
                }
                if (paymentsInMonth.isNotEmpty()) {
                    paidCount++
                    revenue += paymentsInMonth.sumOf { it.amount }
                }
            }
            result.add(
                MonthBreakdown(
                    label = cursor.atDay(1).format(fmt),
                    total = active.size,
                    paid = paidCount,
                    unpaid = active.size - paidCount,
                    revenue = revenue
                )
            )
            cursor = cursor.plusMonths(1)
        }
        return result
    }

    fun filteredRevenue(): Double = filteredPayments().sumOf { it.amount }
    fun filteredLateFees(): Double = filteredPayments().sumOf { it.lateFee }

    fun getStatus(student: Student): PaymentStatus =
        sessionHolder.repository.value?.getStatus(student, _state.value.settings) ?: PaymentStatus.Unpaid
    fun getOverdue(student: Student): Int = sessionHolder.repository.value?.getOverdue(student) ?: 0
    fun getLastPay(student: Student): String? = sessionHolder.repository.value?.getLastPay(student)
}
