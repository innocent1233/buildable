package com.libraryx.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors the `Payment` interface in src/lib/store.ts.
 */
@Serializable
data class Payment(
    val id: String,
    val date: String, // ISO yyyy-MM-dd, kept as String to match the original JSON shape exactly
    val amount: Double,
    val mode: PaymentMode,
    val notes: String = "",
    val lateFee: Double = 0.0
)

/**
 * Mirrors the payment "mode" union type: "Cash" | "Online" | "Cheque".
 */
@Serializable
enum class PaymentMode {
    Cash, Online, Cheque
}

/**
 * Mirrors the student "status" union type: "Active" | "Inactive".
 */
@Serializable
enum class StudentStatus {
    Active, Inactive
}

/**
 * Mirrors the `Student` interface in src/lib/store.ts.
 *
 * Field `pin` stores the *hashed* PIN (see [com.libraryx.domain.IdGenerator.hashPin]),
 * exactly as in the original TS code — the raw PIN is only ever shown once at creation time.
 */
@Serializable
data class Student(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val monthlyFee: Double,
    val joiningDate: String, // ISO yyyy-MM-dd
    val status: StudentStatus = StudentStatus.Active,
    val seat: Int? = null,
    val pin: String? = null,
    val payments: List<Payment> = emptyList()
)

/**
 * Mirrors `getPaymentStatus`'s return union: "Paid" | "Unpaid" | "Overdue".
 */
enum class PaymentStatus { Paid, Unpaid, Overdue }

/**
 * Mirrors `getMonthPaymentStatus`'s return union, which additionally allows "N/A"
 * for months before a student joined or in the future.
 */
enum class MonthPaymentStatus { Paid, Unpaid, Overdue, NotApplicable }

/**
 * Mirrors the anonymous outstanding-month record returned by `getOutstandingMonths`.
 */
data class OutstandingMonth(
    val month: String,
    val year: Int,
    val monthNum: Int,
    val fee: Double,
    val lateFee: Double
)
