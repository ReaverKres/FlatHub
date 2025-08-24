package io.flatzen.platformtools.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import entities.CommonFilterRequestModel
import io.flatzen.platformtools.notifications.LocalNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.mergedrepo.MergedRepository

/**
 * WorkManager worker that performs background apartment checking
 * and sends notifications when new apartments are found
 */
class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    
    private val flatsRepository: MergedRepository by inject()
    private val notificationManager = LocalNotificationManager(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get filter data from input
            val filterDataJson = inputData.keyValueMap
                ?: return@withContext Result.failure()

            val filterData = filterDataJson[BackgroundWorkManager.FILTER_DATA_KEY] as CommonFilterRequestModel

            // Get current count of apartments in database matching the filter
            val currentCount = flatsRepository.getFlatsCount(filterData)
            
            // Fetch new apartments from APIs
            flatsRepository.fetchAndSaveFlats(filterData)
            
            // Get new count after fetching
            val newCount = flatsRepository.getFlatsCount(filterData)
            
            // If there are new apartments, send notification
            if (newCount > currentCount) {
                val newApartmentsCount = newCount - currentCount
                val title = "New apartments found!"
                val body = if (newApartmentsCount == 1) {
                    "1 new apartment matches your filter"
                } else {
                    "$newApartmentsCount new apartments match your filter"
                }
                
                notificationManager.showNotification(
                    id = "new_apartments",
                    title = title,
                    body = body,
                    data = mapOf(
                        "new_count" to newApartmentsCount.toString(),
                        "total_count" to newCount.toString()
                    )
                ).getOrElse { 
                    // If notification fails, still return success for work completion
                    return@withContext Result.success()
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            // Log error and return retry for transient failures
            Result.retry()
        }
    }
}