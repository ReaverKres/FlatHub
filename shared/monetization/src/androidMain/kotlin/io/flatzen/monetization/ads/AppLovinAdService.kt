package io.flatzen.monetization.ads

import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import io.flatzen.monetization.billing.CurrentActivityHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * AppLovin MAX — Android. Requires Activity for interstitial/rewarded.
 * Safe when ad unit / SDK key empty: methods return [AdLoadResult.Disabled].
 */
class AppLovinAdService(
    private val context: Context,
) : AdService {

    @Volatile
    private var initialized = false
    private var interstitial: MaxInterstitialAd? = null
    private var rewarded: MaxRewardedAd? = null

    override fun initialize(sdkKey: String) {
        if (sdkKey.isBlank() || initialized) return
        val initConfig = AppLovinSdkInitializationConfiguration.builder(sdkKey)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        AppLovinSdk.getInstance(context).initialize(initConfig) {
            initialized = true
        }
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun loadBanner(adUnitId: String): AdLoadResult {
        if (!initialized || adUnitId.isBlank()) return AdLoadResult.Disabled
        // Banner is rendered via platform view in UI; loading success is deferred to composable.
        return AdLoadResult.Ready
    }

    override suspend fun showInterstitial(adUnitId: String): AdLoadResult {
        if (!initialized || adUnitId.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.Error("No activity")
        return suspendCancellableCoroutine { cont ->
            val ad = MaxInterstitialAd(adUnitId, activity)
            interstitial = ad
            ad.setListener(object : MaxAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    if (interstitial?.isReady == true) {
                        interstitial?.showAd()
                    }
                }

                override fun onAdDisplayed(ad: MaxAd) = Unit
                override fun onAdHidden(ad: MaxAd) {
                    if (cont.isActive) cont.resume(AdLoadResult.Ready)
                }

                override fun onAdClicked(ad: MaxAd) = Unit
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    if (cont.isActive) cont.resume(AdLoadResult.NoFill)
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    if (cont.isActive) cont.resume(AdLoadResult.Error(error.message))
                }
            })
            ad.loadAd()
        }
    }

    override suspend fun showRewarded(adUnitId: String): AdLoadResult {
        if (!initialized || adUnitId.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.Error("No activity")
        return suspendCancellableCoroutine { cont ->
            var rewardedGranted = false
            val ad = MaxRewardedAd.getInstance(adUnitId, activity)
            rewarded = ad
            ad.setListener(object : MaxRewardedAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    if (rewarded?.isReady == true) rewarded?.showAd()
                }

                override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
                    rewardedGranted = true
                }

                override fun onAdDisplayed(ad: MaxAd) = Unit
                override fun onAdHidden(ad: MaxAd) {
                    if (cont.isActive) {
                        cont.resume(if (rewardedGranted) AdLoadResult.Ready else AdLoadResult.NoFill)
                    }
                }

                override fun onAdClicked(ad: MaxAd) = Unit
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    if (cont.isActive) cont.resume(AdLoadResult.NoFill)
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    if (cont.isActive) cont.resume(AdLoadResult.Error(error.message))
                }
            })
            ad.loadAd()
        }
    }

    override fun destroy() {
        interstitial = null
        rewarded = null
    }
}
