package com.libraryx.data.remote.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.SaasPayment
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.SubAdminDoc
import com.libraryx.domain.Passcode
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class AddStudentInput(
    val name: String,
    val phone: String,
    val email: String,
    val seatNumber: Int?,
    val joiningDate: String,
    val monthlyFee: Double
)

data class MarkPaymentInput(
    val month: String, // YYYY-MM
    val fee: Double,
    val lateFee: Double = 0.0,
    val amount: Double,
    val mode: PaymentMode
)

/**
 * Mirrors src/lib/saas/subAdminService.ts. Every function here maps 1:1 to a
 * Firestore call in the original (collection `subAdmins`, collection `students`).
 */
@Singleton
class SubAdminService @Inject constructor(private val db: FirebaseFirestore) {

    private fun subAdmins() = db.collection("subAdmins")
    private fun students() = db.collection("students")

    suspend fun findSubAdminByEmail(email: String): SubAdminDoc? {
        val snap = subAdmins().whereEqualTo("email", email.lowercase().trim()).get().await()
        val doc = snap.documents.firstOrNull() ?: return null
        return doc.toObject(SubAdminDoc::class.java)?.copy(uid = doc.id)
    }

    suspend fun loginSubAdmin(email: String, passcode: String): SubAdminDoc? {
        val sa = findSubAdminByEmail(email) ?: return null
        if (sa.status.name != "active") return null
        if (!Passcode.verifyPasscode(passcode, sa.masterPasscodeHash)) return null
        subAdmins().document(sa.uid).update("lastLogin", FieldValue.serverTimestamp()).await()
        return sa
    }

    suspend fun getSubAdminDoc(uid: String): SubAdminDoc? {
        val snap = subAdmins().document(uid).get().await()
        return if (snap.exists()) snap.toObject(SubAdminDoc::class.java)?.copy(uid = uid) else null
    }

    /**
     * Mirrors `ensureSubAdminFromAuth`: auto-provisions a single-lab owner document for
     * any Firebase-authenticated user the first time they sign in.
     */
    suspend fun ensureSubAdminFromAuth(uid: String, email: String, displayName: String?): SubAdminDoc {
        val ref = subAdmins().document(uid)
        val existing = ref.get().await()
        if (existing.exists()) {
            return existing.toObject(SubAdminDoc::class.java)!!.copy(uid = uid)
        }

        val data = SubAdminDoc(
            uid = uid,
            labName = displayName?.let { "$it's Lab" } ?: "My Library",
            ownerName = displayName ?: email.substringBefore("@").ifBlank { "Owner" },
            email = email.lowercase().trim(),
            phone = "",
            adminUid = uid, // self-owned (no super-admin)
            masterPasscodeHash = "",
            customPrice = null,
            customOverdueThreshold = null,
            customLateFee = null,
            totalSeats = 60
        )
        val firestoreData = mapOf(
            "labName" to data.labName,
            "ownerName" to data.ownerName,
            "email" to data.email,
            "phone" to data.phone,
            "adminUid" to data.adminUid,
            "masterPasscodeHash" to data.masterPasscodeHash,
            "customPrice" to data.customPrice,
            "customOverdueThreshold" to data.customOverdueThreshold,
            "customLateFee" to data.customLateFee,
            "totalSeats" to data.totalSeats,
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "lastLogin" to FieldValue.serverTimestamp()
        )
        ref.set(firestoreData).await()
        return data
    }

    suspend fun updateSubAdminSettings(
        uid: String,
        customPrice: Double? = null,
        customOverdueThreshold: Int? = null,
        customLateFee: Double? = null,
        labName: String? = null,
        ownerName: String? = null,
        phone: String? = null
    ) {
        val patch = mutableMapOf<String, Any?>()
        customPrice?.let { patch["customPrice"] = it }
        customOverdueThreshold?.let { patch["customOverdueThreshold"] = it }
        customLateFee?.let { patch["customLateFee"] = it }
        labName?.let { patch["labName"] = it }
        ownerName?.let { patch["ownerName"] = it }
        phone?.let { patch["phone"] = it }
        if (patch.isNotEmpty()) subAdmins().document(uid).update(patch).await()
    }

    suspend fun listStudentsForSubAdmin(subAdminUid: String): List<SaasStudent> {
        val snap = students().whereEqualTo("subAdminUid", subAdminUid).get().await()
        return snap.documents.mapNotNull { it.toObject(SaasStudent::class.java)?.copy(uid = it.id) }
    }

    suspend fun addStudent(subAdminUid: String, adminUid: String, input: AddStudentInput): String {
        val uid = "stu_${System.currentTimeMillis()}_${Random.nextInt(0, 60466176).toString(36)}"
        val data = mapOf(
            "subAdminUid" to subAdminUid,
            "adminUid" to adminUid,
            "name" to input.name.trim(),
            "phone" to input.phone.trim(),
            "email" to input.email.trim(),
            "seatNumber" to input.seatNumber,
            "joiningDate" to input.joiningDate,
            "monthlyFee" to input.monthlyFee,
            "status" to "active",
            "payments" to emptyList<Map<String, Any?>>(),
            "currentMonthPaid" to false,
            "lastPaymentDate" to null,
            "daysOverdue" to 0,
            "isLocked" to false,
            "lockPasscodeEntered" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        students().document(uid).set(data).await()
        return uid
    }

    suspend fun updateStudent(uid: String, patch: Map<String, Any?>) {
        if (patch.isNotEmpty()) students().document(uid).update(patch).await()
    }

    suspend fun deleteStudent(uid: String) {
        students().document(uid).delete().await()
    }

    suspend fun markPayment(studentUid: String, input: MarkPaymentInput) {
        val ref = students().document(studentUid)
        val snap = ref.get().await()
        if (!snap.exists()) throw IllegalStateException("Student not found")
        val existing = snap.toObject(SaasStudent::class.java) ?: SaasStudent()
        val newPayment = SaasPayment(
            month = input.month,
            originalFee = input.fee,
            lateFee = input.lateFee,
            totalFee = input.fee + input.lateFee,
            paid = true,
            paidDate = Date(),
            amount = input.amount,
            mode = input.mode,
            receiptId = "REC_${System.currentTimeMillis()}"
        )
        val payments = existing.payments + newPayment
        ref.update(
            mapOf(
                "payments" to payments.map {
                    mapOf(
                        "month" to it.month,
                        "originalFee" to it.originalFee,
                        "lateFee" to it.lateFee,
                        "totalFee" to it.totalFee,
                        "paid" to it.paid,
                        "paidDate" to it.paidDate,
                        "amount" to it.amount,
                        "mode" to it.mode.name,
                        "receiptId" to it.receiptId
                    )
                },
                "currentMonthPaid" to true,
                "lastPaymentDate" to Date(),
                "daysOverdue" to 0,
                "isLocked" to false,
                "lockPasscodeEntered" to false
            )
        ).await()
    }
}
