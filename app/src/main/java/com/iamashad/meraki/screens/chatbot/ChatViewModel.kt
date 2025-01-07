package com.iamashad.meraki.screens.chatbot

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
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

    init {
        val chatDao = ChatDatabase.getInstance(application).chatDao()
        chatRepository = ChatRepository(chatDao)
    }

    val messageList = mutableStateListOf<Message>()

    val generativeModel: GenerativeModel = provideGenerativeModel(
        apiKey = "AIzaSyDJm4lS9PSG83ximY7bX0JFk1epNQQtyZA"
    )

    fun startNewConversation() {
        viewModelScope.launch {
            messageList.clear()
            activeContext = chatRepository.getLastContext()

            val userName = FirebaseAuth.getInstance().currentUser?.displayName

            val greetingMessage = if (activeContext != null) {
                "Hi $userName! Last time we talked about $activeContext. How are you doing now?"
            } else {
                "Hello $userName! How can I help you today?"
            }

            val botMessage = Message(greetingMessage, "model")
            messageList.add(botMessage)
            storeMessageInDatabase(botMessage)
        }
    }

    fun finishConversation(tag: String) {
        viewModelScope.launch {
            activeContext = tag // Save the tag as context
            val lastMessage = messageList.lastOrNull() ?: return@launch
            val chatMessage = ChatMessage(
                message = lastMessage.message,
                role = lastMessage.role,
                context = tag
            )
            chatRepository.insertMessage(chatMessage)
        }
    }

    private fun storeMessageInDatabase(message: Message) {
        viewModelScope.launch {
            chatRepository.insertMessage(ChatMessage(message = message.message, role = message.role))
        }
    }

    fun sendMessage(messageText: String, role: String = "user") {
        viewModelScope.launch {
            val message = Message(messageText, role)
            messageList.add(message)
            storeMessageInDatabase(message)

            if (role == "user") {
                activeContext = analyzeEmotion(messageText) // Update context based on input
                val response = generativeModel.startChat(
                    history = messageList.map {
                        content(it.role) { text(it.message) }
                    }
                ).sendMessage(messageText)

                val botMessage = Message(response.text.toString(), "model")
                messageList.add(botMessage)
                storeMessageInDatabase(botMessage)
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory()
            messageList.clear()
            activeContext = null
        }
    }

    fun determineGradientColors(): List<Color> {
        return gradientMap[activeContext] ?: gradientMap["neutral"]!!
    }

}
