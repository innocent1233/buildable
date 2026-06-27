package com.libraryx.data.remote.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.WhatsappLog
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors src/lib/saas/whatsappService.ts. URL building moved to
 * [com.libraryx.util.WhatsAppLauncher] since on Android we launch a real Intent
 * instead of `window.open`; logging to Firestore stays here.
 */
@Singleton
class WhatsappService @Inject constructor(private val db: FirebaseFirestore) {

    suspend fun logWhatsappSent(
        subAdminUid: String,
        student: SaasStudent,
        message: String,
        status: String = "sent",
        errorMessage: String? = null
    ) {
        db.collection("whatsappLogs").add(
            mapOf(
                "subAdminUid" to subAdminUid,
                "studentUid" to student.uid,
                "studentPhone" to student.phone,
                "studentName" to student.name,
                "message" to message,
                "sentAt" to FieldValue.serverTimestamp(),
                "status" to status,
                "errorMessage" to errorMessage
            )
        ).await()
    }

    suspend fun listRecentLogs(subAdminUid: String, max: Long = 50): List<WhatsappLog> {
        val snap = db.collection("whatsappLogs")
            .whereEqualTo("subAdminUid", subAdminUid)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(max)
            .get()
            .await()
        return snap.documents.mapNotNull { it.toObject(WhatsappLog::class.java)?.copy(uid = it.id) }
    }
}
