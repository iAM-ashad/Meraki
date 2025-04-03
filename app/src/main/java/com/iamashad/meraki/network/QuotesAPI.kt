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
     * Endpoint: GET /quote
     * @return A [Quotes] object containing the quote and its author.
     */
    @GET("quote")
    suspend fun getRandomQuote(): Quotes
}
