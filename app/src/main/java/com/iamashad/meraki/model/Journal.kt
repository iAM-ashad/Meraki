package com.iamashad.meraki.model

data class Journal(
    val journalId: String,
    val userId: String,
    val title: String,
    val content: String,
    val moodScore: Int,
    val reasons: List<String>,
    val date: Long,
    val imageUrl: String? = null
)
