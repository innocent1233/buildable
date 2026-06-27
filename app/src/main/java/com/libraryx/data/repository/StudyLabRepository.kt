package com.libraryx.data.repository

import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.OutstandingMonth
import com.libraryx.data.model.Payment
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudyLabStats
import kotlinx.coroutines.flow.Flow

/**
 * Mirrors `StudyLabContextType` in src/context/StudyLabContext.tsx.
 *
 * Two implementations exist, exactly mirroring the original `source: "local" | "firebase"`
 * prop on `<StudyLabProvider>`:
 *  - [LocalStudyLabRepository] — Solo mode, backed by DataStore (was localStorage).
 *  - [FirebaseStudyLabRepository] — SaaS mode, backed by live Firestore snapshot listeners,
 *    scoped to one sub-admin (lab owner), exactly like `StudyLabProvider source="firebase"`.
 */
interface StudyLabRepository {

    val students: Flow<List<Student>>
    val settings: Flow<AppSettings>
    val stats: Flow<StudyLabStats>

    suspend fun addStudent(
        name: String,
        phone: String,
        email: String,
        monthlyFee: Double,
        joiningDate: String,
        status: com.libraryx.data.model.StudentStatus,
        seat: Int?,
        pin: String?
    )

    suspend fun updateStudent(id: String, mutate: StudentUpdate.() -> Unit)

    suspend fun deleteStudent(id: String)

    suspend fun addPayment(studentId: String, date: String, amount: Double, mode: com.libraryx.data.model.PaymentMode, notes: String, lateFee: Double)

    suspend fun bulkAddPayment(studentIds: List<String>, date: String, amount: Double, mode: com.libraryx.data.model.PaymentMode, notes: String)

    suspend fun updateSettings(settings: AppSettings)

    suspend fun importData(students: List<Student>, settings: AppSettings)

    suspend fun isSeatTaken(seat: Int, excludeId: String? = null): Boolean

    fun getStatus(student: Student, settings: AppSettings): PaymentStatus

    fun getMonthStatus(student: Student, year: Int, month: Int, settings: AppSettings): MonthPaymentStatus

    fun getOverdue(student: Student): Int

    fun getLastPay(student: Student): String?

    fun getLateFee(student: Student, settings: AppSettings): Double

    fun getOutstanding(student: Student, settings: AppSettings): List<OutstandingMonth>
}

/** Mutable partial-update holder, mirroring `Partial<Student>` in `updateStudent`. */
class StudentUpdate {
    var name: String? = null
    var phone: String? = null
    var email: String? = null
    var monthlyFee: Double? = null
    var joiningDate: String? = null
    var status: com.libraryx.data.model.StudentStatus? = null
    var seat: Int? = null
    var seatProvided: Boolean = false
    var pin: String? = null
    var pinProvided: Boolean = false

    fun seat(value: Int?) { seat = value; seatProvided = true }
    fun pin(value: String?) { pin = value; pinProvided = true }
}
