package io.flatzen.firebase

import io.flatzen.commoncomponents.commonentities.monetization.MonetizationConfigData
import io.flatzen.commoncomponents.commonentities.more.FaqConfigData
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

class ConfigManagerImpl : ConfigManager, ConfigFieldsChecker {
    override var connectionTimeout: Long = 3

    override fun init() {
        RemoteConfigNativeHandlers.initHandler?.invoke(connectionTimeout)
    }

    override fun fetchAndActivate(): Flow<ConfigResult> = callbackFlow {
        val handler = RemoteConfigNativeHandlers.fetchAndActivateHandler
        if (handler == null) {
            trySend(ConfigResult(false, IllegalStateException("Remote Config is not initialized")))
            close()
            return@callbackFlow
        }
        handler { success ->
            trySend(
                if (success) {
                    ConfigResult(true)
                } else {
                    ConfigResult(false)
                }
            )
            close()
        }
        awaitClose {}
    }

    private fun isRemoteConfigLoaded(): Boolean {
        return RemoteConfigNativeHandlers.isLoadedHandler?.invoke() == true
    }

    override fun checkLong(configField: ConfigFields): Long? {
        val raw =
            RemoteConfigNativeHandlers.getLongHandler?.invoke(configField.param) ?: return null
        val value = raw.toLongOrNull() ?: return null
        return if (value == 0L) null else value
    }

    override fun checkString(configField: ConfigFields): String? {
        val result =
            RemoteConfigNativeHandlers.getStringHandler?.invoke(configField.param) ?: return null
        return result.takeIf { it.isNotEmpty() }
    }

    override fun checkBoolean(configField: ConfigFields): Boolean? {
        return if (isRemoteConfigLoaded()) {
            RemoteConfigNativeHandlers.getBoolHandler?.invoke(configField.param) == "true"
        } else {
            null
        }
    }

    override fun <T> checkJson(configField: ConfigFields): T? {
        val jsonString =
            RemoteConfigNativeHandlers.getStringHandler?.invoke(configField.param).orEmpty()
        if (jsonString.isEmpty()) return null
        return when (configField) {
            ConfigFields.MoreConfigData -> {
                runCatching { Json.decodeFromString<MoreConfigData>(jsonString) as T }.getOrNull()
            }

            ConfigFields.FaqConfigData -> {
                runCatching { Json.decodeFromString<FaqConfigData>(jsonString) as T }.getOrNull()
            }

            ConfigFields.MonetizationConfigData -> {
                runCatching { Json.decodeFromString<MonetizationConfigData>(jsonString) as T }.getOrNull()
            }

            else -> null
        }
    }
}
