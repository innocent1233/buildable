package com.libraryx.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.libraryx.data.repository.StudyLabRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Mirrors the polling body inside `startNotificationService` from src/lib/notifications.ts:
 * "every interval, if the current hour:minute matches the configured notification time,
 * send the overdue/unpaid summary." WorkManager's minimum periodic interval is 15 minutes
 * (vs. the original's 5-minute `setInterval`), so the match window below is widened to
 * the first 15 minutes of the target hour to guarantee exactly one notification per day.
 */
@HiltWorker
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: StudyLabRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = repository.settings.first()
        if (!settings.notifications.enabled) return Result.success()
        if (settings.notifications.notifyFor == com.libraryx.data.model.NotifyFor.unpaid) return Result.success()

        val (targetHour, _) = settings.notifications.time.split(":").let {
            (it.getOrNull(0)?.toIntOrNull() ?: 9) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        val now = java.time.LocalDateTime.now()
        if (now.hour != targetHour || now.minute >= 15) return Result.success()

        val students = repository.students.first()
        NotificationHelper.sendOverdueSummaryNotification(applicationContext, students, settings)
        return Result.success()
    }
}
