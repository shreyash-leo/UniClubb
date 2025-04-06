package com.example.uniclubb

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val clubId: String = "",
    val clubName: String = "",
    val timestamp: Timestamp? = null
)
