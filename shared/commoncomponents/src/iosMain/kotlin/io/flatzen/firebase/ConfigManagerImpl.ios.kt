package io.flatzen.firebase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ConfigManagerImpl : ConfigManager, ConfigFieldsChecker {
    override var connectionTimeout: Long = 3

    override fun init() {
        // iOS stub implementation
        println("iOS ConfigManagerImpl.init() called")
    }

    override fun fetchAndActivate(): Flow<ConfigResult> {
        // iOS stub implementation - simulate successful fetch
        return flowOf(ConfigResult(true))
    }

    override fun checkLong(configField: ConfigFields): Long? {
        // iOS stub implementation - return default value for remoteConfigVersion
        return null
    }

    override fun checkString(configField: ConfigFields): String? {
        // iOS stub implementation
        return null
    }

    override fun checkBoolean(configField: ConfigFields): Boolean {
        // iOS stub implementation
        return when(configField) {
            ConfigFields.FreeVersionAvailable -> true
            else -> true
        }
    }

    override fun <T> checkJson(configField: ConfigFields): T? {
        return null
    }
}