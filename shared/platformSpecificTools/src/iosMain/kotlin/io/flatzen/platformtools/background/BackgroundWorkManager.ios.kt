package io.flatzen.platformtools.background

import entities.CommonFilterRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.BackgroundTasks.*
import platform.Foundation.NSError
import platform.Foundation.NSUserDefaults
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * iOS implementation of BackgroundWorkManager using BGTaskScheduler
 * Note: iOS has strict limitations on background execution time (typically 30 seconds)
 */
actual class BackgroundWorkManager {
    
    companion object {
        const val BACKGROUND_TASK_ID = "io.flatzen.apartment-check"
        const val FILTER_DATA_KEY = "notification_filter_data"
        const val INTERVAL_KEY = "notification_interval"
    }
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val scheduler = BGTaskScheduler.sharedScheduler
    
    actual suspend fun schedulePeriodicWork(
        intervalMinutes: Int,
        filterData: CommonFilterRequestModel
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Store filter data in UserDefaults
            val filterJson = Json.encodeToString(filterData)
            userDefaults.setObject(filterJson, FILTER_DATA_KEY)
            userDefaults.setInteger(intervalMinutes.toLong(), INTERVAL_KEY)
            userDefaults.synchronize()
            
            // Cancel existing background task
            scheduler.cancelAllTaskRequests()
            
            // Create background app refresh request
            val request = BGAppRefreshTaskRequest(BACKGROUND_TASK_ID).apply {
                earliestBeginDate = platform.Foundation.NSDate().dateByAddingTimeInterval(
                    intervalMinutes * 60.0 // Convert minutes to seconds
                )
            }
            
            // Submit the request
            val error = scheduler.submitTaskRequest(request, null)
            if (error != null) {
                Result.failure(Exception("Failed to schedule background task: ${error.localizedDescription}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    actual suspend fun cancelWork() {
        withContext(Dispatchers.Main) {
            scheduler.cancelAllTaskRequests()
            userDefaults.removeObjectForKey(FILTER_DATA_KEY)
            userDefaults.removeObjectForKey(INTERVAL_KEY)
            userDefaults.synchronize()
        }
    }
    
    /**
     * Register background task handler - should be called during app startup
     */
    fun registerBackgroundTaskHandler() {
        scheduler.registerForTaskWithIdentifier(
            identifier = BACKGROUND_TASK_ID,
            usingQueue = null
        ) { task ->
            handleBackgroundTask(task as BGAppRefreshTask)
        }
    }
    
    private fun handleBackgroundTask(task: BGAppRefreshTask) {
        // Set expiration handler
        task.expirationHandler = {
            task.setTaskCompletedWithSuccess(false)
        }
        
        // Perform background work
        performBackgroundWork { success ->
            task.setTaskCompletedWithSuccess(success)
            
            // Schedule next execution if successful
            if (success) {
                val interval = userDefaults.integerForKey(INTERVAL_KEY)
                if (interval > 0) {
                    val filterJson = userDefaults.stringForKey(FILTER_DATA_KEY) as? String
                    if (filterJson != null) {
                        try {
                            val filterData = Json.decodeFromString<CommonFilterRequestModel>(filterJson)
                            // Note: This is a simplified approach - in production you might want
                            // to handle this scheduling more carefully
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            }
        }
    }
    
    private fun performBackgroundWork(completion: (Boolean) -> Unit) {
        // This is where you would implement the actual apartment checking logic
        // Due to iOS background execution time limits, this should be optimized
        // for quick execution (< 30 seconds typically)
        
        // For now, we'll just mark as completed
        // In a real implementation, you would:
        // 1. Get filter data from UserDefaults
        // 2. Make API calls to check for new apartments
        // 3. Compare with stored data
        // 4. Send local notification if new apartments found
        
        completion(true)
    }
}