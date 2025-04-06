package com.example.uniclubb.model

data class EventRegistration(
    val userId: String = "",                   // UID of the registering user
    val userName: String = "",                 // Full name
    val contactNumber: String = "",            // Primary contact number
    val department: String? = null,            // Department (optional)

    val isGroup: Boolean = false,              // Indicates if it's a group registration
    val groupName: String? = null,             // Name of the group (if group registration)
    val groupSize: Int? = null,                // Number of people in the group

    val alternateContact: String? = null,      // Alternate phone/email (optional)
    val songUrl: String? = null,               // Song URL (Cloudinary or other) if uploaded

    val timestamp: Long = System.currentTimeMillis()  // Time of registration
)
