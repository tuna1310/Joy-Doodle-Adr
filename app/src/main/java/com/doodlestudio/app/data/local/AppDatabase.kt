package com.doodlestudio.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ArtworkEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
}

