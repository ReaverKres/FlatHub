package io.flatzen.platformtools.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import entities.CommonFilterRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Android implementation of BackgroundWorkManager using WorkManager
 */
actual class BackgroundWorkManager(private val context: Context) {
    
    companion object {
        const val WORK_NAME = "FlatZenNotificationWork"
        const val FILTER_DATA_KEY = "filter_data"
        const val INTERVAL_KEY = "interval_minutes"
    }
    
    actual suspend fun schedulePeriodicWork(
        intervalMinutes: Int,
        filterData: CommonFilterRequestModel
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val workManager = WorkManager.getInstance(context)
            
            // Cancel existing work
            workManager.cancelUniqueWork(WORK_NAME)
            
            // Create input data
            val inputData = workDataOf(
                FILTER_DATA_KEY to filterData,
                INTERVAL_KEY to intervalMinutes
            )
            
            // Create periodic work request with minimum 15-minute interval
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                intervalMinutes.toLong().coerceAtLeast(15),
                TimeUnit.MINUTES
            )
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            // Enqueue unique work
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    actual suspend fun cancelWork() {
        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}