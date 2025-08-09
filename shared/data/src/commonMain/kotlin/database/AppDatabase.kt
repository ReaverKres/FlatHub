package database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import entities.AppFlat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [AppFlat::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getDao(): FlatsDao
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}