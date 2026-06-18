package com.example.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

object GitSyncScheduler {
    private const val TAG = "GitSyncScheduler"
    private const val UNIQUE_WORK_NAME = "GitSyncPeriodicWork"

    /**
     * Schedules a periodic Git synchronization background work.
     * Uses NetworkType.CONNECTED to trigger only when a network is available.
     */
    fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 15) {
        Log.d(TAG, "Scheduling periodic background Git sync. Interval: $intervalMinutes minutes.")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Minimum periodic interval for WorkManager is 15 minutes
        val actualInterval = if (intervalMinutes < 15) 15 else intervalMinutes

        val syncRequest = PeriodicWorkRequestBuilder<GitSyncWorker>(
            actualInterval, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // update configuration with new interval or constraints
            syncRequest
        )
    }

    /**
     * Cancels any scheduled periodic Git synchronization backgrounds tasks.
     */
    fun cancelPeriodicSync(context: Context) {
        Log.d(TAG, "Cancelling background periodic Git synchronization tasks.")
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    /**
     * Returns a Flow monitoring the status of the periodic Git synchronization.
     */
    fun observeSyncStatus(context: Context): Flow<Boolean> {
        val workManager = WorkManager.getInstance(context)
        return workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME).asFlow()
            .map { workInfos ->
                if (workInfos.isNullOrEmpty()) {
                    false
                } else {
                    val state = workInfos[0].state
                    state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING
                }
            }
    }
}
