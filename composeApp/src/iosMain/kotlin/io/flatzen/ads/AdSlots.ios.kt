package io.flatzen.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.AppodealNative
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import platform.UIKit.UIView

private const val NATIVE_LOAD_POLL_MS = 200L
private const val NATIVE_LOAD_MAX_ATTEMPTS = 40

/** ~30s — covers delayed Appodeal bridge/SDK init. */
private const val SDK_INIT_MAX_ATTEMPTS = 150

private val AdUIKitProperties = UIKitInteropProperties(
    isInteractive = true,
    isNativeAccessibilityEnabled = true,
)

actual fun clearNativeAdBatch(batchId: String) = Unit

actual fun clearNativeAdReuseCache() {
    AppodealNative.api?.clearNativeAdReuseCache()
}

@Composable
private fun rememberSdkReady(
    adService: AdService,
    apiAvailable: Boolean,
    onInitTimeout: (() -> Unit)? = null,
): Boolean {
    var sdkReady by remember {
        mutableStateOf(adService.isInitialized() && apiAvailable)
    }
    LaunchedEffect(adService, apiAvailable) {
        if (sdkReady) return@LaunchedEffect
        repeat(SDK_INIT_MAX_ATTEMPTS) {
            val api = AppodealNative.api
            if (adService.isInitialized() && api != null) {
                sdkReady = true
                return@LaunchedEffect
            }
            delay(NATIVE_LOAD_POLL_MS)
        }
        onInitTimeout?.invoke()
    }
    return sdkReady
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MrecAdSlot(
    placement: String,
    modifier: Modifier,
) {
    val adService = koinInject<AdService>()
    val sdkReady = rememberSdkReady(
        adService,
        apiAvailable = AppodealNative.api != null,
    )

    if (placement.isBlank()) {
        AdSlotPlaceholder(modifier = modifier)
        return
    }
    if (!sdkReady) {
        AdSlotPlaceholder(modifier = modifier)
        return
    }
    val api = AppodealNative.api
    if (api == null) {
        AdSlotPlaceholder(modifier = modifier)
        return
    }

    LaunchedEffect(placement) {
        adService.prefetchMrec(placement)
    }

    key(placement) {
        UIKitView(
            factory = { api.createMrecView(placement) },
            // Fixed size: wrapContent UIKit hosts often measure as 0×0 on iOS.
            modifier = modifier.fillMaxWidth().height(250.dp),
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
    reuseKey: String?,
    batchId: String?,
    slotIndex: Int,
    batchSize: Int,
    onAdLoadResult: ((loaded: Boolean) -> Unit)?,
) {
    val hideUntilLoaded = onAdLoadResult != null
    val adService = koinInject<AdService>()

    var initTimedOut by remember { mutableStateOf(false) }
    val sdkReady = rememberSdkReady(
        adService,
        apiAvailable = AppodealNative.api != null,
    ) {
        initTimedOut = true
        if (hideUntilLoaded) {
            onAdLoadResult(false)
        }
    }

    if (placement.isBlank()) {
        if (hideUntilLoaded) {
            LaunchedEffect(Unit) { onAdLoadResult(false) }
        } else {
            AdSlotPlaceholder(modifier = modifier)
        }
        return
    }

    if (!sdkReady) {
        if (initTimedOut) {
            if (!hideUntilLoaded) {
                AdSlotPlaceholder(modifier = modifier)
            }
            return
        }
        // Wait for bridge/SDK init — do not report failure yet.
        if (!hideUntilLoaded) {
            AdSlotPlaceholder(modifier = modifier)
        }
        return
    }
    val api = AppodealNative.api
    if (api == null) {
        if (hideUntilLoaded) {
            LaunchedEffect(Unit) { onAdLoadResult(false) }
        } else {
            AdSlotPlaceholder(modifier = modifier)
        }
        return
    }

    val styleName = when (style) {
        NativeAdSlotStyle.ContentStream -> "content_stream"
        NativeAdSlotStyle.AppWall -> "app_wall"
    }
    val effectiveBatchSize = batchSize.coerceIn(1, MAX_NATIVE_ADS_PER_BATCH)
    var nativeLoadTick by remember(
        placement,
        styleName,
        reuseKey,
        batchId,
        slotIndex,
        effectiveBatchSize
    ) {
        mutableIntStateOf(0)
    }
    var isLoaded by remember(placement, styleName, reuseKey, batchId, slotIndex) {
        mutableStateOf(false)
    }
    var loadFailed by remember(placement, styleName, reuseKey, batchId, slotIndex) {
        mutableStateOf(false)
    }
    var hostView by remember(placement, styleName, reuseKey, batchId, slotIndex) {
        mutableStateOf<UIView?>(null)
    }

    LaunchedEffect(placement, styleName, reuseKey, batchId, slotIndex, effectiveBatchSize) {
        if (batchId == null || slotIndex == 0) {
            val prefetchCount = if (batchId != null) effectiveBatchSize else 1
            adService.prefetchNative(placement, prefetchCount)
        }
        nativeLoadTick++
    }

    LaunchedEffect(nativeLoadTick, hostView, isLoaded, loadFailed) {
        if (nativeLoadTick == 0 || isLoaded || loadFailed) return@LaunchedEffect
        val view = hostView ?: return@LaunchedEffect
        repeat(NATIVE_LOAD_MAX_ATTEMPTS) {
            if (api.showNative(view, placement)) {
                isLoaded = true
                onAdLoadResult?.invoke(true)
                return@LaunchedEffect
            }
            delay(NATIVE_LOAD_POLL_MS)
        }
        if (!isLoaded) {
            loadFailed = true
            onAdLoadResult?.invoke(false)
        }
    }

    if (hideUntilLoaded && loadFailed) {
        return
    }
    if (hideUntilLoaded && nativeLoadTick == 0) {
        return
    }

    // Fixed height is required on iOS: wrapContent / heightIn still let UIKitView hosts
    // measure as 0×0, which breaks Appodeal native template constraints.
    val slotModifier = modifier
        .fillMaxWidth()
        .height(NativeAdMinHeight)

    key(placement, styleName, reuseKey, batchId, slotIndex) {
        UIKitView(
            factory = {
                api.createNativeView(placement, styleName, reuseKey).also { hostView = it }
            },
            modifier = slotModifier,
            update = { view ->
                hostView = view
                if (!isLoaded && api.showNative(view, placement)) {
                    isLoaded = true
                    onAdLoadResult?.invoke(true)
                }
            },
            onRelease = {
                // Keep reusedNativeAds; only detach the view (Appodeal list guidance).
                if (hostView === it) {
                    hostView = null
                }
                api.releaseView(it)
            },
            properties = AdUIKitProperties,
        )
    }
}
