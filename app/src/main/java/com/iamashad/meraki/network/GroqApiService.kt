package com.iamashad.meraki.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Retrofit interface for the Groq Cloud chat-completions endpoint.
 *
 * Base URL: https://api.groq.com/
 *
 * Both functions target the same OpenAI-compatible path; they differ only in how
 * the response is consumed:
 *  - [chatCompletion]       → full JSON body, standard suspend fun.
 *  - [chatCompletionStream] → raw [ResponseBody] so callers can read Server-Sent
 *                             Events line-by-line and emit them as a Flow<String>.
 *
 * The Authorization header is intentionally per-call (not baked into the
 * OkHttpClient interceptor) so that future multi-key or per-user token scenarios
 * work without re-creating the client.  The NetworkModule interceptor adds a
 * fallback User-Agent; the Bearer token is the sole auth signal required by Groq.
 */
interface GroqApiService {

    /**
     * Sends a standard (non-streaming) chat-completion request.
     *
     * Set [GroqChatRequest.stream] to `false` (the default) when calling this
     * function.  The entire assistant response is returned in one JSON payload.
     *
     * @param bearerToken  Full "Bearer <key>" string, e.g. "Bearer gsk_…".
     * @param request      Request body including model, messages, and parameters.
     * @return             Wrapped [GroqChatResponse]; call [Response.body] to access it.
     *                     HTTP error codes surface as [Response.errorBody].
     */
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>

    /**
     * Sends a streaming chat-completion request.
     *
     * Set [GroqChatRequest.stream] to `true` before calling this function.
     * The raw [ResponseBody] must be read on a background thread; each line that
     * starts with "data: " carries a JSON-encoded [GroqStreamResponse] chunk.
     * The final chunk contains the literal string "[DONE]" instead of JSON.
     *
     * Typical usage pattern (in a repository or use-case):
     * ```kotlin
     * val body = groqApiService.chatCompletionStream("Bearer $key", request)
     * flow {
     *     body.source().use { source ->
     *         while (!source.exhausted()) {
     *             val line = source.readUtf8Line() ?: break
     *             if (line.startsWith("data: ") && !line.contains("[DONE]")) {
     *                 emit(line.removePrefix("data: "))
     *             }
     *         }
     *     }
     * }.flowOn(Dispatchers.IO)
     * ```
     *
     * @param bearerToken  Full "Bearer <key>" string.
     * @param request      Request body with [GroqChatRequest.stream] set to `true`.
     * @return             Raw [ResponseBody] containing the SSE byte stream.
     */
    @Streaming
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletionStream(
        @Header("Authorization") bearerToken: String,
        @Body request: GroqChatRequest
    ): ResponseBody
}
