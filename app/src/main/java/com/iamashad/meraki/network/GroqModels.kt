package com.iamashad.meraki.network

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Request
// ---------------------------------------------------------------------------

/**
 * Top-level request body sent to POST /openai/v1/chat/completions.
 *
 * @param model       Groq model ID, e.g. "llama3-8b-8192" or "mixtral-8x7b-32768".
 * @param messages    Ordered list of conversation turns.
 * @param stream      When true the server sends Server-Sent Events instead of a
 *                    single JSON response.  Defaults to false (standard mode).
 * @param temperature Sampling temperature in [0.0, 2.0].  Higher = more creative.
 */
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val stream: Boolean = false,
    val temperature: Double = 0.7
)

/**
 * A single conversation turn.
 *
 * @param role    One of "system", "user", or "assistant".
 * @param content The text content of the message.
 */
data class GroqMessage(
    val role: String,
    val content: String
)

// ---------------------------------------------------------------------------
// Standard (non-streaming) response
// ---------------------------------------------------------------------------

/**
 * Root response object returned when [GroqChatRequest.stream] is false.
 *
 * @param id      Unique completion identifier.
 * @param choices List of generated completions (usually one element).
 * @param usage   Token-usage accounting; may be null on certain error paths.
 */
data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChoice>,
    val usage: GroqUsage?
)

/**
 * One generated candidate inside a standard response.
 *
 * @param message      The assistant's reply.
 * @param finishReason Why generation stopped ("stop", "length", etc.).
 */
data class GroqChoice(
    val message: GroqMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

/**
 * Token accounting reported by the Groq API.
 */
data class GroqUsage(
    @SerializedName("prompt_tokens")     val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens")      val totalTokens: Int
)

// ---------------------------------------------------------------------------
// Streaming response (Server-Sent Events)
// ---------------------------------------------------------------------------

/**
 * Each SSE data-line is deserialised into this object when
 * [GroqChatRequest.stream] is true.
 *
 * @param id      Completion identifier (same across all chunks in one request).
 * @param choices List of delta chunks (usually one element).
 */
data class GroqStreamResponse(
    val id: String,
    val choices: List<GroqStreamChoice>
)

/**
 * One chunk inside a streaming response.
 *
 * @param delta        Incremental content produced in this chunk.
 * @param finishReason Non-null on the final chunk ("stop", "length", …).
 */
data class GroqStreamChoice(
    val delta: GroqDelta,
    @SerializedName("finish_reason") val finishReason: String?
)

/**
 * The incremental text fragment carried by a streaming chunk.
 * Content is null on the very first chunk (role-only delta).
 */
data class GroqDelta(
    val content: String?
)
