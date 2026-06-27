package com.libraryx.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.libraryx.data.mapper.StudentMappers
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.LateFeeSettings
import com.libraryx.data.model.MonthPaymentStatus
import com.libraryx.data.model.NotificationSettings
import com.libraryx.data.model.OutstandingMonth
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.data.model.StudyLabStats
import com.libraryx.domain.StatsCalculator
import com.libraryx.domain.StudyLabLogic
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * SaaS-mode repository — mirrors the `source === "firebase"` branch of
 * StudyLabContext.tsx, scoped to one sub-admin (lab owner). Unlike the singleton
 * [LocalStudyLabRepository], this needs per-session parameters (`subAdminUid`,
 * `adminUid`), so it's created through [Factory] rather than injected directly —
 * the same role `<StudyLabProvider source="firebase" subAdminUid=... adminUid=.../>`
 * plays when mounted by SaasStudyLabBridge.tsx.
 */
class FirebaseStudyLabRepository(
    private val db: FirebaseFirestore,
    private val subAdminUid: String,
    private val adminUid: String
) : StudyLabRepository {

    class Factory @Inject constructor(private val db: FirebaseFirestore) {
        fun create(subAdminUid: String, adminUid: String): FirebaseStudyLabRepository =
            FirebaseStudyLabRepository(db, subAdminUid, adminUid)
    }

    /** Live `students` collection query, mapped via [StudentMappers.saasStudentToSolo]. */
    override val students: Flow<List<Student>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("students")
            .whereEqualTo("subAdminUid", subAdminUid)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val list = snap.documents.mapNotNull { d ->
                        d.toObject(SaasStudent::class.java)?.copy(uid = d.id)
                    }.map(StudentMappers::saasStudentToSolo)
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    /** Live `subAdmins/{uid}` doc — feeds labName/totalSeats/overdueDays/lateFee basics. */
    private val subAdminPatch: Flow<SubAdminPatch> = callbackFlow {
        val registration = db.collection("subAdmins").document(subAdminUid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    trySend(
                        SubAdminPatch(
                            labName = snap.getString("labName"),
                            totalSeats = snap.getLong("totalSeats")?.toInt(),
                            overdueDays = snap.getLong("customOverdueThreshold")?.toInt(),
                            lateFeeAmount = snap.getDouble("customLateFee")
                        )
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    /** Live `subAdmins/{uid}/meta/settings` doc — feeds dueDate/notifications/portal/lateFee details. */
    private val metaPatch: Flow<MetaPatch> = callbackFlow {
        val registration = db.collection("subAdmins").document(subAdminUid)
            .collection("meta").document("settings")
            .addSnapshotListener { snap, err ->
                // permission-denied before the meta doc exists is expected; ignore quietly.
                if (snap != null && snap.exists()) {
                    trySend(
                        MetaPatch(
                            dueDate = snap.getLong("dueDate")?.toInt(),
                            studentPortalEnabled = snap.getBoolean("studentPortalEnabled"),
                            notifEnabled = snap.getBoolean("notifications.enabled"),
                            notifTime = snap.getString("notifications.time"),
                            notifSound = snap.getBoolean("notifications.sound"),
                            lateFeeAfterDays = snap.getLong("lateFee.afterDays")?.toInt(),
                            lateFeeCompound = snap.getBoolean("lateFee.compound")
                        )
                    )
                } else {
                    trySend(MetaPatch())
                }
            }
        awaitClose { registration.remove() }
    }

    override val settings: Flow<AppSettings> = combine(subAdminPatch, metaPatch) { sa, meta ->
        val base = AppSettings.Default
        base.copy(
            labName = sa.labName ?: base.labName,
            totalSeats = sa.totalSeats ?: base.totalSeats,
            overdueDays = sa.overdueDays ?: base.overdueDays,
            dueDate = meta.dueDate ?: base.dueDate,
            studentPortalEnabled = meta.studentPortalEnabled ?: base.studentPortalEnabled,
            lateFee = LateFeeSettings(
                enabled = (sa.lateFeeAmount ?: 0.0) > 0.0,
                amount = sa.lateFeeAmount ?: base.lateFee.amount,
                afterDays = meta.lateFeeAfterDays ?: base.lateFee.afterDays,
                compound = meta.lateFeeCompound ?: base.lateFee.compound
            ),
            notifications = NotificationSettings(
                enabled = meta.notifEnabled ?: base.notifications.enabled,
                time = meta.notifTime ?: base.notifications.time,
                sound = meta.notifSound ?: base.notifications.sound
            )
        )
    }.distinctUntilChanged()

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
        val uid = "stu_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(0, 60466176).toString(36)}"
        val data = mapOf(
            "subAdminUid" to subAdminUid,
            "adminUid" to adminUid,
            "name" to name,
            "phone" to phone,
            "email" to email,
            "seatNumber" to seat,
            "joiningDate" to joiningDate,
            "monthlyFee" to monthlyFee,
            "status" to if (status == StudentStatus.Active) "active" else "inactive",
            "payments" to emptyList<Map<String, Any?>>(),
            "currentMonthPaid" to false,
            "lastPaymentDate" to null,
            "daysOverdue" to 0,
            "isLocked" to false,
            "lockPasscodeEntered" to false,
            "pinHash" to pin,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("students").document(uid).set(data).await()
    }

    override suspend fun updateStudent(id: String, mutate: StudentUpdate.() -> Unit) {
        val update = StudentUpdate().apply(mutate)
        val fields = StudentMappers.soloStudentToSaasFields(
            name = update.name,
            phone = update.phone,
            email = update.email,
            monthlyFee = update.monthlyFee,
            joiningDate = update.joiningDate,
            status = update.status,
            seat = update.seat,
            seatProvided = update.seatProvided,
            pin = update.pin,
            pinProvided = update.pinProvided
        )
        if (fields.isNotEmpty()) db.collection("students").document(id).update(fields).await()
    }

    override suspend fun deleteStudent(id: String) {
        db.collection("students").document(id).delete().await()
    }

    override suspend fun addPayment(studentId: String, date: String, amount: Double, mode: PaymentMode, notes: String, lateFee: Double) {
        val ref = db.collection("students").document(studentId)
        val snap = ref.get().await()
        if (!snap.exists()) return
        val data = snap.toObject(SaasStudent::class.java) ?: return
        val sp = StudentMappers.soloPaymentToSaas(date, amount, mode, lateFee, data.monthlyFee)
        ref.update(
            mapOf(
                "payments" to (data.payments + sp).map(::paymentToMap),
                "currentMonthPaid" to true,
                "lastPaymentDate" to java.util.Date(),
                "daysOverdue" to 0,
                "isLocked" to false
            )
        ).await()
    }

    override suspend fun bulkAddPayment(studentIds: List<String>, date: String, amount: Double, mode: PaymentMode, notes: String) {
        val batch = db.batch()
        for (id in studentIds) {
            val ref = db.collection("students").document(id)
            val snap = ref.get().await()
            if (!snap.exists()) continue
            val data = snap.toObject(SaasStudent::class.java) ?: continue
            val sp = StudentMappers.soloPaymentToSaas(date, amount, mode, 0.0, data.monthlyFee)
            batch.update(
                ref,
                mapOf(
                    "payments" to (data.payments + sp).map(::paymentToMap),
                    "currentMonthPaid" to true,
                    "lastPaymentDate" to java.util.Date(),
                    "daysOverdue" to 0,
                    "isLocked" to false
                )
            )
        }
        batch.commit().await()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        val subAdminPatchMap = mapOf(
            "labName" to settings.labName,
            "totalSeats" to settings.totalSeats,
            "customOverdueThreshold" to settings.overdueDays,
            "customLateFee" to if (settings.lateFee.enabled) settings.lateFee.amount else 0.0
        )
        val metaPatchMap = mapOf(
            "dueDate" to settings.dueDate,
            "notifications" to mapOf(
                "enabled" to settings.notifications.enabled,
                "time" to settings.notifications.time,
                "notifyFor" to settings.notifications.notifyFor.name,
                "sound" to settings.notifications.sound
            ),
            "studentPortalEnabled" to settings.studentPortalEnabled,
            "lateFee" to mapOf(
                "enabled" to settings.lateFee.enabled,
                "amount" to settings.lateFee.amount,
                "afterDays" to settings.lateFee.afterDays,
                "compound" to settings.lateFee.compound
            )
        )
        db.collection("subAdmins").document(subAdminUid).update(subAdminPatchMap).await()
        db.collection("subAdmins").document(subAdminUid).collection("meta").document("settings")
            .set(metaPatchMap, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /** Not supported in firebase mode via this generic API (mirrors the original console.warn). */
    override suspend fun importData(students: List<Student>, settings: AppSettings) {
        // SaaS import flow goes through BackupScreen's own dedicated import (see BackupViewModel),
        // exactly as the original delegates to SaasBackup.tsx instead of StudyLabContext.importData.
    }

    override suspend fun isSeatTaken(seat: Int, excludeId: String?): Boolean {
        val snap = db.collection("students").whereEqualTo("subAdminUid", subAdminUid).get().await()
        return snap.documents.any { d ->
            val s = d.toObject(SaasStudent::class.java)
            s != null && s.seatNumber == seat && d.id != excludeId && s.status.name == "active"
        }
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

    private fun paymentToMap(p: com.libraryx.data.model.SaasPayment) = mapOf(
        "month" to p.month,
        "originalFee" to p.originalFee,
        "lateFee" to p.lateFee,
        "totalFee" to p.totalFee,
        "paid" to p.paid,
        "paidDate" to p.paidDate,
        "amount" to p.amount,
        "mode" to p.mode.name,
        "receiptId" to p.receiptId
    )

    private data class SubAdminPatch(
        val labName: String?,
        val totalSeats: Int?,
        val overdueDays: Int?,
        val lateFeeAmount: Double?
    )

    private data class MetaPatch(
        val dueDate: Int? = null,
        val studentPortalEnabled: Boolean? = null,
        val notifEnabled: Boolean? = null,
        val notifTime: String? = null,
        val notifSound: Boolean? = null,
        val lateFeeAfterDays: Int? = null,
        val lateFeeCompound: Boolean? = null
    )
}
