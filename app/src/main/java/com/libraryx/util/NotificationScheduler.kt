package com.libraryx.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Mirrors `startNotificationService`/`stopNotificationService` from src/lib/notifications.ts,
 * using `WorkManager` instead of a JS `setInterval` so the check survives process death and
 * device reboots (once re-enqueued at app start, matching the original's effective behaviour
 * of restarting whenever the app/tab reloads).
 */
object NotificationScheduler {
    private const val WORK_NAME = "studylab_overdue_check"

    fun start(context: Context) {
        val request = PeriodicWorkRequestBuilder<OverdueCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
