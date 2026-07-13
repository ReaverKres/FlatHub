package io.flatzen.monetization.ads

import android.app.Activity
import android.content.Context
import com.appodeal.ads.Appodeal
import com.appodeal.ads.RewardedVideoCallbacks
import io.flatzen.monetization.billing.CurrentActivityHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Appodeal — Android. Requires Activity for rewarded / view display.
 * Safe when app key empty: methods return [AdLoadResult.Disabled].
 */
class AppodealAdService(
    private val context: Context,
) : AdService {

    @Volatile
    private var initialized = false

    override fun initialize(androidAppKey: String, iosAppKey: String) {
        if (androidAppKey.isBlank() || initialized) return
        val adTypes = Appodeal.NATIVE or Appodeal.MREC or Appodeal.REWARDED_VIDEO
        Appodeal.setAutoCache(Appodeal.NATIVE, true)
        Appodeal.setAutoCache(Appodeal.MREC, true)
        Appodeal.setAutoCache(Appodeal.REWARDED_VIDEO, true)
        Appodeal.setTesting(false)
        Appodeal.initialize(context, androidAppKey, adTypes) { _ ->
            initialized = true
        }
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun prefetchNative(placement: String, count: Int): AdLoadResult {
        if (!initialized || placement.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.NoFill
        Appodeal.cache(activity, Appodeal.NATIVE)
        return if (Appodeal.isLoaded(Appodeal.NATIVE)) {
            AdLoadResult.Ready
        } else {
            AdLoadResult.NoFill
        }
    }

    override suspend fun prefetchMrec(placement: String): AdLoadResult {
        if (!initialized || placement.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.NoFill
        Appodeal.cache(activity, Appodeal.MREC)
        return if (Appodeal.canShow(Appodeal.MREC, placement)) {
            AdLoadResult.Ready
        } else {
            AdLoadResult.NoFill
        }
    }

    override suspend fun showRewarded(placement: String): AdLoadResult {
        if (!initialized || placement.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.Error("No activity")
        if (!Appodeal.canShow(Appodeal.REWARDED_VIDEO, placement)) {
            Appodeal.cache(activity, Appodeal.REWARDED_VIDEO)
            if (!Appodeal.canShow(Appodeal.REWARDED_VIDEO, placement)) {
                return AdLoadResult.NoFill
            }
        }
        return suspendCancellableCoroutine { cont ->
            var rewardedGranted = false
            Appodeal.setRewardedVideoCallbacks(object : RewardedVideoCallbacks {
                override fun onRewardedVideoLoaded(isPrecache: Boolean) = Unit

                override fun onRewardedVideoFailedToLoad() {
                    if (cont.isActive) cont.resume(AdLoadResult.NoFill)
                }

                override fun onRewardedVideoShown() = Unit

                override fun onRewardedVideoShowFailed() {
                    if (cont.isActive) cont.resume(AdLoadResult.Error("Show failed"))
                }

                override fun onRewardedVideoFinished(amount: Double, currency: String) {
                    rewardedGranted = true
                }

                override fun onRewardedVideoClosed(finished: Boolean) {
                    if (cont.isActive) {
                        cont.resume(if (rewardedGranted) AdLoadResult.Ready else AdLoadResult.NoFill)
                    }
                }

                override fun onRewardedVideoExpired() = Unit

                override fun onRewardedVideoClicked() = Unit
            })
            Appodeal.show(activity, Appodeal.REWARDED_VIDEO, placement)
        }
    }

    override fun destroy() {
        Appodeal.destroy(Appodeal.NATIVE)
        Appodeal.destroy(Appodeal.MREC)
        Appodeal.destroy(Appodeal.REWARDED_VIDEO)
    }
}
