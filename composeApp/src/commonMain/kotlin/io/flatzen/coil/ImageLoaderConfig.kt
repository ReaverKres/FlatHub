package io.flatzen.coil

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.commoncomponents.utils.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Path

private const val MB = 1024L * 1024
private const val GB = 1024L * MB

private const val DISK_CACHE_MAX_BYTES = 40L * MB
private const val MEMORY_CACHE_PERCENT = 0.10
private const val MEMORY_CACHE_LOW_RAM_PERCENT = 0.05

/** Disable caches when free space is below this. */
private const val FREE_DISK_MIN_BYTES = 500L * MB

/** Android: soft low-RAM tier below this. */
private const val ANDROID_LOW_RAM_BYTES = 3L * GB

/** Android: full cache off below this. */
private const val ANDROID_CRITICAL_RAM_BYTES = 2L * GB

/** iOS: soft low-RAM tier at or below this (e.g. iPhone 6s = 2GB). */
private const val IOS_LOW_RAM_BYTES = 2L * GB

internal data class ImageCachePolicy(
    /** `null` disables the memory cache. */
    val memoryMaxPercent: Double?,
    /** `null` disables the disk cache. */
    val diskMaxBytes: Long?,
)

internal fun DevicePlatform.resolveImageCachePolicy(): ImageCachePolicy {
    val ram = totalRamBytes()
    val lowDisk = freeDiskBytes() < FREE_DISK_MIN_BYTES

    val lowRam = when (platformType) {
        PlatformType.ANDROID -> ram < ANDROID_LOW_RAM_BYTES
        PlatformType.IOS -> ram <= IOS_LOW_RAM_BYTES
    }
    val veryWeak = when (platformType) {
        PlatformType.ANDROID -> ram < ANDROID_CRITICAL_RAM_BYTES
        PlatformType.IOS -> ram < IOS_LOW_RAM_BYTES
    }

    // Full off: critically low RAM, or free disk below the safety threshold.
    if (veryWeak || lowDisk) {
        return ImageCachePolicy(memoryMaxPercent = null, diskMaxBytes = null)
    }

    // Soft low-RAM: small memory cache for scroll FPS, no disk I/O.
    if (lowRam) {
        return ImageCachePolicy(
            memoryMaxPercent = MEMORY_CACHE_LOW_RAM_PERCENT,
            diskMaxBytes = null,
        )
    }

    return ImageCachePolicy(
        memoryMaxPercent = MEMORY_CACHE_PERCENT,
        diskMaxBytes = DISK_CACHE_MAX_BYTES,
    )
}

fun configureSingletonImageLoader(devicePlatform: DevicePlatform) {
    val policy = runBlocking {
        withContext(Dispatchers.Default) {
            devicePlatform.resolveImageCachePolicy()
        }
    }
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .memoryCache {
                val percent = policy.memoryMaxPercent ?: return@memoryCache null
                MemoryCache.Builder()
                    .maxSizePercent(context, percent)
                    .build()
            }
            .diskCache {
                val maxBytes = policy.diskMaxBytes ?: return@diskCache null
                DiskCache.Builder()
                    .directory(imageCacheDirectory(context))
                    .maxSizeBytes(maxBytes)
                    .build()
            }
            .build()
    }
}

internal expect fun imageCacheDirectory(context: PlatformContext): Path
