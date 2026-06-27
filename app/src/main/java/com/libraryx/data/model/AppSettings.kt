package com.libraryx.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors `LateFeeSettings` in src/lib/store.ts.
 */
@Serializable
data class LateFeeSettings(
    val enabled: Boolean = false,
    val amount: Double = 50.0,
    val afterDays: Int = 5,
    val compound: Boolean = false
)

/**
 * Mirrors `NotificationSettings.notifyFor` union: "unpaid" | "overdue" | "both".
 */
@Serializable
enum class NotifyFor { unpaid, overdue, both }

/**
 * Mirrors `NotificationSettings` in src/lib/store.ts.
 */
@Serializable
data class NotificationSettings(
    val enabled: Boolean = false,
    val time: String = "09:00", // HH:mm, 24h
    val notifyFor: NotifyFor = NotifyFor.both,
    val sound: Boolean = true
)

/**
 * Mirrors `AppSettings` in src/lib/store.ts.
 */
@Serializable
data class AppSettings(
    val labName: String = "Study Lab",
    val totalSeats: Int = 60,
    val overdueDays: Int = 5,
    val dueDate: Int = 1,
    val lateFee: LateFeeSettings = LateFeeSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val studentPortalEnabled: Boolean = true
) {
    companion object {
        /** Mirrors `defaultSettings` in src/lib/store.ts. */
        val Default = AppSettings()
    }
}

/**
 * Mirrors the `stats` object computed inside StudyLabContext.tsx.
 */
data class StudyLabStats(
    val totalStudents: Int = 0,
    val paidCount: Int = 0,
    val unpaidCount: Int = 0,
    val overdueCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val pendingRevenue: Double = 0.0,
    val occupiedSeats: Int = 0,
    val totalLateFees: Double = 0.0
)

/**
 * Mirrors src/lib/appMode.ts `AppMode` union: "solo" | "saas".
 */
enum class AppMode { Solo, Saas }
