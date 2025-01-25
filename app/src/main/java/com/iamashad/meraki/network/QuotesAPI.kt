package com.iamashad.meraki.network

import com.iamashad.meraki.model.QuotesItem
import retrofit2.http.GET

interface QuotesAPI {
    @GET("random")
    suspend fun getRandomQuote(): List<QuotesItem>
}
