package com.iamashad.meraki.model

import androidx.compose.runtime.Immutable

/**
 * Data model representing a motivational or inspirational quote.
 *
 * @property author The name of the person who said or wrote the quote.
 * @property quote The actual quote text.
 */
// Phase 6: @Immutable signals to the Compose compiler that all properties are stable,
// enabling skipping of unnecessary recompositions.
@Immutable
data class Quotes(
    val author: String,
    val quote: String
)
