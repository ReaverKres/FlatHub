package io.flatzen.monetization.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

actual fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    createPreferencesDataStoreWithPath(producePath())

fun createAndroidPreferencesDataStore(context: Context): DataStore<Preferences> {
    val file = context.filesDir.resolve("flatzen_secure_prefs.preferences_pb").absolutePath
    return createPreferencesDataStoreWithPath(file)
}
