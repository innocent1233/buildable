package com.libraryx.domain

import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.OutstandingMonth
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Pure date/fee calculation functions. This object is a 1:1 behavioural port of the
 * free functions in src/lib/store.ts (getPaymentStatus, getStudentDueDay,
 * getMonthPaymentStatus, getDaysOverdue, getLastPaymentDate, calculateLateFee,
 * getOutstandingMonths) plus src/lib/lateFee.ts (getStudentTotalDue, formatLateFeeBreakdown).
 *
 * All functions are deterministic given `today` so they're trivially unit-testable —
 * `today` defaults to the real current date, matching the original `new Date()` usage.
 */
object StudyLabLogic {

    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val MONTH_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    /** Parses a `yyyy-MM-dd` string the same lenient way `new Date(string)` would in JS. */
    private fun parseDateOrNull(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.take(10), ISO_DATE) }.getOrNull()

    /**
     * Each student's monthly due day = day-of-month of their joining date (clamped to 1-28).
     * Mirrors `getStudentDueDay`.
     */
    fun getStudentDueDay(student: Student): Int {
        val d = parseDateOrNull(student.joiningDate) ?: return 1
        val day = d.dayOfMonth
        return min(max(day, 1), 28)
    }

    private fun paidInMonth(student: Student, year: Int, month: Int): Boolean =
        student.payments.any { p ->
            val d = parseDateOrNull(p.date) ?: return@any false
            d.monthValue - 1 == month && d.year == year
        }

    /** Mirrors `getPaymentStatus`. `month` is 0-indexed to match the original JS `Date#getMonth()`. */
    fun getPaymentStatus(student: Student, overdueDays: Int, today: LocalDate = LocalDate.now()): PaymentStatus {
        val currentMonth = today.monthValue - 1
        val currentYear = today.year
        if (paidInMonth(student, currentYear, currentMonth)) return PaymentStatus.Paid

        val dueDay = getStudentDueDay(student)
        if (today.dayOfMonth < dueDay) return PaymentStatus.Unpaid
        if (today.dayOfMonth > dueDay + overdueDays) return PaymentStatus.Overdue
        return PaymentStatus.Unpaid
    }

    /** Mirrors `getMonthPaymentStatus`. `month` is 0-indexed. */
    fun getMonthPaymentStatus(
        student: Student,
        year: Int,
        month: Int,
        overdueDays: Int,
        today: LocalDate = LocalDate.now()
    ): MonthPaymentStatus {
        val joinDate = parseDateOrNull(student.joiningDate) ?: today
        val checkMonth = YearMonth.of(year, month + 1)
        val joinMonth = YearMonth.of(joinDate.year, joinDate.monthValue)
        if (checkMonth < joinMonth) return MonthPaymentStatus.NotApplicable

        val isFuture = year > today.year || (year == today.year && month > today.monthValue - 1)
        if (isFuture) return MonthPaymentStatus.NotApplicable

        if (paidInMonth(student, year, month)) return MonthPaymentStatus.Paid

        val isCurrentMonth = year == today.year && month == today.monthValue - 1
        val dueDay = getStudentDueDay(student)
        if (isCurrentMonth) {
            if (today.dayOfMonth < dueDay) return MonthPaymentStatus.Unpaid
            return if (today.dayOfMonth > dueDay + overdueDays) MonthPaymentStatus.Overdue else MonthPaymentStatus.Unpaid
        }

        return MonthPaymentStatus.Overdue
    }

    /** Mirrors `getDaysOverdue`. */
    fun getDaysOverdue(student: Student, today: LocalDate = LocalDate.now()): Int {
        val currentMonth = today.monthValue - 1
        val currentYear = today.year
        if (paidInMonth(student, currentYear, currentMonth)) return 0
        val dueDay = getStudentDueDay(student)
        return max(0, today.dayOfMonth - dueDay)
    }

    /** Mirrors `getLastPaymentDate`. Returns the raw ISO date string of the most recent payment. */
    fun getLastPaymentDate(student: Student): String? =
        student.payments.maxByOrNull { parseDateOrNull(it.date) ?: LocalDate.MIN }?.date

    /** Mirrors `calculateLateFee`. */
    fun calculateLateFee(student: Student, settings: AppSettings, today: LocalDate = LocalDate.now()): Double {
        if (!settings.lateFee.enabled) return 0.0

        val currentMonth = today.monthValue - 1
        val currentYear = today.year
        if (paidInMonth(student, currentYear, currentMonth)) return 0.0

        val dueDay = getStudentDueDay(student)
        val daysLate = today.dayOfMonth - dueDay - settings.lateFee.afterDays
        if (daysLate <= 0) return 0.0

        return if (settings.lateFee.compound) {
            val weeks = ceil(daysLate / 7.0).toInt()
            settings.lateFee.amount * weeks
        } else {
            settings.lateFee.amount
        }
    }

    /** Mirrors `getOutstandingMonths`: scans the trailing 6 months (inclusive of the current one). */
    fun getOutstandingMonths(
        student: Student,
        settings: AppSettings,
        today: LocalDate = LocalDate.now()
    ): List<OutstandingMonth> {
        val outstanding = mutableListOf<OutstandingMonth>()
        for (i in 5 downTo 0) {
            val ym = YearMonth.of(today.year, today.monthValue).minusMonths(i.toLong())
            val year = ym.year
            val month = ym.monthValue - 1 // 0-indexed, matching JS Date#getMonth()

            val status = getMonthPaymentStatus(student, year, month, settings.overdueDays, today)
            if (status == MonthPaymentStatus.Unpaid || status == MonthPaymentStatus.Overdue) {
                val lateFee = if (settings.lateFee.enabled && status == MonthPaymentStatus.Overdue) {
                    settings.lateFee.amount
                } else {
                    0.0
                }
                outstanding.add(
                    OutstandingMonth(
                        month = MONTH_NAMES[month],
                        year = year,
                        monthNum = month,
                        fee = student.monthlyFee,
                        lateFee = lateFee
                    )
                )
            }
        }
        return outstanding
    }

    /** Mirrors `getStudentTotalDue` in src/lib/lateFee.ts. */
    fun getStudentTotalDue(student: Student, settings: AppSettings, today: LocalDate = LocalDate.now()): TotalDue {
        val lateFee = calculateLateFee(student, settings, today)
        return TotalDue(
            originalFee = student.monthlyFee,
            lateFee = lateFee,
            totalDue = student.monthlyFee + lateFee
        )
    }

    /** Mirrors `formatLateFeeBreakdown` in src/lib/lateFee.ts. */
    fun formatLateFeeBreakdown(originalFee: Double, lateFee: Double): String {
        if (lateFee == 0.0) return "\u20B9${originalFee.toInt()}"
        return "\u20B9${originalFee.toInt()} + \u20B9${lateFee.toInt()} (late) = \u20B9${(originalFee + lateFee).toInt()}"
    }

    data class TotalDue(val originalFee: Double, val lateFee: Double, val totalDue: Double)
}
