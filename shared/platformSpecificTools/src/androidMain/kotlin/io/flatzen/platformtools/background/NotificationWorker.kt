package io.flatzen.platformtools.background

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
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
    
    companion object {
        const val CHANNEL_ID = "flatzen_background_work"
        const val NOTIFICATION_ID = 1001
        const val ACTION_CANCEL_NOTIFICATION = "cancel_notification_filter"
    }
    
    private val flatsRepository: MergedRepository by inject()
    private val notificationManager = LocalNotificationManager(context)
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }
    
    private fun createForegroundInfo(): ForegroundInfo {
        // Create notification channel using LocalNotificationManager
        notificationManager.createNotificationChannel(
            channelId = CHANNEL_ID,
            channelName = "Background Work",
            importance = android.app.NotificationManager.IMPORTANCE_LOW,
            description = "Background apartment search notifications"
        )
        
        // Create cancel intent
        val cancelIntent = Intent(context, NotificationCancelReceiver::class.java).apply {
            action = ACTION_CANCEL_NOTIFICATION
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return notificationManager.createForegroundInfo(
            notificationId = NOTIFICATION_ID,
            channelId = CHANNEL_ID,
            title = "Searching for new apartments",
            text = "Background check is running",
            cancelIntent = cancelPendingIntent
        )
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get filter data from input
            val filterData = inputData.keyValueMap[BackgroundWorkManager.FILTER_DATA_KEY] as? CommonFilterRequestModel
                ?: return@withContext Result.failure()
            
            // Get current apartment IDs before fetching
            val currentFlatIds = flatsRepository.getFlatsIds(filterData)
            
            // Fetch new apartments from APIs
            flatsRepository.fetchAndSaveFlats(filterData)
            
            // Get new apartment IDs after fetching
            val newFlatsIds = flatsRepository.getFlatsIds(filterData)
            val newApartments = newFlatsIds.filter { !currentFlatIds.contains(it) }
            
            // If there are new apartments, send notification
            if (newApartments.isNotEmpty()) {
                val newApartmentsCount = newApartments.size
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
                        "total_count" to newFlatsIds.size.toString()
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