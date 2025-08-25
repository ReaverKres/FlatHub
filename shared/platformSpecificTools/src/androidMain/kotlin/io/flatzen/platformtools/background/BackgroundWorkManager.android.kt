package io.flatzen.platformtools.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import entities.CommonFilterRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            
            // Create input data - serialize CommonFilterRequestModel to JSON string
            val filterDataJson = Json.encodeToString(filterData)
            val inputData = workDataOf(
                FILTER_DATA_KEY to filterDataJson,
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
                // Add expedited work for immediate execution and foreground service
//                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            // Enqueue unique work
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
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