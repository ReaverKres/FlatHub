package maps

/**
 * Persistent tile cache for map raster tiles (PNG).
 * Platform implementations store under the app cache directory.
 */
interface MapTileDiskCache {
    fun get(key: String): ByteArray?
    fun put(key: String, bytes: ByteArray)
}

class NoOpMapTileDiskCache : MapTileDiskCache {
    override fun get(key: String): ByteArray? = null
    override fun put(key: String, bytes: ByteArray) = Unit
}
