package com.libraryx.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors `AdminDoc` in src/lib/saas/types.ts.
 * (Super-admin tier — present in the schema but unused by the current UI,
 * since each authenticated Firebase user self-provisions as its own SubAdmin.)
 */
data class AdminDoc(
    val uid: String = "",
    val email: String = "",
    val role: String = "admin",
    val appDefaultPrice: Double = 0.0,
    val appOverdueThreshold: Int = 0,
    val appLateFee: Double = 0.0,
    val whatsappEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null
)

/**
 * Mirrors `SubAdminDoc` in src/lib/saas/types.ts — the Firestore document
 * representing a lab owner (`subAdmins/{uid}`).
 */
data class SubAdminDoc(
    val uid: String = "",
    val labName: String = "",
    val ownerName: String = "",
    val email: String = "",
    val phone: String = "",
    val adminUid: String = "",
    val masterPasscodeHash: String = "",
    val customPrice: Double? = null,
    val customOverdueThreshold: Int? = null,
    val customLateFee: Double? = null,
    val totalSeats: Int = 60,
    val status: SubAdminStatus = SubAdminStatus.active,
    @ServerTimestamp val createdAt: Date? = null,
    val lastLogin: Date? = null
)

enum class SubAdminStatus { active, inactive, suspended }

/**
 * Mirrors `SaasPayment` in src/lib/saas/types.ts.
 */
data class SaasPayment(
    val month: String = "", // YYYY-MM
    val originalFee: Double = 0.0,
    val lateFee: Double = 0.0,
    val totalFee: Double = 0.0,
    val paid: Boolean = false,
    val paidDate: Date? = null,
    val amount: Double = 0.0,
    val mode: PaymentMode = PaymentMode.Cash,
    val receiptId: String = ""
)

/**
 * Mirrors `SaasStudent` in src/lib/saas/types.ts — Firestore document at `students/{uid}`.
 */
data class SaasStudent(
    val uid: String = "",
    val subAdminUid: String = "",
    val adminUid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val seatNumber: Int? = null,
    val joiningDate: String = "",
    val monthlyFee: Double = 0.0,
    val status: SaasStudentStatus = SaasStudentStatus.active,
    val payments: List<SaasPayment> = emptyList(),
    val currentMonthPaid: Boolean = false,
    val lastPaymentDate: Date? = null,
    val daysOverdue: Int = 0,
    val isLocked: Boolean = false,
    val lockPasscodeEntered: Boolean = false,
    val pinHash: String? = null,
    @ServerTimestamp val createdAt: Date? = null
)

enum class SaasStudentStatus { active, inactive }

/**
 * Mirrors `WhatsappLog` in src/lib/saas/types.ts — Firestore document at `whatsappLogs/{uid}`.
 */
data class WhatsappLog(
    val uid: String = "",
    val subAdminUid: String = "",
    val studentUid: String = "",
    val studentPhone: String = "",
    val studentName: String = "",
    val message: String = "",
    @ServerTimestamp val sentAt: Date? = null,
    val status: WhatsappLogStatus = WhatsappLogStatus.sent,
    val errorMessage: String? = null
)

enum class WhatsappLogStatus { sent, failed, delivered }
