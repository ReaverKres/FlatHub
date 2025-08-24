package io.flatzen.platformtools.background

import entities.CommonFilterRequestModel

/**
 * Cross-platform background work manager interface
 */
expect class BackgroundWorkManager {
    suspend fun schedulePeriodicWork(
        intervalMinutes: Int,
        filterData: CommonFilterRequestModel
    ): Result<Unit>
    
    suspend fun cancelWork()
}

/**
 * Data class representing background work request parameters
 */
data class BackgroundWorkRequest(
    val filterData: CommonFilterRequestModel,
    val intervalMinutes: Int
)