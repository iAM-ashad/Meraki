package com.iamashad.meraki.network

import com.iamashad.meraki.model.Quotes
import retrofit2.http.GET

/**
 * Retrofit interface for accessing quote-related API endpoints.
 */
interface QuotesAPI {

    /**
     * Fetches a random motivational or inspirational quote from the API.
     *
     * Endpoint: GET /api/random (ZenQuotes)
     * @return A single-element list containing a [Quotes] object with the quote and its author.
     */
    @GET("api/random")
    suspend fun getRandomQuote(): List<Quotes>
}
