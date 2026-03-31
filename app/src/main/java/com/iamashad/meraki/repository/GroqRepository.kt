package com.iamashad.meraki.repository

import com.iamashad.meraki.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Contract for sending chat messages to the Groq Cloud API and receiving a
 * streaming response.
 *
 * Implementations are responsible for:
 *  - Constructing the full Groq message list (system prompt, history, user turn).
 *  - Calling the underlying [com.iamashad.meraki.network.GroqApiService].
 *  - Parsing the Server-Sent Events (SSE) stream into individual text tokens.
 *  - Propagating rate-limit and network errors as human-readable strings in the
 *    same [Flow] so callers need only one collector to handle both success and
 *    error paths.
 */
interface GroqRepository {

    /**
     * Sends a message to Groq and returns a cold [Flow] that emits individual
     * text tokens as they arrive from the SSE stream.
     *
     * The flow is cold — a new API call is made for each new collector.
     * Cancelling the collector cancels the underlying network request.
     *
     * Error strings are emitted into the flow rather than thrown so that the
     * UI layer can display them inline with the assistant's response bubble.
     * Callers can detect a rate-limit error by checking whether the emitted
     * string equals [com.iamashad.meraki.repository.GroqRepositoryImpl.ERROR_RATE_LIMITED].
     *
     * @param systemPrompt   Base instructions that define the assistant's persona
     *                       and behaviour for the entire session.
     * @param history        Prior conversation turns in chronological order.
     *                       Each [Message] carries a [Message.role] of either
     *                       "user" or "assistant".
     * @param userMessage    The latest message typed by the user (not yet in
     *                       [history]).
     * @param userProfile    Serialised user-profile context that personalises the
     *                       assistant's responses (name, preferences, goals, etc.).
     * @param emotionContext A short description of the emotion detected from the
     *                       user's input, appended to the final user turn so the
     *                       model can adapt its tone accordingly.
     * @return               A [Flow] that emits successive text chunks.  The flow
     *                       completes normally when the [DONE] sentinel is received
     *                       or when the underlying stream is exhausted.
     */
    fun sendMessageStream(
        systemPrompt: String,
        history: List<Message>,
        userMessage: String,
        userProfile: String,
        emotionContext: String
    ): Flow<String>

    /**
     * Sends a single non-streaming prompt to Groq and returns the full completion
     * text, or null if the request fails or returns an empty response.
     *
     * Intended for low-latency, one-shot tasks such as session summarisation where
     * streaming is unnecessary.  The caller is responsible for running this inside
     * a coroutine; implementations should switch to an IO dispatcher internally.
     *
     * @param prompt      The user-turn text to send to the model.
     * @param temperature Sampling temperature in [0.0, 2.0].  Use lower values
     *                    (e.g. 0.3) for structured / deterministic outputs.
     * @return            The assistant's reply as a plain string, or null on error.
     */
    suspend fun generateSimpleResponse(
        prompt: String,
        temperature: Float = 0.7f
    ): String?
}
