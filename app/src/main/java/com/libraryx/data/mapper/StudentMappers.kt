package com.libraryx.data.mapper

import com.libraryx.data.model.Payment
import com.libraryx.data.model.PaymentMode
import com.libraryx.data.model.SaasPayment
import com.libraryx.data.model.SaasStudent
import com.libraryx.data.model.SaasStudentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.random.Random

/**
 * Mirrors src/lib/saas/mappers.ts: converts between Firestore's `SaasStudent`/`SaasPayment`
 * shape and the plain `Student`/`Payment` shape that all UI screens consume — exactly the
 * same "adapter" role the original mappers play between Firebase mode and the shared
 * Solo UI components.
 */
object StudentMappers {

    private fun Date?.toIso(): String =
        (this ?: Date()).toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    /** Mirrors `saasStudentToSolo`. */
    fun saasStudentToSolo(s: SaasStudent): Student {
        val payments: List<Payment> = s.payments.map { p ->
            val isoDate = p.paidDate.toIso()
            Payment(
                id = p.receiptId.ifBlank { "${p.month}-$isoDate" },
                date = isoDate,
                amount = p.amount,
                mode = p.mode,
                notes = "Receipt ${p.receiptId} (${p.month})".trim(),
                lateFee = p.lateFee
            )
        }
        return Student(
            id = s.uid,
            name = s.name,
            phone = s.phone,
            email = s.email,
            monthlyFee = s.monthlyFee,
            joiningDate = s.joiningDate,
            status = if (s.status == SaasStudentStatus.active) StudentStatus.Active else StudentStatus.Inactive,
            seat = s.seatNumber,
            pin = s.pinHash,
            payments = payments
        )
    }

    /** Mirrors `soloPaymentToSaas`: used when a sub-admin records a payment via the bridged UI. */
    fun soloPaymentToSaas(date: String, amount: Double, mode: PaymentMode, lateFee: Double, monthlyFee: Double): SaasPayment {
        val parsed = runCatching { Instant.parse(date) }.getOrNull()
            ?: runCatching { java.time.LocalDate.parse(date.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrElse { Instant.now() }
        val zoned = parsed.atZone(ZoneOffset.UTC)
        val month = "%04d-%02d".format(zoned.year, zoned.monthValue)
        return SaasPayment(
            month = month,
            originalFee = monthlyFee,
            lateFee = lateFee,
            totalFee = monthlyFee + lateFee,
            paid = true,
            paidDate = Date.from(parsed),
            amount = amount,
            mode = mode,
            receiptId = "REC_${System.currentTimeMillis()}_${Random.nextInt(0, 46656).toString(36)}"
        )
    }

    /** Mirrors `soloStudentToSaasFields`: partial-update field map for Firestore `updateDoc`. */
    fun soloStudentToSaasFields(
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        monthlyFee: Double? = null,
        joiningDate: String? = null,
        status: StudentStatus? = null,
        seat: Int? = null,
        seatProvided: Boolean = false,
        pin: String? = null,
        pinProvided: Boolean = false
    ): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        name?.let { out["name"] = it }
        phone?.let { out["phone"] = it }
        email?.let { out["email"] = it }
        monthlyFee?.let { out["monthlyFee"] = it }
        joiningDate?.let { out["joiningDate"] = it }
        status?.let { out["status"] = if (it == StudentStatus.Active) "active" else "inactive" }
        if (seatProvided) out["seatNumber"] = seat
        if (pinProvided) out["pinHash"] = pin
        return out
    }
}
