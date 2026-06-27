package com.libraryx.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.SubAdminDoc
import com.libraryx.domain.Passcode
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors src/lib/saas/membershipService.ts: membership-lock checks tied to the
 * sub-admin's master passcode, used by MembershipLockScreen.
 */
@Singleton
class MembershipService @Inject constructor(private val db: FirebaseFirestore) {

    suspend fun isStudentLocked(studentUid: String): Boolean {
        val snap = db.collection("students").document(studentUid).get().await()
        return if (snap.exists()) snap.toObject(SaasStudent::class.java)?.isLocked ?: false else false
    }

    suspend fun verifySubAdminPasscode(subAdminUid: String, enteredPasscode: String): Boolean {
        val snap = db.collection("subAdmins").document(subAdminUid).get().await()
        if (!snap.exists()) return false
        val sa = snap.toObject(SubAdminDoc::class.java) ?: return false
        return Passcode.verifyPasscode(enteredPasscode, sa.masterPasscodeHash)
    }

    suspend fun unlockStudent(studentUid: String) {
        db.collection("students").document(studentUid)
            .update(mapOf("isLocked" to false, "lockPasscodeEntered" to true))
            .await()
    }

    /**
     * Mirrors `checkAndLockExpired`: scans unpaid students for an admin and locks any
     * whose last payment (or joining date, if never paid) is older than [overdueThreshold] days.
     */
    suspend fun checkAndLockExpired(adminUid: String, overdueThreshold: Int = 5): Int {
        val snap = db.collection("students")
            .whereEqualTo("adminUid", adminUid)
            .whereEqualTo("currentMonthPaid", false)
            .get()
            .await()
        val now = System.currentTimeMillis()
        var locked = 0
        for (doc in snap.documents) {
            val s = doc.toObject(SaasStudent::class.java) ?: continue
            val lastPaidMs = s.lastPaymentDate?.time
                ?: runCatching { java.time.LocalDate.parse(s.joiningDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }
                    .getOrDefault(now)
            val daysLate = (now - lastPaidMs) / (1000L * 60 * 60 * 24)
            if (daysLate > overdueThreshold) {
                doc.reference.update(mapOf("isLocked" to true, "daysOverdue" to daysLate.toInt())).await()
                locked++
            }
        }
        return locked
    }
}
