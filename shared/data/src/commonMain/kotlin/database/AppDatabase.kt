package database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import entities.AppFlat
import entities.SavedFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [AppFlat::class, SavedFilter::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getDao(): FlatsDao
    abstract fun getSavedFiltersDao(): SavedFiltersDao
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