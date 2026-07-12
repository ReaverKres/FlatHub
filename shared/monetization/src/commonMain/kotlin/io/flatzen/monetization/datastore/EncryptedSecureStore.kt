package io.flatzen.monetization.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.flatzen.monetization.crypto.PlatformCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Preference values are encrypted with [PlatformCipher] before writing to DataStore.
 * DataStore itself is androidx.datastore:datastore-preferences-core (KMP).
 * Official datastore-tink is Android/JVM-only — hence custom encryption layer.
 */
class EncryptedSecureStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: PlatformCipher,
) {
    private val tierKey = stringPreferencesKey("subscription_tier")
    private val expiresKey = longPreferencesKey("subscription_expires_at")
    private val productKey = stringPreferencesKey("subscription_product_id")
    private val trialStartedKey = longPreferencesKey("trial_started_at")
    private val rewardedUntilKey = longPreferencesKey("rewarded_premium_until")

    fun observeSubscriptionCache(): Flow<SubscriptionCache> =
        dataStore.data.map { prefs ->
            SubscriptionCache(
                tier = prefs[tierKey]?.let { decryptString(it) },
                expiresAtEpochMs = prefs[expiresKey],
                productId = prefs[productKey]?.let { decryptString(it) },
            )
        }

    suspend fun saveSubscriptionCache(tier: String?, expiresAtEpochMs: Long?, productId: String?) {
        dataStore.edit { prefs ->
            if (tier == null) prefs.remove(tierKey) else prefs[tierKey] = encryptString(tier)
            if (expiresAtEpochMs == null) prefs.remove(expiresKey) else prefs[expiresKey] =
                expiresAtEpochMs
            if (productId == null) prefs.remove(productKey) else prefs[productKey] =
                encryptString(productId)
        }
    }

    fun observeTrialStartedAt(): Flow<Long?> =
        dataStore.data.map { it[trialStartedKey] }

    suspend fun setTrialStartedAtIfAbsent(epochMs: Long) {
        dataStore.edit { prefs ->
            if (prefs[trialStartedKey] == null) {
                prefs[trialStartedKey] = epochMs
            }
        }
    }

    suspend fun setTrialStartedAt(epochMs: Long) {
        dataStore.edit { it[trialStartedKey] = epochMs }
    }

    fun observeRewardedUntil(): Flow<Long?> =
        dataStore.data.map { it[rewardedUntilKey] }

    suspend fun setRewardedUntil(epochMs: Long?) {
        dataStore.edit { prefs ->
            if (epochMs == null) prefs.remove(rewardedUntilKey) else prefs[rewardedUntilKey] =
                epochMs
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encryptString(value: String): String =
        Base64.encode(cipher.encrypt(value.encodeToByteArray()))

    @OptIn(ExperimentalEncodingApi::class)
    private fun decryptString(value: String): String =
        runCatching {
            cipher.decrypt(Base64.decode(value)).decodeToString()
        }.getOrDefault(value)
}

data class SubscriptionCache(
    val tier: String?,
    val expiresAtEpochMs: Long?,
    val productId: String?,
)
