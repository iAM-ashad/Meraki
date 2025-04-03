package com.iamashad.meraki.repository

import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatMessage
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Repository class that provides an abstraction layer over ChatDao.
 * Handles chat-related data operations using Room database.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {

    /**
     * Inserts a chat message into the local database.
     *
     * @param chatMessage The message to be inserted.
     */
    suspend fun insertMessage(chatMessage: ChatMessage) {
        chatDao.insertMessage(chatMessage)
    }

    /**
     * Deletes all chat messages for a specific user.
     *
     * @param userId ID of the user whose chat history should be cleared.
     */
    suspend fun clearChatHistory(userId: String) {
        chatDao.clearChatHistory(userId)
    }

    /**
     * Retrieves the last non-null and non-blank context message for a user.
     *
     * @param userId ID of the user whose last context is requested.
     * @return The last context string, or null if not found.
     */
    suspend fun getLastContext(userId: String): String? {
        return chatDao.getAllMessages(userId)
            .lastOrNull { !it.context.isNullOrBlank() }
            ?.context
    }

    /**
     * Returns a list of all unique chat messages for the specified user.
     *
     * @param userId ID of the user whose messages are being fetched.
     * @return List of distinct ChatMessage objects by ID.
     */
    suspend fun getAllMessages(userId: String): List<ChatMessage> {
        return chatDao.getAllMessages(userId).distinctBy { it.id }
    }
}
