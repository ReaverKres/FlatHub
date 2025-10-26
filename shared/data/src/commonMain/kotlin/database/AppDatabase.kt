package database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import entities.AppFlat
import entities.SavedFilter
import entities.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [AppFlat::class, SavedFilter::class, UserPreferences::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getDao(): FlatsDao
    abstract fun getSavedFiltersDao(): SavedFiltersDao
    abstract fun getUserPreferencesDao(): UserPreferencesDao
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}