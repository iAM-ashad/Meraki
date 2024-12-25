package com.iamashad.meraki.screens.chatbot

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.iamashad.meraki.model.Message
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    val messageList by lazy {
        mutableStateListOf<Message>()
    }

    val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = "AIzaSyDJm4lS9PSG83ximY7bX0JFk1epNQQtyZA",
        systemInstruction = content {text(" You are a professional mental health assistant trained to provide empathetic, supportive, and non-judgmental responses. Act as a compassionate therapist, focusing on the user's emotions and concerns.Provide concise yet thoughtful answers that are warm and understanding. Avoid lengthy explanations unless absolutely necessary. Offer actionable suggestions when appropriate, but prioritize listening and validating the user's feelings. Avoid asking excessive questions; instead, encourage the user to share at their own pace. Always aim to foster trust, safety, and comfort in the conversation.")}
    )

    fun sendMessage(question: String) {
        viewModelScope.launch {
            try {
                // Add user message first
                messageList.add(Message(question, "user"))
                // Add "Typing..." message
                messageList.add(Message("Typing....", "model"))

                // Make the API call
                val chat = generativeModel.startChat(
                    history = messageList.map {
                        content(it.role) { text(it.message) }
                    }.toList()
                )

                // Send the user message to Gemini and await response
                val response = chat.sendMessage(question)

                // Remove "Typing..." and add the actual model response
                messageList.removeAt(messageList.lastIndex)
                messageList.add(Message(response.text.toString(), "model"))
            } catch (e: Exception) {
                // In case of error, remove "Typing..." and show error message
                messageList.removeAt(messageList.lastIndex)
                messageList.add(Message("Error: ${e.message}", "model"))
            }
        }
    }
}

