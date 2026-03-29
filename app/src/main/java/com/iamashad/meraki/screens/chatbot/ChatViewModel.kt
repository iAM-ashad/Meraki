package com.iamashad.meraki.screens.chatbot

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Phase 5: firebase-ai (Firebase AI Logic) replaces deprecated com.google.ai.client.generativeai.
// startChat() / sendMessage() / response.text API surface is identical — only imports change.
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.utils.analyzeEmotion
import com.iamashad.meraki.utils.gradientMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Phase 2: UDF — single immutable state class replacing dispersed mutableStateOf/mutableStateListOf.
data class ChatUiState(
    val activeContext: String = "neutral",
    val messages: List<Message> = emptyList(),
    val isTyping: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * ViewModel to manage chatbot state, messages, and interactions.
 * Handles message sending, context tagging, history loading and emotion-based feedback.
 * Phase 2: all state is consolidated into a single MutableStateFlow<ChatUiState>.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val generativeModel: GenerativeModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // Checks if there are any previously saved messages
    suspend fun hasPreviousConversation(): Boolean {
        return chatRepository.getLastContext(userId) != null
    }

    // Initializes active context based on previous session or default
    fun initializeContext(userId: String) {
        viewModelScope.launch {
            val lastContext = chatRepository.getLastContext(userId)
            _uiState.update { it.copy(activeContext = lastContext ?: "neutral") }
            Log.d("ChatViewModel", "Active context initialized: ${_uiState.value.activeContext}")
        }
    }

    // Loads previous conversation from repository and populates messages.
    // Phase 3: getAllMessages() now returns Flow — use .first() for a one-shot snapshot.
    fun loadPreviousConversation() {
        viewModelScope.launch {
            val chatHistory = chatRepository.getAllMessages(userId).first()
            val messages = chatHistory.map { Message(it.message, it.role) }.distinct()
            val lastContext = chatRepository.getLastContext(userId) ?: "neutral"
            _uiState.update { it.copy(messages = messages, activeContext = lastContext) }
        }
    }

    // Begins a new session and generates a friendly greeting
    fun startNewConversation() {
        viewModelScope.launch {
            val lastContext = chatRepository.getLastContext(userId)
            val activeContext = lastContext ?: "neutral"
            val userName = FirebaseAuth.getInstance().currentUser?.displayName
            val firstName = userName?.split(" ")?.firstOrNull()

            val greetingMessage = if (lastContext != null) {
                "Hi $firstName! Last time we talked about $lastContext. How are you doing now?"
            } else {
                "Hello $firstName! How can I help you today?"
            }

            val botMessage = Message(greetingMessage, "model")
            _uiState.update { it.copy(messages = listOf(botMessage), activeContext = activeContext) }
            storeMessageInDatabase(botMessage)
        }
    }

    // Tags the last message with a user-defined context
    fun finishConversation(tag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeContext = tag) }
            val lastMessage = _uiState.value.messages.lastOrNull() ?: return@launch
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
            _uiState.update { it.copy(messages = it.messages + message) }
            storeMessageInDatabase(message)

            if (role == "user") {
                val newContext = analyzeEmotion(messageText)
                _uiState.update { it.copy(activeContext = newContext, isTyping = true) }

                // Snapshot messages before the API call to build stable history
                val historySnapshot = _uiState.value.messages
                val response = generativeModel.startChat(
                    history = historySnapshot.map { content(it.role) { text(it.message) } }
                ).sendMessage(messageText)

                val botMessage = Message(response.text.toString(), "model")
                _uiState.update { it.copy(isTyping = false, messages = it.messages + botMessage) }
                storeMessageInDatabase(botMessage)
            }
        }
    }

    // Clears message list and history in the database
    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory(userId)
            _uiState.update { it.copy(messages = emptyList(), activeContext = "") }
        }
    }

    // Returns a background gradient list based on current mood/context
    fun determineGradientColors(): List<Color> {
        return gradientMap[_uiState.value.activeContext] ?: gradientMap["neutral"]!!
    }
}
