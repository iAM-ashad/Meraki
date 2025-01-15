package com.iamashad.meraki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT DISTINCT * FROM chat_messages WHERE userId = :userId ORDER BY id ASC")
    fun getAllMessagesFlow(userId: String): Flow<List<ChatMessage>>

    @Query("SELECT DISTINCT * FROM chat_messages WHERE userId = :userId ORDER BY id ASC")
    suspend fun getAllMessages(userId: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(chatMessage: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatHistory(userId: String)
}


