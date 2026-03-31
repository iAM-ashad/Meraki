package com.iamashad.meraki.repository

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.iamashad.meraki.BuildConfig
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.network.GroqApiService
import com.iamashad.meraki.network.GroqChatRequest
import com.iamashad.meraki.network.GroqMessage
import com.iamashad.meraki.network.GroqStreamResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [GroqRepository].
 *
 * Responsibilities:
 *  1. Build the Groq message list from the high-level parameters supplied by the
 *     caller (Phase 3.5 prompt-engineering conventions).
 *  2. Call [GroqApiService.chatCompletionStream] with `stream = true`.
 *  3. Parse each SSE line into a [GroqStreamResponse] and emit the delta text.
 *  4. Detect the `[DONE]` sentinel and close the flow gracefully.
 *  5. Convert [HttpException] (especially 429 Too Many Requests) and [IOException]
 *     into human-readable strings emitted into the same flow so the UI can display
 *     them inline.
 *
 * All blocking I/O runs on the injected [ioDispatcher] via [Flow.flowOn].
 *
 * @param apiService     Retrofit interface for the Groq Cloud completions endpoint.
 * @param ioDispatcher   Dispatcher for off-main-thread I/O (injected as [@IoDispatcher]).
 */
@Singleton
class GroqRepositoryImpl @Inject constructor(
    private val apiService: GroqApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GroqRepository {

    // Reuse a single Gson instance — it is thread-safe.
    private val gson = Gson()

    // -----------------------------------------------------------------------
    // GroqRepository implementation
    // -----------------------------------------------------------------------

    override fun sendMessageStream(
        systemPrompt: String,
        history: List<Message>,
        userMessage: String,
        userProfile: String,
        emotionContext: String
    ): Flow<String> = flow {

        // -------------------------------------------------------------------
        // 1. Build the Groq message list
        // -------------------------------------------------------------------

        // System turn: base persona + user-profile context.
        // The profile is appended under a clearly labelled section so the model
        // can easily reference it without confusing it with the conversation.
        val systemContent = buildString {
            append(systemPrompt.trim())
            if (userProfile.isNotBlank()) {
                append("\n\n## User Profile\n")
                append(userProfile.trim())
            }
        }

        val messages = buildList<GroqMessage> {
            // Always first: system turn.
            add(GroqMessage(role = ROLE_SYSTEM, content = systemContent))

            // Historical turns in chronological order.
            // Map "model" (Gemini/Firebase convention) → "assistant" (OpenAI/Groq convention).
            history.forEach { msg ->
                val groqRole = if (msg.role == "model") "assistant" else msg.role
                add(GroqMessage(role = groqRole, content = msg.message))
            }

            // Current user turn: append emotion context if present so the model
            // can adapt its empathetic tone without requiring a separate system
            // message update mid-session.
            val userContent = buildString {
                append(userMessage.trim())
                if (emotionContext.isNotBlank()) {
                    append("\n\n[Detected emotional context: ${emotionContext.trim()}]")
                }
            }
            add(GroqMessage(role = ROLE_USER, content = userContent))
        }

        // -------------------------------------------------------------------
        // 2. Fire the streaming request
        // -------------------------------------------------------------------

        val request = GroqChatRequest(
            model = GROQ_MODEL,
            messages = messages,
            stream = true,
            temperature = GROQ_TEMPERATURE
        )

        try {
            val responseBody = apiService.chatCompletionStream(
                bearerToken = "Bearer ${BuildConfig.GROQ_API_KEY}",
                request = request
            )

            // -----------------------------------------------------------------
            // 3. Parse SSE stream line by line
            // -----------------------------------------------------------------

            responseBody.charStream().buffered().use { reader ->
                for (line in reader.lineSequence()) {
                    currentCoroutineContext().ensureActive()

                    if (line.startsWith(SSE_DATA_PREFIX)) {
                        val payload = line.removePrefix(SSE_DATA_PREFIX)

                        if (payload.trimEnd() == SSE_DONE_SIGNAL) break

                        try {
                            val chunk = gson.fromJson(payload, GroqStreamResponse::class.java)
                            chunk.choices
                                .firstOrNull()
                                ?.delta
                                ?.content
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { emit(it) }

                        } catch (_: JsonSyntaxException) {
                            // ignore
                        }
                    }
                }
            }

        } catch (e: HttpException) {
            // -------------------------------------------------------------------
            // 4. HTTP-level error handling
            // -------------------------------------------------------------------
            val errorMessage = when (e.code()) {
                HTTP_TOO_MANY_REQUESTS -> ERROR_RATE_LIMITED
                HTTP_UNAUTHORIZED -> ERROR_UNAUTHORIZED
                HTTP_SERVER_ERROR -> ERROR_SERVER
                else -> "$ERROR_API_FAILURE (HTTP ${e.code()})"
            }
            emit(errorMessage)

        } catch (_: IOException) {
            // -------------------------------------------------------------------
            // 5. Network / IO failure
            // -------------------------------------------------------------------
            emit(ERROR_NETWORK)
        }

    }.flowOn(ioDispatcher) // All blocking I/O stays off the main thread.

    // -----------------------------------------------------------------------
    // GroqRepository — non-streaming implementation
    // -----------------------------------------------------------------------

    override suspend fun generateSimpleResponse(
        prompt: String,
        temperature: Float
    ): String? = kotlinx.coroutines.withContext(ioDispatcher) {
        try {
            val request = GroqChatRequest(
                model = GROQ_MODEL,
                messages = listOf(GroqMessage(role = ROLE_USER, content = prompt)),
                stream = false,
                temperature = temperature.toDouble()
            )
            val response = apiService.chatCompletion(
                bearerToken = "Bearer ${BuildConfig.GROQ_API_KEY}",
                request = request
            )
            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {

        // Groq model used for chat completions.
        // llama-3.3-70b-versatile is a large, high-quality model that produces
        // natural, nuanced responses — well-suited for an empathetic companion.
        // Groq's hardware still delivers fast streaming despite the model size.
        private const val GROQ_MODEL = "llama-3.3-70b-versatile"

        // Sampling temperature: 0.7 gives a good balance between creativity
        // and coherence for an empathetic mental-health companion.
        private const val GROQ_TEMPERATURE = 0.7

        // Message roles (OpenAI / Groq convention).
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"

        // Server-Sent Events protocol constants.
        private const val SSE_DATA_PREFIX = "data: "
        private const val SSE_DONE_SIGNAL = "[DONE]"

        // HTTP status codes for error differentiation.
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_SERVER_ERROR = 500

        // --- Error strings emitted into the flow ---
        // Public so that callers (e.g. ChatViewModel) can check identity for
        // special handling such as showing a "slow down" UI cue.

        /** Emitted when Groq returns HTTP 429 — rate limit exceeded. */
        const val ERROR_RATE_LIMITED =
            "⚠️ You're sending messages too quickly. Please wait a moment and try again."

        /** Emitted when the API key is invalid or missing (HTTP 401). */
        const val ERROR_UNAUTHORIZED =
            "⚠️ AI service authentication failed. Please check the API configuration."

        /** Emitted on HTTP 500 or other server-side failures. */
        const val ERROR_SERVER =
            "⚠️ The AI service is temporarily unavailable. Please try again shortly."

        /** Emitted on any other non-2xx HTTP response. */
        const val ERROR_API_FAILURE =
            "⚠️ The AI service encountered an unexpected error"

        /** Emitted on connectivity or socket failures. */
        const val ERROR_NETWORK =
            "⚠️ Network error. Please check your connection and try again."
    }
}
