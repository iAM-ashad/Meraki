package com.iamashad.meraki.screens.chatbot

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.utils.analyzeEmotion
import com.iamashad.meraki.utils.gradientMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel to manage chatbot state, messages, and interactions
 * Handles message sending, context tagging, history loading and emotion-based feedback
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val generativeModel: GenerativeModel
) : ViewModel() {

    // Contextual label based on emotional tone or user input topic
    var activeContext by mutableStateOf("neutral")
        private set

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // List of chat messages shown in the UI
    val messageList = mutableStateListOf<Message>()

    // UI flag for showing typing animation
    var isTyping = mutableStateOf(false)
        private set

    // Checks if there are any previously saved messages
    suspend fun hasPreviousConversation(): Boolean {
        return chatRepository.getLastContext(userId) != null
    }

    // Initializes active context based on previous session or default
    fun initializeContext(userId: String) {
        viewModelScope.launch {
            val lastContext = chatRepository.getLastContext(userId)
            activeContext = lastContext ?: "neutral"
            Log.d("ChatViewModel", "Active context initialized: $activeContext")
        }
    }

    // Loads previous conversation from repository and populates messageList
    fun loadPreviousConversation() {
        viewModelScope.launch {
            val chatHistory = chatRepository.getAllMessages(userId)
            val newMessageList = chatHistory.map { Message(it.message, it.role) }
            messageList.clear()
            messageList.addAll(newMessageList.distinct())
            activeContext = chatRepository.getLastContext(userId)!!
        }
    }

    // Begins a new session and generates a friendly greeting
    fun startNewConversation() {
        viewModelScope.launch {
            messageList.clear()
            val lastContext = chatRepository.getLastContext(userId)
            activeContext = lastContext ?: "neutral"

            val userName = FirebaseAuth.getInstance().currentUser?.displayName
            val firstName = userName?.split(" ")?.firstOrNull()

            val greetingMessage = if (lastContext != null) {
                "Hi $firstName! Last time we talked about $lastContext. How are you doing now?"
            } else {
                "Hello $firstName! How can I help you today?"
            }

            val botMessage = Message(greetingMessage, "model")
            messageList.add(botMessage)
            storeMessageInDatabase(botMessage)
        }
    }

    // Tags the last message with a user-defined context
    fun finishConversation(tag: String) {
        viewModelScope.launch {
            activeContext = tag
            val lastMessage = messageList.lastOrNull() ?: return@launch
            val chatMessage = ChatMessage(
                message = lastMessage.message,
                role = lastMessage.role,
                context = tag,
                userId = userId
            )
            chatRepository.insertMessage(chatMessage)
        }
    }

    // Persists message in Room database
    private fun storeMessageInDatabase(message: Message) {
        viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessage(
                    message = message.message,
                    role = message.role,
                    userId = userId
                )
            )
        }
    }

    /**
     * Sends a message to the model and retrieves a generated response.
     * Also updates emotional context based on user input.
     */
    fun sendMessage(messageText: String, role: String = "user") {
        viewModelScope.launch {
            val message = Message(messageText, role)
            messageList.add(message)
            storeMessageInDatabase(message)

            if (role == "user") {
                activeContext = analyzeEmotion(messageText) // Update emotion context
                isTyping.value = true

                val response = generativeModel.startChat(history = messageList.map {
                    content(it.role) { text(it.message) }
                }).sendMessage(messageText)

                isTyping.value = false

                val botMessage = Message(response.text.toString(), "model")
                messageList.add(botMessage)
                storeMessageInDatabase(botMessage)
            }
        }
    }

    // Clears message list and history in the database
    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory(userId)
            messageList.clear()
            activeContext = ""
        }
    }

    // Returns a background gradient list based on current mood/context
    fun determineGradientColors(): List<Color> {
        return gradientMap[activeContext] ?: gradientMap["neutral"]!!
    }
}
