package io.flatzen.monetization.ads

import android.app.Activity
import com.appodeal.ads.Appodeal
import com.appodeal.consent.ConsentInfoUpdateCallback
import com.appodeal.consent.ConsentManager
import com.appodeal.consent.ConsentManagerError
import com.appodeal.consent.ConsentUpdateRequestParameters
import com.appodeal.consent.OnConsentFormDismissedListener

/**
 * Requests GDPR/CCPA consent (Stack Consent Manager / UMP) then initializes Appodeal with [Activity].
 * Must run on the main thread from a visible activity (typically [android.app.Activity.onCreate]).
 */
object AppodealConsentStartup {

    @Volatile
    private var started = false

    fun start(activity: Activity, adService: AdService) {
        val appodealService = adService as? AppodealAdService ?: return
        if (started || appodealService.isInitialized()) return
        started = true

        ConsentManager.requestConsentInfoUpdate(
            parameters = ConsentUpdateRequestParameters(
                activity = activity,
                key = appodealService.androidAppKey,
                tagForUnderAgeOfConsent = false,
                sdk = "Appodeal",
                sdkVersion = Appodeal.getVersion(),
            ),
            callback = object : ConsentInfoUpdateCallback {
                override fun onUpdated() {
                    showConsentFormIfRequired(activity, appodealService)
                }

                override fun onFailed(error: ConsentManagerError) {
                    showConsentFormIfRequired(activity, appodealService)
                }
            },
        )
    }

    private fun showConsentFormIfRequired(activity: Activity, adService: AppodealAdService) {
        ConsentManager.loadAndShowConsentFormIfRequired(
            activity = activity,
            dismissedListener = object : OnConsentFormDismissedListener {
                override fun onConsentFormDismissed(error: ConsentManagerError?) {
                    adService.initializeWithActivity(activity)
                }
            },
        )
    }
}
