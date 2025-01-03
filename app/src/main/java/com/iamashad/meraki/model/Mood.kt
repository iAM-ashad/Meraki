package com.iamashad.meraki.model

data class Mood(
    val id: String = "", // Default empty string
    val userId: String = "", // Default empty string
    val score: Int = 0, // Default value for score
    val label: String = "", // Default empty string
    val timestamp: Long = 0L // Default epoch time
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0, "", 0L)
}

