package com.example.uniclubb

data class Event(
    val eventId: String = "",
    val flyerUrl: String = "",
    val clubLogoUrl: String = "",
    val clubName: String = "",
    val title: String = "",
    val date: String = "",
    val venue: String = "",
    val category: String = "",
    val description: String = "",
    val registrationRequired: Boolean = false,
    val allowGroup: Boolean = false,
    val allowSong: Boolean = false,
    val timestamp: com.google.firebase.Timestamp? = null,
    val clubId: String = ""
)
