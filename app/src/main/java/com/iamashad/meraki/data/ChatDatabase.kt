package com.iamashad.meraki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database class for managing local chat and emotion-log storage.
 *
 * Version history:
 *  v1–v5 : chat_messages table iterations (see git history).
 *  v6    : Phase 3 — adds the `emotion_logs` table for on-device emotion persistence.
 *  v7    : Phase 4 — adds the `session_summaries` table for long-term memory.
 *
 * All migrations are explicit and non-destructive: each only creates the new table,
 * leaving all existing data untouched.
 * [fallbackToDestructiveMigration] is retained as a last-resort safety net for
 * any un-bridged version gap that might exist on sideloaded / development builds.
 */
@Database(
    entities = [ChatMessage::class, EmotionLog::class, SessionSummary::class],
    version = 7,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    /** Chat message DAO — unchanged from previous versions. */
    abstract fun chatDao(): ChatDao

    /** Phase 3: emotion classification log DAO. */
    abstract fun emotionDao(): EmotionDao

    /** Phase 4: session summary / long-term memory DAO. */
    abstract fun sessionSummaryDao(): SessionSummaryDao

    companion object {

        @Volatile
        private var INSTANCE: ChatDatabase? = null

        // ── Migrations ────────────────────────────────────────────────────────

        /**
         * Non-destructive migration from schema v5 to v6.
         *
         * Creates the `emotion_logs` table.  Column types and nullability match
         * the [EmotionLog] entity definition exactly, so Room's schema validator
         * will not complain.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `emotion_logs` (
                        `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` TEXT    NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `emotion`   TEXT    NOT NULL,
                        `intensity` TEXT    NOT NULL,
                        `confidence` REAL   NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_emotion_logs_sessionId` ON `emotion_logs` (`sessionId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_emotion_logs_messageId` ON `emotion_logs` (`messageId`)"
                )
            }
        }

        /**
         * Non-destructive migration from schema v6 to v7.
         *
         * Creates the `session_summaries` table required by Phase 4 local memory.
         * No existing data in `chat_messages` or `emotion_logs` is modified.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_summaries` (
                        `sessionId`       TEXT    PRIMARY KEY NOT NULL,
                        `date`            INTEGER NOT NULL,
                        `dominantEmotion` TEXT    NOT NULL,
                        `keyThemes`       TEXT    NOT NULL,
                        `helperPattern`   TEXT    NOT NULL,
                        `summaryText`     TEXT    NOT NULL,
                        `tokenCount`      INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // ── Singleton accessor ────────────────────────────────────────────────

        /**
         * Returns the singleton [ChatDatabase] instance, creating it if needed.
         *
         * Migration strategy:
         *  - MIGRATION_5_6 handles the v5→v6 transition non-destructively.
         *  - MIGRATION_6_7 handles the v6→v7 transition non-destructively.
         *  - fallbackToDestructiveMigration() handles any other un-bridged gap
         *    (e.g. fresh installs on very old schema versions in dev builds).
         */
        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
