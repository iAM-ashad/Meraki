package com.iamashad.meraki.screens.chatbot

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.iamashad.meraki.model.Message
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    val messageList by lazy { mutableStateListOf<Message>() }

    private var isPromptProcessed = false

    val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = "AIzaSyDJm4lS9PSG83ximY7bX0JFk1epNQQtyZA",
        systemInstruction = content {text(" You are a professional mental health assistant trained to provide empathetic, supportive, and non-judgmental responses. Act as a compassionate therapist, focusing on the user's emotions and concerns.Provide concise yet thoughtful answers that are warm and understanding. Avoid lengthy explanations unless absolutely necessary. Offer actionable suggestions when appropriate, but prioritize listening and validating the user's feelings. Avoid asking excessive questions; instead, encourage the user to share at their own pace. Always aim to foster trust, safety, and comfort in the conversation.")}
    )

    fun processInitialPrompt(prompt: String?) {
        if (!isPromptProcessed && !prompt.isNullOrEmpty()) {
            isPromptProcessed = true
            sendMessage(prompt)
        }
    }

    fun sendMessage(question: String) {
        viewModelScope.launch {
            try {
                messageList.add(Message(question, "user"))
                messageList.add(Message("Typing....", "model"))

                val chat = generativeModel.startChat(
                    history = messageList.map {
                        content(it.role) { text(it.message) }
                    }.toList()
                )

                val response = chat.sendMessage(question)
                messageList.removeAt(messageList.lastIndex)
                messageList.add(Message(response.text.toString(), "model"))
            } catch (e: Exception) {
                messageList.removeAt(messageList.lastIndex)
                messageList.add(Message("Error: ${e.message}", "model"))
            }
        }
    }
}


