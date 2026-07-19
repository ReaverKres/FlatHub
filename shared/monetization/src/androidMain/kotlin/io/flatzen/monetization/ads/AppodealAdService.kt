package io.flatzen.monetization.ads

import android.app.Activity
import android.content.Context
import com.appodeal.ads.Appodeal
import com.appodeal.ads.RewardedVideoCallbacks
import com.appodeal.ads.utils.Log
import io.flatzen.monetization.billing.CurrentActivityHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Appodeal — Android. SDK starts from [AppodealConsentStartup] with a visible [Activity] for CMP.
 * Safe when app key empty: methods return [AdLoadResult.Disabled].
 *
 * Cold start initializes NATIVE|MREC only; REWARDED_VIDEO is added on first [showRewarded].
 */
class AppodealAdService(
    @Suppress("UNUSED_PARAMETER") context: Context,
    val androidAppKey: String,
) : AdService {

    @Volatile
    private var initialized = false

    @Volatile
    private var rewardedReady = false

    private val coldStartAdTypes = Appodeal.NATIVE or Appodeal.MREC
    private val rewardedAdTypes = coldStartAdTypes or Appodeal.REWARDED_VIDEO

    override fun initialize(androidAppKey: String, iosAppKey: String) {
        val activity = CurrentActivityHolder.activity
        if (activity != null && androidAppKey.isNotBlank()) {
            initializeWithActivity(activity)
        }
    }

    fun initializeWithActivity(activity: Activity) {
        if (androidAppKey.isBlank() || initialized) return
        Appodeal.setAutoCache(Appodeal.NATIVE, false)
        Appodeal.setAutoCache(Appodeal.MREC, false)
        Appodeal.setAutoCache(Appodeal.REWARDED_VIDEO, false)
        Appodeal.setTesting(false)
        Appodeal.setLogLevel(Log.LogLevel.none)
        Appodeal.initialize(activity, androidAppKey, coldStartAdTypes) { _ ->
            initialized = true
        }
    }

    /** Extends SDK init with rewarded when the user first opens a rewarded placement. */
    fun ensureRewardedInitialized(activity: Activity) {
        if (androidAppKey.isBlank() || rewardedReady) return
        Appodeal.setAutoCache(Appodeal.REWARDED_VIDEO, false)
        Appodeal.initialize(activity, androidAppKey, rewardedAdTypes) { _ ->
            initialized = true
            rewardedReady = true
        }
    }

    override fun isInitialized(): Boolean =
        initialized || Appodeal.isInitialized(Appodeal.NATIVE or Appodeal.MREC)

    override suspend fun prefetchNative(placement: String, count: Int): AdLoadResult {
        if (!isInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.NoFill
        val requested = count.coerceIn(1, 5)
        Appodeal.cache(activity, Appodeal.NATIVE, requested)
        return if (Appodeal.isLoaded(Appodeal.NATIVE)) {
            AdLoadResult.Ready
        } else {
            AdLoadResult.NoFill
        }
    }

    override suspend fun prefetchMrec(placement: String): AdLoadResult {
        if (!isInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.NoFill
        Appodeal.cache(activity, Appodeal.MREC)
        return if (Appodeal.canShow(Appodeal.MREC, placement)) {
            AdLoadResult.Ready
        } else {
            AdLoadResult.NoFill
        }
    }

    override suspend fun showRewarded(placement: String): AdLoadResult {
        if (placement.isBlank()) return AdLoadResult.Disabled
        val activity = CurrentActivityHolder.activity ?: return AdLoadResult.Error("No activity")
        ensureRewardedInitialized(activity)
        if (!isInitialized()) return AdLoadResult.Disabled
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
