package com.iamashad.meraki.repository

import com.iamashad.meraki.model.Quotes
import com.iamashad.meraki.network.QuotesAPI
import javax.inject.Inject

class QuotesRepository @Inject constructor(
    private val quotesApi: QuotesAPI
) {
    suspend fun getRandomQuote(): Quotes {
        val quotes = quotesApi.getRandomQuote()
        return quotes
    }
}
