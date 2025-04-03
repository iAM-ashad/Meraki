package com.iamashad.meraki.model

/**
 * Data model representing a motivational or inspirational quote.
 *
 * @property author The name of the person who said or wrote the quote.
 * @property quote The actual quote text.
 */
data class Quotes(
    val author: String,
    val quote: String
)
