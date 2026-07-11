package io.flatzen.monetization.ads

sealed class FeedItem<out T> {
    data class Flat<T>(val value: T) : FeedItem<T>()
    data object Ad : FeedItem<Nothing>()
}

fun <T> buildFeedItems(
    flats: List<T>,
    interval: Int,
    showAds: Boolean,
): List<FeedItem<T>> {
    if (!showAds || interval <= 0) return flats.map { FeedItem.Flat(it) }
    val result = mutableListOf<FeedItem<T>>()
    flats.forEachIndexed { index, flat ->
        result += FeedItem.Flat(flat)
        if ((index + 1) % interval == 0) result += FeedItem.Ad
    }
    return result
}
