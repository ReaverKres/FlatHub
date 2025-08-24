package io.flatzen.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import database.AppDatabase
import database.getRoomDatabase

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("my_room.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .fallbackToDestructiveMigration(true)
}

fun getDatabase(ctx: Context): AppDatabase {
    return getRoomDatabase(getDatabaseBuilder(ctx))
}