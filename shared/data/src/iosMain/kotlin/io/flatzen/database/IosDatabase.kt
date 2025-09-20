package io.flatzen.database

import androidx.room.Room
import androidx.room.RoomDatabase
import database.AppDatabase
import database.getRoomDatabase
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/my_room.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath
    )
}

fun getDatabase(): AppDatabase {
    return getRoomDatabase(getDatabaseBuilder())
}