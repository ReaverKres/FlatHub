package io.flatzen.monetization.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    createPreferencesDataStoreWithPath(producePath())

@OptIn(ExperimentalForeignApi::class)
fun createIosPreferencesDataStore(): DataStore<Preferences> {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val path = requireNotNull(dir?.path) + "/flatzen_secure_prefs.preferences_pb"
    return createPreferencesDataStoreWithPath(path)
}
