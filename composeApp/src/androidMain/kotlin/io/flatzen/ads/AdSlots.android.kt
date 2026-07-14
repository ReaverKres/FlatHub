package io.flatzen.ads

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appodeal.ads.Appodeal
import com.appodeal.ads.NativeAd
import com.appodeal.ads.nativead.NativeAdViewAppWall
import com.appodeal.ads.nativead.NativeAdViewContentStream
import io.flatzen.monetization.ads.AdService
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val NATIVE_LOAD_POLL_MS = 300L
private const val NATIVE_LOAD_MAX_ATTEMPTS = 40

private enum class NativeAdSlotState {
    Loading,
    Loaded,
    Failed,
}

private val nativeAdBatches = mutableMapOf<String, List<NativeAd>>()
private val batchLock = Any()

actual fun clearNativeAdBatch(batchId: String) {
    synchronized(batchLock) {
        nativeAdBatches.remove(batchId)
    }
}

@Composable
actual fun MrecAdSlot(
    placement: String,
    modifier: Modifier,
) {
    val activity = LocalActivity.current
    val adService = koinInject<AdService>()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
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
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(backgroundColor)
                val mrecView = Appodeal.getMrecView(activity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                }
                addView(mrecView)
            }
        },
        update = {
            showMrecIfReady(activity, placement)
        },
    )
}

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
    val activity = LocalActivity.current
    val adService = koinInject<AdService>()
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.surface.toArgb()
    val titleTextColor = colorScheme.onSurface.toArgb()
    val bodyTextColor = colorScheme.onSurfaceVariant.toArgb()

    if (activity == null || !adService.isInitialized() || placement.isBlank()) {
        if (hideUntilLoaded) {
            LaunchedEffect(Unit) { onAdLoadResult(false) }
        } else {
            AdSlotPlaceholder(modifier = modifier)
        }
        return
    }

    val effectiveBatchSize = batchSize.coerceIn(1, MAX_NATIVE_ADS_PER_BATCH)
    var nativeLoadTick by remember(placement, style, batchId, slotIndex, effectiveBatchSize) {
        mutableIntStateOf(0)
    }
    var loadState by remember(placement, style, batchId, slotIndex, effectiveBatchSize) {
        mutableStateOf(NativeAdSlotState.Loading)
    }

    LaunchedEffect(placement, style, batchId, slotIndex, effectiveBatchSize) {
        if (batchId == null) {
            adService.prefetchNative(placement)
            repeat(NATIVE_LOAD_MAX_ATTEMPTS) {
                if (Appodeal.isLoaded(Appodeal.NATIVE) && Appodeal.canShow(
                        Appodeal.NATIVE,
                        placement
                    )
                ) {
                    nativeLoadTick++
                    return@LaunchedEffect
                }
                Appodeal.cache(activity, Appodeal.NATIVE)
                delay(NATIVE_LOAD_POLL_MS)
            }
            return@LaunchedEffect
        }

        val adsAlreadyInBatch = synchronized(batchLock) {
            nativeAdBatches[batchId]?.size ?: 0
        }
        val adsNeededForSlot = slotIndex + 1
        val prefetchCount = (adsNeededForSlot - adsAlreadyInBatch)
            .coerceAtLeast(1)
            .coerceAtMost(MAX_NATIVE_ADS_PER_BATCH)

        if (slotIndex == 0) {
            adService.prefetchNative(placement, effectiveBatchSize)
        } else {
            Appodeal.cache(activity, Appodeal.NATIVE, prefetchCount)
        }

        var sdkReady = false
        repeat(NATIVE_LOAD_MAX_ATTEMPTS) {
            val batchReady = synchronized(batchLock) {
                (nativeAdBatches[batchId]?.size ?: 0) > slotIndex
            }
            sdkReady = Appodeal.getAvailableNativeAdsCount() > 0 &&
                    Appodeal.canShow(Appodeal.NATIVE, placement)
            if (batchReady || sdkReady) {
                nativeLoadTick++
                return@LaunchedEffect
            }
            Appodeal.cache(activity, Appodeal.NATIVE, prefetchCount)
            delay(NATIVE_LOAD_POLL_MS)
        }

        if (hideUntilLoaded && loadState != NativeAdSlotState.Loaded) {
            loadState = NativeAdSlotState.Failed
            onAdLoadResult(false)
        }
    }

    LaunchedEffect(nativeLoadTick, hideUntilLoaded, loadState) {
        if (!hideUntilLoaded || nativeLoadTick == 0 || loadState != NativeAdSlotState.Loading) {
            return@LaunchedEffect
        }
        repeat(NATIVE_LOAD_MAX_ATTEMPTS) {
            delay(NATIVE_LOAD_POLL_MS)
            if (loadState == NativeAdSlotState.Loaded) return@LaunchedEffect
        }
        if (loadState != NativeAdSlotState.Loaded) {
            loadState = NativeAdSlotState.Failed
            onAdLoadResult?.invoke(false)
        }
    }

    if (hideUntilLoaded && loadState == NativeAdSlotState.Failed) {
        return
    }
    if (hideUntilLoaded && loadState == NativeAdSlotState.Loading && nativeLoadTick == 0) {
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val nativeView = when (style) {
                NativeAdSlotStyle.AppWall -> NativeAdViewAppWall(context)
                NativeAdSlotStyle.ContentStream -> NativeAdViewContentStream(context)
            }.apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setBackgroundColor(backgroundColor)
                addView(nativeView)
                tag = nativeView
            }
        },
        update = { container ->
            nativeLoadTick
            container.setBackgroundColor(backgroundColor)
            val nativeView = container.tag as? View ?: return@AndroidView
            val registered = registerNativeAd(
                activity = activity,
                view = nativeView,
                placement = placement,
                batchId = batchId,
                slotIndex = slotIndex,
                batchSize = effectiveBatchSize,
            )
            applyNativeAdThemeColors(
                root = nativeView,
                titleTextColor = titleTextColor,
                bodyTextColor = bodyTextColor,
            )
            if (registered) {
                if (loadState != NativeAdSlotState.Loaded) {
                    loadState = NativeAdSlotState.Loaded
                    onAdLoadResult?.invoke(true)
                }
                nativeView.requestLayout()
                container.requestLayout()
            }
        },
        onRelease = { container ->
            when (val view = container.tag) {
                is NativeAdViewAppWall -> view.unregisterView()
                is NativeAdViewContentStream -> view.unregisterView()
            }
        },
    )
}

/**
 * Appodeal template TextViews often keep dark-theme (near-white) colors.
 * Re-tint them to match the current Material surface contrast.
 */
private fun applyNativeAdThemeColors(
    root: View,
    titleTextColor: Int,
    bodyTextColor: Int,
) {
    fun View.walk(isRoot: Boolean = false) {
        when (this) {
            is TextView -> {
                val text = text?.toString().orEmpty()
                val useBodyColor = !isRoot &&
                        (text.length > 48 || lineCount > 1 || textSize < 16f)
                setTextColor(if (useBodyColor) bodyTextColor else titleTextColor)
            }

            is ViewGroup -> {
                for (i in 0 until childCount) {
                    getChildAt(i).walk(isRoot = false)
                }
            }
        }
    }
    root.walk(isRoot = true)
}

private fun showMrecIfReady(activity: Activity, placement: String) {
    if (Appodeal.canShow(Appodeal.MREC, placement)) {
        Appodeal.show(activity, Appodeal.MREC, placement)
    } else {
        Appodeal.cache(activity, Appodeal.MREC)
    }
}

private fun obtainBatchNativeAd(
    activity: Activity,
    batchId: String,
    slotIndex: Int,
    batchSize: Int,
    placement: String,
): NativeAd? = synchronized(batchLock) {
    val cached = nativeAdBatches[batchId].orEmpty()
    if (slotIndex < cached.size) {
        return cached[slotIndex]
    }

    val targetCount = (slotIndex + 1).coerceAtMost(batchSize)
    val toRequest = targetCount - cached.size
    if (toRequest <= 0) return null

    if (!Appodeal.isLoaded(Appodeal.NATIVE) || !Appodeal.canShow(Appodeal.NATIVE, placement)) {
        Appodeal.cache(activity, Appodeal.NATIVE, toRequest)
        return null
    }

    val newAds = Appodeal.getNativeAds(toRequest)
    if (newAds.isEmpty()) return null

    val merged = cached + newAds
    nativeAdBatches[batchId] = merged
    merged.getOrNull(slotIndex)
}

private fun registerNativeAd(
    activity: Activity,
    view: android.view.View,
    placement: String,
    batchId: String?,
    slotIndex: Int,
    batchSize: Int,
): Boolean {
    val nativeAd = if (batchId != null) {
        obtainBatchNativeAd(activity, batchId, slotIndex, batchSize, placement)
    } else {
        if (!Appodeal.isLoaded(Appodeal.NATIVE) || !Appodeal.canShow(Appodeal.NATIVE, placement)) {
            Appodeal.cache(activity, Appodeal.NATIVE)
            return false
        }
        Appodeal.getNativeAds(1).firstOrNull()
    } ?: return false

    return when (view) {
        is NativeAdViewAppWall -> view.registerView(nativeAd, placement)
        is NativeAdViewContentStream -> view.registerView(nativeAd, placement)
        else -> false
    }
}
