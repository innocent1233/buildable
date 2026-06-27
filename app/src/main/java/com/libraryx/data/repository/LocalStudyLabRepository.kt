package com.libraryx.data.repository

import com.libraryx.data.local.LocalStudyLabDataSource
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.OutstandingMonth
import com.libraryx.data.model.Payment
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.data.model.StudyLabStats
import com.libraryx.domain.IdGenerator
import com.libraryx.domain.StatsCalculator
import com.libraryx.domain.StudyLabLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Solo-mode repository — mirrors the `source === "local"` branch of StudyLabContext.tsx,
 * which read/wrote `localStorage` via lib/store.ts. Here, [LocalStudyLabDataSource]
 * (DataStore) plays that role, and every mutation re-persists the full list exactly like
 * the original `useEffect(() => saveStudents(students), [students])`.
 */
@Singleton
class LocalStudyLabRepository @Inject constructor(
    private val local: LocalStudyLabDataSource
) : StudyLabRepository {

    override val students: Flow<List<Student>> = local.studentsFlow
    override val settings: Flow<AppSettings> = local.settingsFlow

    override val stats: Flow<StudyLabStats> = combine(students, settings) { stu, settings ->
        StatsCalculator.compute(stu, settings)
    }

    override suspend fun addStudent(
        name: String,
        phone: String,
        email: String,
        monthlyFee: Double,
        joiningDate: String,
        status: StudentStatus,
        seat: Int?,
        pin: String?
    ) {
        val current = local.getStudentsOnce()
        val newStudent = Student(
            id = IdGenerator.generateId(),
            name = name,
            phone = phone,
            email = email,
            monthlyFee = monthlyFee,
            joiningDate = joiningDate,
            status = status,
            seat = seat,
            pin = pin,
            payments = emptyList()
        )
        local.saveStudents(current + newStudent)
    }

    override suspend fun updateStudent(id: String, mutate: StudentUpdate.() -> Unit) {
        val update = StudentUpdate().apply(mutate)
        val current = local.getStudentsOnce()
        val next = current.map { s ->
            if (s.id != id) return@map s
            s.copy(
                name = update.name ?: s.name,
                phone = update.phone ?: s.phone,
                email = update.email ?: s.email,
                monthlyFee = update.monthlyFee ?: s.monthlyFee,
                joiningDate = update.joiningDate ?: s.joiningDate,
                status = update.status ?: s.status,
                seat = if (update.seatProvided) update.seat else s.seat,
                pin = if (update.pinProvided) update.pin else s.pin
            )
        }
        local.saveStudents(next)
    }

    override suspend fun deleteStudent(id: String) {
        val current = local.getStudentsOnce()
        local.saveStudents(current.filterNot { it.id == id })
    }

    override suspend fun addPayment(studentId: String, date: String, amount: Double, mode: PaymentMode, notes: String, lateFee: Double) {
        val current = local.getStudentsOnce()
        val next = current.map { s ->
            if (s.id != studentId) return@map s
            val payment = Payment(id = IdGenerator.generateId(), date = date, amount = amount, mode = mode, notes = notes, lateFee = lateFee)
            s.copy(payments = s.payments + payment)
        }
        local.saveStudents(next)
    }

    override suspend fun bulkAddPayment(studentIds: List<String>, date: String, amount: Double, mode: PaymentMode, notes: String) {
        val current = local.getStudentsOnce()
        val idSet = studentIds.toSet()
        val next = current.map { s ->
            if (s.id !in idSet) return@map s
            val payment = Payment(id = IdGenerator.generateId(), date = date, amount = amount, mode = mode, notes = notes)
            s.copy(payments = s.payments + payment)
        }
        local.saveStudents(next)
    }

    override suspend fun updateSettings(settings: AppSettings) {
        local.saveSettings(settings)
    }

    override suspend fun importData(students: List<Student>, settings: AppSettings) {
        local.saveStudents(students)
        local.saveSettings(settings)
    }

    override suspend fun isSeatTaken(seat: Int, excludeId: String?): Boolean {
        val current = local.getStudentsOnce()
        return current.any { it.seat == seat && it.id != excludeId && it.status == StudentStatus.Active }
    }

    override fun getStatus(student: Student, settings: AppSettings): PaymentStatus =
        StudyLabLogic.getPaymentStatus(student, settings.overdueDays)

    override fun getMonthStatus(student: Student, year: Int, month: Int, settings: AppSettings): MonthPaymentStatus =
        StudyLabLogic.getMonthPaymentStatus(student, year, month, settings.overdueDays)

    override fun getOverdue(student: Student): Int = StudyLabLogic.getDaysOverdue(student)

    override fun getLastPay(student: Student): String? = StudyLabLogic.getLastPaymentDate(student)

    override fun getLateFee(student: Student, settings: AppSettings): Double =
        StudyLabLogic.calculateLateFee(student, settings)

    override fun getOutstanding(student: Student, settings: AppSettings): List<OutstandingMonth> =
        StudyLabLogic.getOutstandingMonths(student, settings)
}
