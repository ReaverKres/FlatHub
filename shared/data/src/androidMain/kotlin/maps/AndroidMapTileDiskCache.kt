package maps

import android.content.Context
import java.io.File

class AndroidMapTileDiskCache(
    context: Context,
    maxBytes: Long = DEFAULT_MAX_BYTES,
    maxFiles: Int = DEFAULT_MAX_FILES,
) : MapTileDiskCache {
    private val root = File(context.cacheDir, "map_tiles").also { it.mkdirs() }
    private val maxBytes = maxBytes.coerceAtLeast(1L * 1024 * 1024)
    private val maxFiles = maxFiles.coerceAtLeast(100)

    override fun get(key: String): ByteArray? {
        val file = fileFor(key)
        if (!file.isFile) return null
        return runCatching {
            file.setLastModified(System.currentTimeMillis())
            file.readBytes()
        }.getOrNull()
    }

    override fun put(key: String, bytes: ByteArray) {
        runCatching {
            val file = fileFor(key)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            trimIfNeeded()
        }
    }

    private fun fileFor(key: String): File {
        val safe = key.replace('/', '_')
        return File(root, "$safe.png")
    }

    private fun trimIfNeeded() {
        val files = root.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return
        var totalBytes = files.sumOf { it.length() }
        var remaining = files.size
        for (file in files) {
            if (remaining <= maxFiles && totalBytes <= maxBytes) break
            totalBytes -= file.length()
            remaining--
            file.delete()
        }
    }

    private companion object {
        const val DEFAULT_MAX_BYTES = 30L * 1024 * 1024
        const val DEFAULT_MAX_FILES = 1_500
    }
}
