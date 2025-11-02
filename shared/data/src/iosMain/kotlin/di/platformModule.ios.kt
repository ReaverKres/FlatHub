package di

import database.FlatsDao
import database.MapAreasDao
import database.SavedFiltersDao
import database.UserPreferencesDao
import io.flatzen.database.getDatabase
import org.koin.dsl.module

actual fun databaseModule() = module {
    single<FlatsDao> { getDatabase().getDao() }
    single<SavedFiltersDao> { getDatabase().getSavedFiltersDao() }
    single<UserPreferencesDao> { getDatabase().getUserPreferencesDao() }
    single<MapAreasDao> { getDatabase().getSavedMapAreasDao() }
}