package io.flatzen.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.AppodealNative
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val NATIVE_LOAD_POLL_MS = 300L
private const val NATIVE_LOAD_MAX_ATTEMPTS = 40

private val AdUIKitProperties = UIKitInteropProperties(
    isInteractive = true,
    isNativeAccessibilityEnabled = true,
)

actual fun clearNativeAdBatch(batchId: String) = Unit

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
    batchId: String?,
    slotIndex: Int,
    batchSize: Int,
    onAdLoadResult: ((loaded: Boolean) -> Unit)?,
) {
    val hideUntilLoaded = onAdLoadResult != null
    val adService = koinInject<AdService>()
    val api = AppodealNative.api

    if (!adService.isInitialized() || api == null || placement.isBlank()) {
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
    var nativeLoadTick by remember(placement, styleName, batchId, slotIndex, effectiveBatchSize) {
        mutableIntStateOf(0)
    }
    var isLoaded by remember(placement, styleName, batchId, slotIndex) { mutableStateOf(false) }
    var loadFailed by remember(placement, styleName, batchId, slotIndex) { mutableStateOf(false) }

    LaunchedEffect(placement, styleName, batchId, slotIndex, effectiveBatchSize) {
        if (batchId != null && slotIndex != 0) return@LaunchedEffect
        val prefetchCount = if (batchId != null) effectiveBatchSize else 1
        adService.prefetchNative(placement, prefetchCount)
        if (hideUntilLoaded) {
            nativeLoadTick++
        }
    }

    LaunchedEffect(nativeLoadTick, hideUntilLoaded, isLoaded, loadFailed) {
        if (!hideUntilLoaded || nativeLoadTick == 0 || isLoaded || loadFailed) return@LaunchedEffect
        repeat(NATIVE_LOAD_MAX_ATTEMPTS) {
            delay(NATIVE_LOAD_POLL_MS)
            if (isLoaded) return@LaunchedEffect
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

    key(placement, styleName, batchId, slotIndex) {
        UIKitView(
            factory = { api.createNativeView(placement, styleName) },
            modifier = modifier,
            update = { view ->
                api.showNative(view, placement)
                if (hideUntilLoaded && !isLoaded) {
                    isLoaded = true
                    nativeLoadTick++
                    onAdLoadResult(true)
                }
            },
            onRelease = {
                api.releaseView(it)
            },
            properties = AdUIKitProperties,
        )
    }
}
