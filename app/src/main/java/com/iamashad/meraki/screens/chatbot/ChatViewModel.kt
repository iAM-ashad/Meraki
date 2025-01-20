package com.iamashad.meraki.screens.chatbot

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.utils.analyzeEmotion
import com.iamashad.meraki.utils.gradientMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val generativeModel: GenerativeModel,
    private val auth: FirebaseAuth
) : ViewModel() {

    var activeContext by mutableStateOf("neutral")
        private set

    private val userId: String = auth.currentUser?.uid.orEmpty()
    private var cachedContext: String? = null
    private var cachedChatHistory: List<Message>? = null

    private val _messageList = mutableStateListOf<Message>()
    val messageList: List<Message> get() = _messageList

    var isTyping = mutableStateOf(false)
        private set

    private val preloadedGradients = gradientMap.mapValues { it.value }

    init {
        viewModelScope.launch {
            cachedContext = chatRepository.getLastContext(userId)
            cachedChatHistory = chatRepository.getAllMessages(userId).map {
                Message(it.message, it.role)
            }
            activeContext = cachedContext ?: "neutral"
            Log.d("ChatViewModel", "Preloaded data for user $userId")
        }
    }

    suspend fun hasPreviousConversation(): Boolean = withContext(Dispatchers.IO) {
        chatRepository.getLastContext(userId) != null
    }

    fun initializeContext() {
        viewModelScope.launch {
            if (cachedContext == null) {
                cachedContext = chatRepository.getLastContext(userId)
            }
            activeContext = cachedContext ?: "neutral"
            Log.d("ChatViewModel", "Active context initialized: $activeContext")
        }
    }

    fun loadPreviousConversation() {
        viewModelScope.launch {
            if (cachedChatHistory == null) {
                val chatHistory = chatRepository.getAllMessages(userId)
                cachedChatHistory = chatHistory.map { Message(it.message, it.role) }
            }

            _messageList.clear()
            cachedChatHistory?.let { _messageList.addAll(it) }
            activeContext = cachedContext ?: "neutral"
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            _messageList.clear()
            initializeContext()

            val userName = auth.currentUser?.displayName
            val firstName = userName?.split(" ")?.firstOrNull()

            val greetingMessage = if (activeContext.isNotEmpty()) {
                "Hi $firstName! Last time we talked about $activeContext. How are you doing now?"
            } else {
                "Hello $firstName! How can I help you today?"
            }

            val botMessage = Message(greetingMessage, "model")
            _messageList.add(botMessage)
            storeMessageInDatabase(botMessage)
        }
    }

    fun finishConversation(tag: String) {
        viewModelScope.launch {
            activeContext = tag
            cachedContext = tag

            val lastMessage = _messageList.lastOrNull() ?: return@launch
            val chatMessage = ChatMessage(
                message = lastMessage.message,
                role = lastMessage.role,
                context = tag,
                userId = userId
            )
            chatRepository.insertMessage(chatMessage)
        }
    }

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

    fun sendMessage(messageText: String, role: String = "user") {
        viewModelScope.launch {
            val message = Message(messageText, role)
            _messageList.add(message)
            storeMessageInDatabase(message)

            if (role == "user") {
                activeContext = analyzeEmotion(messageText)
                isTyping.value = true

                try {
                    val recentMessages = _messageList.takeLast(10)
                    val response = generativeModel.startChat(
                        history = recentMessages.map {
                            com.google.ai.client.generativeai.type.content(it.role) {
                                text(it.message)
                            }
                        }
                    ).sendMessage(messageText)

                    isTyping.value = false

                    val botMessage = Message(response.text.toString(), "model")
                    _messageList.add(botMessage)
                    storeMessageInDatabase(botMessage)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error generating response", e)
                    val errorMessage = Message(
                        "Sorry, I couldn't process that. Please try again.",
                        "model"
                    )
                    _messageList.add(errorMessage)
                    storeMessageInDatabase(errorMessage)
                }
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory(userId)
            _messageList.clear()
            activeContext = "neutral"
            cachedContext = null
            cachedChatHistory = null
        }
    }

    fun determineGradientColors(): List<Color> {
        return preloadedGradients[activeContext] ?: preloadedGradients["neutral"]!!
    }
}
