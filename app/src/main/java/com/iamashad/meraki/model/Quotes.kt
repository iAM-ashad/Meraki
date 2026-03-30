package com.iamashad.meraki.model

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

/**
 * Data model representing a motivational or inspirational quote.
 *
 * ZenQuotes API (https://zenquotes.io/api/random) returns JSON keys "q" (quote)
 * and "a" (author), so @SerializedName is used to map them to readable property names.
 *
 * @property author The name of the person who said or wrote the quote.
 * @property quote The actual quote text.
 */
// Phase 6: @Immutable signals to the Compose compiler that all properties are stable,
// enabling skipping of unnecessary recompositions.
@Immutable
data class Quotes(
    @SerializedName("a") val author: String,
    @SerializedName("q") val quote: String
)
