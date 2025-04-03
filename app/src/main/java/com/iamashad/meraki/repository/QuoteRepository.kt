package com.iamashad.meraki.repository

import com.iamashad.meraki.model.Quotes
import com.iamashad.meraki.network.QuotesAPI
import javax.inject.Inject

/**
 * Repository class responsible for fetching motivational quotes
 * from the remote Quotes API.
 */
class QuotesRepository @Inject constructor(
    private val quotesApi: QuotesAPI
) {

    /**
     * Suspends and retrieves a random quote from the API.
     *
     * @return A [Quotes] object containing the quote and its author.
     */
    suspend fun getRandomQuote(): Quotes {
        val quotes = quotesApi.getRandomQuote()
        return quotes
    }
}
