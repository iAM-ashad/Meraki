package com.iamashad.meraki.model

data class Journal(
    val journalId: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val date: Long = System.currentTimeMillis()
)

