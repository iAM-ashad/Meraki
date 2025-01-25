package com.iamashad.meraki.network

import com.iamashad.meraki.model.Quotes
import retrofit2.http.GET

interface QuotesAPI {
    @GET("quote")
    suspend fun getRandomQuote(): Quotes
}
