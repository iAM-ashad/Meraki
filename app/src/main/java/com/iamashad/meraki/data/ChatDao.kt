package com.iamashad.meraki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    suspend fun getAllMessages(): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(chatMessage: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}
