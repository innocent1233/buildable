package com.libraryx.domain

import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.data.model.StudyLabStats
import java.time.LocalDate

/**
 * Mirrors the `stats` computation block at the bottom of StudyLabContext.tsx
 * (totalStudents, paidCount, unpaidCount, overdueCount, totalRevenue, pendingRevenue,
 * occupiedSeats, totalLateFees) — derived purely from `students` + `settings`.
 */
object StatsCalculator {

    fun compute(students: List<Student>, settings: AppSettings, today: LocalDate = LocalDate.now()): StudyLabStats {
        val active = students.filter { it.status == StudentStatus.Active }

        fun statusOf(s: Student) = StudyLabLogic.getPaymentStatus(s, settings.overdueDays, today)

        val paidCount = active.count { statusOf(it) == PaymentStatus.Paid }
        val overdueCount = active.count { statusOf(it) == PaymentStatus.Overdue }
        val unpaidCount = active.size - paidCount

        val currentMonth = today.monthValue - 1
        val currentYear = today.year

        val totalRevenue = active.sumOf { s ->
            s.payments.filter { p ->
                val d = runCatching { LocalDate.parse(p.date.take(10)) }.getOrNull()
                d != null && d.monthValue - 1 == currentMonth && d.year == currentYear
            }.sumOf { it.amount }
        }

        val pendingRevenue = active.filter { statusOf(it) != PaymentStatus.Paid }.sumOf { it.monthlyFee }

        val occupiedSeats = active.count { it.seat != null && it.seat != 0 }

        val totalLateFees = active.sumOf { StudyLabLogic.calculateLateFee(it, settings, today) }

        return StudyLabStats(
            totalStudents = active.size,
            paidCount = paidCount,
            unpaidCount = unpaidCount,
            overdueCount = overdueCount,
            totalRevenue = totalRevenue,
            pendingRevenue = pendingRevenue,
            occupiedSeats = occupiedSeats,
            totalLateFees = totalLateFees
        )
    }
}
