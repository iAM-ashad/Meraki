package com.iamashad.meraki.repository

import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    val chatMessagesFlow: Flow<List<ChatMessage>> = chatDao.getAllMessagesFlow()

    suspend fun insertMessage(chatMessage: ChatMessage) {
        chatDao.insertMessage(chatMessage) // Already executed on IO in ViewModel
    }

    suspend fun clearChatHistory() {
        chatDao.clearChatHistory()
    }

    suspend fun getLastContext(): String? {
        // Fetch the last message with a non-null context
        return chatDao.getAllMessages()
            .lastOrNull { !it.context.isNullOrBlank() }
            ?.context
    }
}
