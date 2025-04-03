package com.iamashad.meraki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database class for managing local chat message storage.
 * Defines the database schema and provides the DAO instance.
 */
@Database(entities = [ChatMessage::class], version = 5, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {

    /**
     * Provides access to the ChatDao for database operations.
     */
    abstract fun chatDao(): ChatDao

    companion object {
        // Ensures a single instance of the database is used across the app (singleton).
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        /**
         * Returns the singleton instance of the database.
         * Creates it if it doesn't exist using Room's database builder.
         *
         * @param context The application context used to build the database.
         * @return ChatDatabase instance.
         */
        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                    // This allows the database to be rebuilt if no migration is provided.
                    // Use cautiously in production as it deletes existing data.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
