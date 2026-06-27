package com.libraryx

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.libraryx.util.NotificationHelper
import com.libraryx.util.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class annotated with `@HiltAndroidApp` so Hilt can generate its component
 * hierarchy. Initialises the notification channel and periodic WorkManager job on first
 * launch, mirroring the side-effects that ran at React app startup in the original
 * (src/lib/notifications.ts `startNotificationService` was called from StudyLabContext's
 * `useEffect` on settings load).
 *
 * Also implements [Configuration.Provider] so Hilt can supply its own [HiltWorkerFactory]
 * to WorkManager (required for `@HiltWorker` CoroutineWorker injection to work).
 */
@HiltAndroidApp
class StudyLabApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        NotificationScheduler.start(this)
    }
}
