package io.flatzen.firebase

import kotlinx.coroutines.flow.Flow

class ConfigResult(val isSuccess: Boolean, val exception: Exception? = null)

interface ConfigManager {
    var connectionTimeout: Long
    fun init()
    fun fetchAndActivate(): Flow<ConfigResult>
}

interface ConfigFieldsChecker {
    fun checkLong(configField: ConfigFields): Long?
    fun checkString(configField: ConfigFields): String?
    fun checkBoolean(configField: ConfigFields): Boolean?
    fun <T> checkJson(configField: ConfigFields): T?
}