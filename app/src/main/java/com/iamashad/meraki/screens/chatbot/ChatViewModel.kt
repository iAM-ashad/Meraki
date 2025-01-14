package com.iamashad.meraki.screens.chatbot

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.utils.analyzeEmotion
import com.iamashad.meraki.utils.gradientMap
import com.iamashad.meraki.utils.provideGenerativeModel
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository
    var activeContext: String? = null
    private val userId: String =
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty() // Add userId here

    init {
        val chatDao = ChatDatabase.getInstance(application).chatDao()
        chatRepository = ChatRepository(chatDao)
    }

    val messageList = mutableStateListOf<Message>()

    val generativeModel: GenerativeModel = provideGenerativeModel(
        apiKey = "AIzaSyDJm4lS9PSG83ximY7bX0JFk1epNQQtyZA"
    )

    var isTyping = mutableStateOf(false)
        private set

    fun startNewConversation() {
        viewModelScope.launch {
            messageList.clear()
            activeContext =
                chatRepository.getLastContext(userId) // Pass userId to fetch user-specific context

            val userName = FirebaseAuth.getInstance().currentUser?.displayName
            val firstName = userName?.split(" ")?.firstOrNull()

            val greetingMessage = if (activeContext != null) {
                "Hi $firstName! Last time we talked about $activeContext. How are you doing now?"
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
            chatRepository.clearChatHistory(userId) // Clear only for the current user
            messageList.clear()
            activeContext = null
        }
    }

    fun determineGradientColors(): List<Color> {
        return gradientMap[activeContext] ?: gradientMap["neutral"]!!
    }
}

