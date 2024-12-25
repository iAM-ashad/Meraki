package com.iamashad.meraki.model

data class Mood(
    val label: String,
    val emoji: String,
    val action: () -> Unit
)
