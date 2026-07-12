package io.flatzen.firebase

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.DEFAULT_VALUE_FOR_LONG
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import io.flatzen.commoncomponents.commonentities.monetization.MonetizationConfigData
import io.flatzen.commoncomponents.commonentities.more.FaqConfigData
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

class ConfigManagerImpl : ConfigManager, ConfigFieldsChecker {
    override var connectionTimeout: Long = 3

    private var remoteConfig: FirebaseRemoteConfig? = null

    override fun init() {
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
            fetchTimeoutInSeconds = connectionTimeout
        }
        remoteConfig?.setConfigSettingsAsync(configSettings)
    }

    override fun fetchAndActivate(): Flow<ConfigResult> = callbackFlow {
        val remoteConfigInstance = remoteConfig
        if (remoteConfigInstance == null) {
            trySend(
                ConfigResult(
                    false,
                    IllegalStateException("Remote Config is not initialized")
                )
            )
            return@callbackFlow
        }

       remoteConfigInstance.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(ConfigResult(true))
                } else {
                    trySend(ConfigResult(false))
                }
                close()
            }
            .addOnFailureListener { exception ->
                trySend(ConfigResult(false, exception))
            }
        awaitClose {}
    }

    fun isRemoteConfigLoaded(): Boolean {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configInfo = remoteConfig.info

        return when (configInfo.lastFetchStatus) {
            FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS -> true
            FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET -> false
            FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE -> false
            FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED -> true
            else -> false
        }
    }

    override fun checkLong(configField: ConfigFields): Long? {
        val result = FirebaseRemoteConfig.getInstance().getLong(configField.param)
        return if (result == DEFAULT_VALUE_FOR_LONG) null else result
    }

    override fun checkString(configField: ConfigFields): String? {
        val result = FirebaseRemoteConfig.getInstance().getString(configField.param)
        return if (result == DEFAULT_VALUE_FOR_STRING) null else result
    }

    override fun checkBoolean(configField: ConfigFields): Boolean? {
        return if (isRemoteConfigLoaded()) {
            FirebaseRemoteConfig.getInstance().getBoolean(configField.param)
        } else null
    }

    override fun <T> checkJson(configField: ConfigFields): T? {
        val jsonString = FirebaseRemoteConfig.getInstance().getString(configField.param)
        return when (configField) {
            ConfigFields.MoreConfigData -> {
                try {
                    Json.decodeFromString<MoreConfigData>(jsonString) as T
                } catch (e: Exception) {
                    print("MoreConfigData parsing exception\n ${e.localizedMessage}")
                    null
                }
            }
            ConfigFields.FaqConfigData -> {
                try {
                    Json.decodeFromString<FaqConfigData>(jsonString) as T
                } catch (e: Exception) {
                    print("FaqConfigData parsing exception\n ${e.localizedMessage}")
                    null
                }
            }

            ConfigFields.MonetizationConfigData -> {
                try {
                    Json.decodeFromString<MonetizationConfigData>(jsonString) as T
                } catch (e: Exception) {
                    print("MonetizationConfigData parsing exception\n ${e.localizedMessage}")
                    null
                }
            }
            else -> null
        }
    }
}