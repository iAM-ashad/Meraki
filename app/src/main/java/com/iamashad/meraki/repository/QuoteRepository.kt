package com.iamashad.meraki.repository

import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.Quotes
import com.iamashad.meraki.network.QuotesAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository class responsible for fetching motivational quotes
 * from the remote Quotes API.
 *
 * Phase 3: getRandomQuote() is wrapped in withContext(ioDispatcher) for
 * explicit main-safety and testability with injected test dispatchers.
 * Retrofit 2.6+ suspend functions are already non-blocking, but the
 * explicit withContext documents the intent and simplifies unit testing.
 */
class QuotesRepository @Inject constructor(
    private val quotesApi: QuotesAPI,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Suspends and retrieves a random quote from the API.
     *
     * @return A [Quotes] object containing the quote and its author.
     */
    suspend fun getRandomQuote(): Quotes = withContext(ioDispatcher) {
        quotesApi.getRandomQuote()
    }
}
