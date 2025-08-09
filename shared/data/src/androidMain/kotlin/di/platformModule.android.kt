package di

import database.AppDatabase
import database.FlatsDao
import io.flatzen.database.getDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun databaseModule() = module {
    single<FlatsDao> { getDatabase(get()).getDao() }
}