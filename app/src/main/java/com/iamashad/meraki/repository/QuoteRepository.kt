package com.iamashad.meraki.repository

import com.iamashad.meraki.model.QuotesItem
import com.iamashad.meraki.network.QuotesAPI
import javax.inject.Inject

class ZenQuotesRepository @Inject constructor(
    private val zenQuotesApi: QuotesAPI
) {
    suspend fun getRandomQuote(): QuotesItem {
        val quotes = zenQuotesApi.getRandomQuote()
        return quotes.first() // Get the first (and only) quote in the list
    }
}
