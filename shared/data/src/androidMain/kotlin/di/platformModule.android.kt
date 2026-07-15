package di

import android.content.Context
import database.FlatsDao
import database.SavedFiltersDao
import database.UserMapAreasDao
import database.UserPreferencesDao
import io.flatzen.database.getDatabase
import maps.AndroidMapTileDiskCache
import maps.MapTileDiskCache
import org.koin.dsl.module

actual fun databaseModule() = module {
    single<FlatsDao> { getDatabase(get()).getDao() }
    single<SavedFiltersDao> { getDatabase(get()).getSavedFiltersDao() }
    single<UserPreferencesDao> { getDatabase(get()).getUserPreferencesDao() }
    single<UserMapAreasDao> { getDatabase(get()).getSavedMapAreasDao() }
    single<MapTileDiskCache> { AndroidMapTileDiskCache(get<Context>()) }
}