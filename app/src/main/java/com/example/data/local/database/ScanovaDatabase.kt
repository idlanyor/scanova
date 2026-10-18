package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ScanDao
import com.example.data.local.entity.ScanEntity

@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScanovaDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: ScanovaDatabase? = null

        fun getInstance(context: Context): ScanovaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanovaDatabase::class.java,
                    "scanova_history.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
