package maps

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import ovh.plrapps.mapcompose.core.TileStreamProvider

/**
 * Tile provider for Carto/OSM raster tiles with memory LRU + optional disk cache.
 */
class TileProviderImpl(
    private val httpClient: HttpClient,
    private val diskCache: MapTileDiskCache = NoOpMapTileDiskCache(),
    memoryCacheSize: Int = 512,
) : TileStreamProvider {

    private val maxMemoryEntries = memoryCacheSize.coerceAtLeast(32)
    private val memoryMutex = Mutex()
    private val memoryCache = LinkedHashMap<String, ByteArray>()
    private val accessOrder = ArrayDeque<String>()

    override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): RawSource? {
        val cacheKey = "$zoomLvl/$col/$row"
        return try {
            memoryGet(cacheKey)?.let { return createRawSourceFromBytes(it) }

            diskCache.get(cacheKey)?.let { fromDisk ->
                memoryPut(cacheKey, fromDisk)
                return createRawSourceFromBytes(fromDisk)
            }

            val url = "https://basemaps.cartocdn.com/rastertiles/voyager/$zoomLvl/$col/$row.png"
            val bytes: ByteArray = httpClient.get(url).body()
            memoryPut(cacheKey, bytes)
            diskCache.put(cacheKey, bytes)
            createRawSourceFromBytes(bytes)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun memoryGet(key: String): ByteArray? = memoryMutex.withLock {
        val bytes = memoryCache[key] ?: return@withLock null
        accessOrder.remove(key)
        accessOrder.addLast(key)
        bytes
    }

    private suspend fun memoryPut(key: String, bytes: ByteArray) = memoryMutex.withLock {
        if (memoryCache.containsKey(key)) {
            accessOrder.remove(key)
        }
        memoryCache[key] = bytes
        accessOrder.addLast(key)
        while (memoryCache.size > maxMemoryEntries) {
            val eldest = accessOrder.removeFirstOrNull() ?: break
            memoryCache.remove(eldest)
        }
    }

    private fun createRawSourceFromBytes(bytes: ByteArray): RawSource {
        val buffer = Buffer()
        buffer.write(bytes)
        return buffer
    }
}
