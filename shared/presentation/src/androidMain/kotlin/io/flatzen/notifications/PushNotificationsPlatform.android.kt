package io.flatzen.notifications

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val PUSH_PREFS = "flatzen_push_notifications"
private const val KEY_FCM_TOKEN = "fcm_token"

class AndroidPushNotificationsPlatform(
    private val context: Context,
) : PushNotificationsPlatform {

    override fun requestToken(onResult: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                val token = if (task.isSuccessful) task.result else null
                token?.let { saveToken(it) }
                onResult(token)
            }
    }

    override fun getLastKnownToken(): String? {
        return context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    override suspend fun deleteToken() {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener {
                    context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
                        .edit { remove(KEY_FCM_TOKEN) }
                    if (cont.isActive) cont.resume(Unit)
                }
        }
    }

    private fun saveToken(token: String) {
        context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_FCM_TOKEN, token) }
    }
}

internal fun saveFcmToken(context: Context, token: String) {
    context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
        .edit { putString(KEY_FCM_TOKEN, token) }
}
