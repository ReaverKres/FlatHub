package di

import database.FlatsDao
import database.SavedFiltersDao
import database.UserMapAreasDao
import database.UserPreferencesDao
import io.flatzen.database.getDatabase
import maps.IosMapTileDiskCache
import maps.MapTileDiskCache
import org.koin.dsl.module

actual fun databaseModule() = module {
    single<FlatsDao> { getDatabase().getDao() }
    single<SavedFiltersDao> { getDatabase().getSavedFiltersDao() }
    single<UserPreferencesDao> { getDatabase().getUserPreferencesDao() }
    single<UserMapAreasDao> { getDatabase().getSavedMapAreasDao() }
    single<MapTileDiskCache> { IosMapTileDiskCache() }
}
