package io.flatzen.ads

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appodeal.ads.Appodeal
import com.appodeal.ads.nativead.NativeAdViewAppWall
import com.appodeal.ads.nativead.NativeAdViewContentStream
import io.flatzen.monetization.ads.AdService
import org.koin.compose.koinInject

@Composable
actual fun MrecAdSlot(
    placement: String,
    modifier: Modifier,
) {
    val activity = LocalActivity.current
    val adService = koinInject<AdService>()
    if (activity == null || !adService.isInitialized() || placement.isBlank()) {
        AdSlotPlaceholder(modifier = modifier.fillMaxWidth().height(250.dp))
        return
    }

    LaunchedEffect(placement) {
        adService.prefetchMrec(placement)
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(250.dp),
        factory = { context ->
            Appodeal.getMrecView(activity).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        },
        update = { view ->
            showMrecIfReady(activity, placement)
        },
        onRelease = { _ ->
            // MrecView is a FrameLayout; Appodeal manages ad lifecycle globally.
        },
    )
}

@Composable
actual fun NativeAdSlot(
    placement: String,
    modifier: Modifier,
    style: NativeAdSlotStyle,
) {
    val activity = LocalActivity.current
    val adService = koinInject<AdService>()
    if (activity == null || !adService.isInitialized() || placement.isBlank()) {
        AdSlotPlaceholder(modifier = modifier)
        return
    }

    LaunchedEffect(placement, style) {
        adService.prefetchNative(placement)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            when (style) {
                NativeAdSlotStyle.AppWall -> NativeAdViewAppWall(context)
                NativeAdSlotStyle.ContentStream -> NativeAdViewContentStream(context)
            }
        },
        update = { view ->
            registerNativeAd(activity, view, placement)
        },
        onRelease = { view ->
            when (view) {
                is NativeAdViewAppWall -> view.unregisterView()
                is NativeAdViewContentStream -> view.unregisterView()
            }
        },
    )
}

private fun showMrecIfReady(activity: Activity, placement: String) {
    if (Appodeal.canShow(Appodeal.MREC, placement)) {
        Appodeal.show(activity, Appodeal.MREC, placement)
    } else {
        Appodeal.cache(activity, Appodeal.MREC)
    }
}

private fun registerNativeAd(
    activity: Activity,
    view: android.view.View,
    placement: String,
) {
    if (!Appodeal.isLoaded(Appodeal.NATIVE)) {
        Appodeal.cache(activity, Appodeal.NATIVE)
        return
    }
    val nativeAds = Appodeal.getNativeAds(1)
    val nativeAd = nativeAds.firstOrNull() ?: return
    when (view) {
        is NativeAdViewAppWall -> view.registerView(nativeAd, placement)
        is NativeAdViewContentStream -> view.registerView(nativeAd, placement)
    }
}
