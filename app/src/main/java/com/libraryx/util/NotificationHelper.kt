package com.libraryx.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.libraryx.R
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.PaymentStatus
import com.libraryx.data.model.Student
import com.libraryx.data.model.StudentStatus
import com.libraryx.domain.StudyLabLogic
import java.text.NumberFormat
import java.util.Locale

/**
 * Replaces `requestNotificationPermission` / `getNotificationPermission` / `sendNotification` /
 * `sendOverdueSummaryNotification` from src/lib/notifications.ts. The browser's Notification
 * API maps to `NotificationCompat` + the runtime `POST_NOTIFICATIONS` permission (API 33+).
 */
object NotificationHelper {

    const val CHANNEL_ID = "studylab_payment_alerts"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Payment Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of unpaid and overdue student fees"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /** Mirrors `getNotificationPermission`. */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Mirrors `sendNotification`. Actual runtime permission request happens in the UI layer (Compose). */
    fun sendNotification(context: Context, title: String, body: String, sound: Boolean) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { if (!sound) setSilent(true) }
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Mirrors `sendOverdueSummaryNotification`. */
    fun sendOverdueSummaryNotification(context: Context, students: List<Student>, settings: AppSettings) {
        val active = students.filter { it.status == StudentStatus.Active }
        val overdue = active.filter { StudyLabLogic.getPaymentStatus(it, settings.overdueDays) == PaymentStatus.Overdue }
        val unpaid = active.filter { StudyLabLogic.getPaymentStatus(it, settings.overdueDays) == PaymentStatus.Unpaid }
        if (overdue.isEmpty() && unpaid.isEmpty()) return

        val totalPending = (overdue + unpaid).sumOf { it.monthlyFee }
        val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalPending.toInt())
        val body = "${overdue.size} overdue, ${unpaid.size} unpaid. \u20B9$formatted pending."
        sendNotification(context, "Study Lab - Payment Alert", body, settings.notifications.sound)
    }
}
