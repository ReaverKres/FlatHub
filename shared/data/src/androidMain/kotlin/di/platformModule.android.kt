package di

import database.FlatsDao
import database.MapAreasDao
import database.SavedFiltersDao
import database.UserPreferencesDao
import io.flatzen.database.getDatabase
import org.koin.dsl.module

actual fun databaseModule() = module {
    single<FlatsDao> { getDatabase(get()).getDao() }
    single<SavedFiltersDao> { getDatabase(get()).getSavedFiltersDao() }
    single<UserPreferencesDao> { getDatabase(get()).getUserPreferencesDao() }
    single<MapAreasDao> { getDatabase(get()).getSavedMapAreasDao() }
}