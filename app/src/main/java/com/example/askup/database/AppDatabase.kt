package com.example.askup.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Main database for the app
// We add each new entity to the entities array as we build features
@Database(
    entities = [
        User::class,
        Session::class,
        Question::class  
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAOs for accessing each table
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun questionDao(): QuestionDao  // Added question DAO

    companion object {
        // Singleton pattern - only one database instance
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If database already exists, return it
            // Otherwise create a new one
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "askup_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}