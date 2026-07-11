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
    fun initialize(sdkKey: String)
    fun isInitialized(): Boolean
    suspend fun loadBanner(adUnitId: String): AdLoadResult
    suspend fun showInterstitial(adUnitId: String): AdLoadResult
    suspend fun showRewarded(adUnitId: String): AdLoadResult
    fun destroy()
}

/** No-op until AppLovin SDK key is provided. App builds and runs without ads. */
class NoOpAdService : AdService {
    private var initialized = false
    override fun initialize(sdkKey: String) {
        initialized = sdkKey.isNotBlank()
    }

    override fun isInitialized(): Boolean = initialized
    override suspend fun loadBanner(adUnitId: String): AdLoadResult = AdLoadResult.Disabled
    override suspend fun showInterstitial(adUnitId: String): AdLoadResult = AdLoadResult.Disabled
    override suspend fun showRewarded(adUnitId: String): AdLoadResult = AdLoadResult.Disabled
    override fun destroy() = Unit
}
