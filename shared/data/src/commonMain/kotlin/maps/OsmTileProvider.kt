package maps

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.Url
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import ovh.plrapps.mapcompose.core.TileStreamProvider

/**
 * Простейший TileStreamProvider для OSM.
 * В бою добавьте кэш и троттлинг запросов.
 */
class CachedOsmTileProvider(
    private val httpClient: HttpClient,
    private val cacheSize: Int = 1000
) : TileStreamProvider {

    private val cache = HashMap<String, ByteArray>()

    override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): RawSource? {
        val cacheKey = "$zoomLvl/$col/$row"

        return try {
//            // Проверяем кэш
            val cachedData = cache[cacheKey]
            if (cachedData != null) {
                return createRawSourceFromBytes(cachedData)
            }

            // Загружаем из сети
            val url = "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"
            val bytes: ByteArray = httpClient.get(url).body()

//            // Сохраняем в кэш (с ограничением размера)
            if (cache.size >= cacheSize) {
                clearCache() // Простая стратегия очистки
            }
            cache[cacheKey] = bytes

            createRawSourceFromBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createRawSourceFromBytes(bytes: ByteArray): RawSource {
        val buffer = Buffer()
        buffer.write(bytes)
        return buffer
    }

    fun clearCache() {
        cache.clear()
    }

    fun getCacheSize(): Int = cache.size
}


