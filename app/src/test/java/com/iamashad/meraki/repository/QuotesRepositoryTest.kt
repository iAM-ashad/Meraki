package com.iamashad.meraki.repository

import com.google.common.truth.Truth.assertThat
import com.iamashad.meraki.network.QuotesAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Unit tests for [QuotesRepository] using [MockWebServer] to simulate
 * real HTTP responses without hitting the network.
 *
 * The Retrofit instance is built with the mock server's base URL so all
 * requests are intercepted locally.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuotesRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var quotesApi: QuotesAPI
    private lateinit var repository: QuotesRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        quotesApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
            .create(QuotesAPI::class.java)

        repository = QuotesRepository(quotesApi, testDispatcher)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ── getRandomQuote — success path ─────────────────────────────────────────

    @Test
    fun `getRandomQuote - parses author and quote from JSON response`() = runTest(testDispatcher) {
        val json = """{"author":"Marcus Aurelius","quote":"The impediment to action advances action."}"""
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

        val result = repository.getRandomQuote()

        assertThat(result.author).isEqualTo("Marcus Aurelius")
        assertThat(result.quote).isEqualTo("The impediment to action advances action.")
    }

    @Test
    fun `getRandomQuote - returns correct Quotes object on 200 OK`() = runTest(testDispatcher) {
        val json = """{"author":"Seneca","quote":"Luck is what happens when preparation meets opportunity."}"""
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

        val result = repository.getRandomQuote()

        assertThat(result.author).isEqualTo("Seneca")
        assertThat(result.quote).contains("preparation meets opportunity")
    }

    @Test
    fun `getRandomQuote - request hits the correct endpoint`() = runTest(testDispatcher) {
        val json = """{"author":"A","quote":"Q"}"""
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

        repository.getRandomQuote()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/quote")
        assertThat(recordedRequest.method).isEqualTo("GET")
    }

    @Test
    fun `getRandomQuote - author field is preserved exactly as received`() = runTest(testDispatcher) {
        val json = """{"author":"Albert Einstein","quote":"Imagination is more important than knowledge."}"""
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

        val result = repository.getRandomQuote()

        assertThat(result.author).isEqualTo("Albert Einstein")
    }

    @Test
    fun `getRandomQuote - handles empty author gracefully`() = runTest(testDispatcher) {
        val json = """{"author":"","quote":"Anonymous wisdom."}"""
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

        val result = repository.getRandomQuote()

        assertThat(result.author).isEmpty()
        assertThat(result.quote).isEqualTo("Anonymous wisdom.")
    }

    // ── getRandomQuote — error paths ──────────────────────────────────────────

    @Test
    fun `getRandomQuote - throws on 500 server error`() = runTest(testDispatcher) {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val exception = runCatching { repository.getRandomQuote() }.exceptionOrNull()

        assertThat(exception).isNotNull()
    }

    @Test
    fun `getRandomQuote - throws on 404 not found`() = runTest(testDispatcher) {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val exception = runCatching { repository.getRandomQuote() }.exceptionOrNull()

        assertThat(exception).isNotNull()
    }

    @Test
    fun `getRandomQuote - throws on malformed JSON response`() = runTest(testDispatcher) {
        mockWebServer.enqueue(MockResponse().setBody("not-json").setResponseCode(200))

        val exception = runCatching { repository.getRandomQuote() }.exceptionOrNull()

        assertThat(exception).isNotNull()
    }

    @Test
    fun `getRandomQuote - throws on connection failure`() = runTest(testDispatcher) {
        // Shut down the server to simulate a connection refused error
        mockWebServer.shutdown()

        val exception = runCatching { repository.getRandomQuote() }.exceptionOrNull()

        assertThat(exception).isNotNull()

        // Restart so @After tearDown doesn't double-throw
        mockWebServer = MockWebServer()
    }
}
