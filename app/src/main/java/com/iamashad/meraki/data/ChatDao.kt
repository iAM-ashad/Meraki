package com.iamashad.meraki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface for managing chat message data
 * in the Room database.
 */
@Dao
interface ChatDao {

    /**
     * Returns a Flow of chat messages for the given user ID.
     * This allows reactive data updates using Kotlin coroutines.
     *
     * @param userId The ID of the user whose messages are being fetched.
     * @return A Flow emitting the list of ChatMessage objects.
     */
    @Query("SELECT DISTINCT * FROM chat_messages WHERE userId = :userId ORDER BY id ASC")
    fun getAllMessagesFlow(userId: String): Flow<List<ChatMessage>>

    /**
     * Fetches all chat messages for a specific user in a suspending context.
     * This is useful for one-time data reads.
     *
     * @param userId The ID of the user whose messages are being fetched.
     * @return A list of ChatMessage objects.
     */
    @Query("SELECT DISTINCT * FROM chat_messages WHERE userId = :userId ORDER BY id ASC")
    suspend fun getAllMessages(userId: String): List<ChatMessage>

    /**
     * Inserts a new chat message into the database.
     * If a conflict occurs (e.g., duplicate primary key), the existing record is replaced.
     *
     * @param chatMessage The message to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(chatMessage: ChatMessage)

    /**
     * Deletes all chat messages for the given user ID.
     *
     * @param userId The ID of the user whose chat history is to be cleared.
     */
    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatHistory(userId: String)
}
