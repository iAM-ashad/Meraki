package com.iamashad.meraki.repository

import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    fun getChatMessagesFlow(userId: String): Flow<List<ChatMessage>> =
        chatDao.getAllMessagesFlow(userId)

    suspend fun insertMessage(chatMessage: ChatMessage) {
        chatDao.insertMessage(chatMessage)
    }

    suspend fun clearChatHistory(userId: String) {
        chatDao.clearChatHistory(userId)
    }

    suspend fun getLastContext(userId: String): String? {
        return chatDao.getAllMessages(userId)
            .lastOrNull { !it.context.isNullOrBlank() }
            ?.context
    }

    suspend fun getAllMessages(userId: String): List<ChatMessage> {
        return chatDao.getAllMessages(userId).distinctBy { it.id }
    }
}

