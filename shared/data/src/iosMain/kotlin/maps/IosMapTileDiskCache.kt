package maps

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class IosMapTileDiskCache(
    maxFiles: Int = 2_000,
) : MapTileDiskCache {
    private val root: String
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
        val data = NSData.dataWithContentsOfFile(pathFor(key)) ?: return null
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
        val contents = fileManager.contentsOfDirectoryAtPath(root, error = null) ?: return
        if (contents.size <= maxFiles) return
        val overflow = contents.size - maxFiles
        contents.take(overflow).forEach { name ->
            val fileName = name as? String ?: return@forEach
            fileManager.removeItemAtPath("$root/$fileName", error = null)
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
}
