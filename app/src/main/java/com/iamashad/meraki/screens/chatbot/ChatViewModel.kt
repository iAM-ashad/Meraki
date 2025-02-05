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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val generativeModel: GenerativeModel
) : ViewModel() {
    var activeContext by mutableStateOf("neutral") // Default value
        private set
    private val userId: String =
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val messageList = mutableStateListOf<Message>()

    var isTyping = mutableStateOf(false)
        private set

    suspend fun hasPreviousConversation(): Boolean {
        return chatRepository.getLastContext(userId) != null
    }

    fun initializeContext(userId: String) {
        viewModelScope.launch {
            val lastContext = chatRepository.getLastContext(userId)
            activeContext = lastContext ?: "neutral" // Use default if null
            Log.d("ChatViewModel", "Active context initialized: $activeContext")
        }
    }

    fun loadPreviousConversation() {
        viewModelScope.launch {
            // Fetch all messages from the repository
            val chatHistory = chatRepository.getAllMessages(userId)
            val newMessageList = chatHistory.map {
                Message(it.message, it.role)
            }

            // Clear the existing list to avoid duplication
            messageList.clear()

            // Add only unique messages
            messageList.addAll(newMessageList.distinct())
            activeContext = chatRepository.getLastContext(userId)!!
        }
    }

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


    fun finishConversation(tag: String) {
        viewModelScope.launch {
            activeContext = tag
            val lastMessage = messageList.lastOrNull() ?: return@launch
            val chatMessage = ChatMessage(
                message = lastMessage.message,
                role = lastMessage.role,
                context = tag,
                userId = userId // Associate with the current user
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
                    userId = userId // Associate with the current user
                )
            )
        }
    }

    fun sendMessage(messageText: String, role: String = "user") {
        viewModelScope.launch {
            val message = Message(messageText, role)
            messageList.add(message)
            storeMessageInDatabase(message)

            if (role == "user") {
                activeContext = analyzeEmotion(messageText)
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

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory(userId)
            messageList.clear()
            activeContext = ""
        }
    }

    fun determineGradientColors(): List<Color> {
        return gradientMap[activeContext] ?: gradientMap["neutral"]!!
    }
}

