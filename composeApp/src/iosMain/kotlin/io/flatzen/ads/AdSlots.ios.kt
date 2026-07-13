package io.flatzen.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.AppodealNative
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.koinInject

private val AdUIKitProperties = UIKitInteropProperties(
    isInteractive = true,
    isNativeAccessibilityEnabled = true,
)

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MrecAdSlot(
    placement: String,
    modifier: Modifier,
) {
    val adService = koinInject<AdService>()
    val api = AppodealNative.api
    if (!adService.isInitialized() || api == null || placement.isBlank()) {
        AdSlotPlaceholder(modifier = modifier)
        return
    }

    LaunchedEffect(placement) {
        adService.prefetchMrec(placement)
    }

    key(placement) {
        UIKitView(
            factory = { api.createMrecView(placement) },
            modifier = modifier,
            update = {
                api.showMrec(placement)
            },
            onRelease = {
                api.releaseView(it)
            },
            properties = AdUIKitProperties,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeAdSlot(
    placement: String,
    modifier: Modifier,
    style: NativeAdSlotStyle,
) {
    val adService = koinInject<AdService>()
    val api = AppodealNative.api
    if (!adService.isInitialized() || api == null || placement.isBlank()) {
        AdSlotPlaceholder(modifier = modifier)
        return
    }

    val styleName = when (style) {
        NativeAdSlotStyle.ContentStream -> "content_stream"
        NativeAdSlotStyle.AppWall -> "app_wall"
    }

    LaunchedEffect(placement, styleName) {
        adService.prefetchNative(placement)
    }

    key(placement, styleName) {
        UIKitView(
            factory = { api.createNativeView(placement, styleName) },
            modifier = modifier,
            update = {
                api.showNative(it, placement)
            },
            onRelease = {
                api.releaseView(it)
            },
            properties = AdUIKitProperties,
        )
    }
}
