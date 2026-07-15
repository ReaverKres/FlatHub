package maps

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class IosMapTileDiskCache(
    maxBytes: Long = DEFAULT_MAX_BYTES,
    maxFiles: Int = DEFAULT_MAX_FILES,
) : MapTileDiskCache {
    private val root: String
    private val maxBytes = maxBytes.coerceAtLeast(1L * 1024 * 1024)
    private val maxFiles = maxFiles.coerceAtLeast(100)
    private val fileManager = NSFileManager.defaultManager

    init {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true,
        )
        val caches = paths.firstOrNull() as? String ?: ""
        root = "$caches/map_tiles"
        fileManager.createDirectoryAtPath(
            root,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    override fun get(key: String): ByteArray? {
        val path = pathFor(key)
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        fileManager.setAttributes(
            mapOf(NSFileModificationDate to NSDate()),
            ofItemAtPath = path,
            error = null,
        )
        return data.toByteArray()
    }

    override fun put(key: String, bytes: ByteArray) {
        bytes.toNSData().writeToFile(pathFor(key), atomically = true)
        trimIfNeeded()
    }

    private fun pathFor(key: String): String {
        val safe = key.replace('/', '_')
        return "$root/$safe.png"
    }

    private fun trimIfNeeded() {
        val names = fileManager.contentsOfDirectoryAtPath(root, error = null) ?: return

        data class Entry(val path: String, val modified: Double, val size: Long)

        val entries = names.mapNotNull { name ->
            val fileName = name as? String ?: return@mapNotNull null
            val path = "$root/$fileName"
            val attrs =
                fileManager.attributesOfItemAtPath(path, error = null) ?: return@mapNotNull null
            val size = (attrs[NSFileSize] as? NSNumber)?.longLongValue ?: 0L
            val modified = (attrs[NSFileModificationDate] as? NSDate)?.timeIntervalSince1970 ?: 0.0
            Entry(path, modified, size)
        }.sortedBy { it.modified }

        var totalBytes = entries.sumOf { it.size }
        var remaining = entries.size
        for (entry in entries) {
            if (remaining <= maxFiles && totalBytes <= maxBytes) break
            totalBytes -= entry.size
            remaining--
            fileManager.removeItemAtPath(entry.path, error = null)
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return ByteArray(0)
        val out = ByteArray(size)
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
        return out
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

    private companion object {
        const val DEFAULT_MAX_BYTES = 30L * 1024 * 1024
        const val DEFAULT_MAX_FILES = 1_500
    }
}
