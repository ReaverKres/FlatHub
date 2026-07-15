package maps

import android.content.Context
import java.io.File

class AndroidMapTileDiskCache(
    context: Context,
    maxFiles: Int = 2_000,
) : MapTileDiskCache {
    private val root = File(context.cacheDir, "map_tiles").also { it.mkdirs() }
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
        val files = root.listFiles() ?: return
        if (files.size <= maxFiles) return
        files.sortBy { it.lastModified() }
        val toRemove = files.size - maxFiles
        repeat(toRemove) { index ->
            files.getOrNull(index)?.delete()
        }
    }
}
