package io.flatzen.platformtools.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.fillter.FilterRepository

/**
 * BroadcastReceiver for handling notification cancel action
 */
class NotificationCancelReceiver : BroadcastReceiver(), KoinComponent {
    
    private val filterRepository: FilterRepository by inject()
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationWorker.ACTION_CANCEL_NOTIFICATION) {
            // Cancel WorkManager work
            WorkManager.getInstance(context).cancelUniqueWork(BackgroundWorkManager.WORK_NAME)
            
            // Delete notification filter from database
            CoroutineScope(Dispatchers.IO).launch {
                filterRepository.deleteNotificationFilter()
            }
        }
    }
}