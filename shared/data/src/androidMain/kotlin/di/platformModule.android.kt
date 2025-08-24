package di

import database.FlatsDao
import database.SavedFiltersDao
import io.flatzen.database.getDatabase
import org.koin.dsl.module

actual fun databaseModule() = module {
    single<FlatsDao> { getDatabase(get()).getDao() }
    single<SavedFiltersDao> { getDatabase(get()).getSavedFiltersDao() }
}