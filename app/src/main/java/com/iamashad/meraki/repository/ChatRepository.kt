package com.iamashad.meraki.repository

import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository class that provides an abstraction layer over ChatDao.
 * Handles chat-related data operations using Room database.
 *
 * Phase 3: All operations are main-safe.
 * - getAllMessages() returns Flow<List<ChatMessage>> for reactive data.
 * - Suspend functions are wrapped in withContext(ioDispatcher).
 * - ioDispatcher is injected via Hilt for testability.
 *
 * Note: @Inject constructor removed — this class is provided as a singleton
 * by DatabaseModule.provideChatRepository to avoid a duplicate binding with
 * the @Provides method.
 */
class ChatRepository(
    private val chatDao: ChatDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Inserts a chat message into the local database.
     */
    suspend fun insertMessage(chatMessage: ChatMessage) = withContext(ioDispatcher) {
        chatDao.insertMessage(chatMessage)
    }

    /**
     * Deletes all chat messages for a specific user.
     */
    suspend fun clearChatHistory(userId: String) = withContext(ioDispatcher) {
        chatDao.clearChatHistory(userId)
    }

    /**
     * Retrieves the last non-null, non-blank context message for a user.
     * Phase 3: uses getAllMessages().first() to avoid a separate DAO query.
     */
    suspend fun getLastContext(userId: String): String? = withContext(ioDispatcher) {
        getAllMessages(userId).first()
            .lastOrNull { !it.context.isNullOrBlank() }
            ?.context
    }

    /**
     * Returns a Flow of all unique chat messages for the specified user.
     * Phase 3: replaces the one-shot suspend List return for better reactivity.
     * Room automatically emits updates whenever the underlying data changes.
     */
    fun getAllMessages(userId: String): Flow<List<ChatMessage>> {
        return chatDao.getAllMessagesFlow(userId)
            .map { messages -> messages.distinctBy { it.id } }
            .flowOn(ioDispatcher)
    }
}
