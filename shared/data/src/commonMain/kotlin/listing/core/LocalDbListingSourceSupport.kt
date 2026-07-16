package listing.core

import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Shared local-DB helpers for PL (and future) sources that store list payloads in Room
 * and do not yet implement network detail / platform caches.
 */
internal fun FlatsDao.flowById(platform: FlatPlatform, adId: Long): Flow<AppFlat?> = flow {
    emit(getById(platform, adId))
}
