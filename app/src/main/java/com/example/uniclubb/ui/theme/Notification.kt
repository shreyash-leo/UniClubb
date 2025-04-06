package com.example.uniclubb.ui.theme

data class Notification(
    val id: String = "",
    val clubId: String = "",
    val clubName: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

