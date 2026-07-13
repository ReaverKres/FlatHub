package io.flatzen.monetization.ads

enum class AdPlacement {
    HOME_FEED,
    SWIPE_INTERSTITIAL,
    REWARDED_PREMIUM,
}

sealed interface AdLoadResult {
    data object Ready : AdLoadResult
    data object NoFill : AdLoadResult
    data class Error(val message: String) : AdLoadResult
    data object Disabled : AdLoadResult
}

interface AdService {
    fun initialize(androidAppKey: String, iosAppKey: String)
    fun isInitialized(): Boolean
    suspend fun showRewarded(placement: String): AdLoadResult
    suspend fun prefetchNative(placement: String, count: Int = 1): AdLoadResult
    suspend fun prefetchMrec(placement: String): AdLoadResult
    fun destroy()
}

/** No-op until Appodeal app key is provided. App builds and runs without ads. */
class NoOpAdService : AdService {
    private var initialized = false

    override fun initialize(androidAppKey: String, iosAppKey: String) {
        initialized = androidAppKey.isNotBlank() || iosAppKey.isNotBlank()
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun showRewarded(placement: String): AdLoadResult = AdLoadResult.Disabled

    override suspend fun prefetchNative(placement: String, count: Int): AdLoadResult = AdLoadResult.Disabled

    override suspend fun prefetchMrec(placement: String): AdLoadResult = AdLoadResult.Disabled

    override fun destroy() = Unit
}
