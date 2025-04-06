package com.example.uniclubb

import java.util.UUID
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Club(
    val id: String = UUID.randomUUID().toString(),
    val bannerImageUrl: String? = null,
    val logoImageUrl: String? = null,
    val clubname: String? = null,
    val category: String? = null,
    val description: String? = null,
    val presidentName: String? = null,
    val presidentEmail: String? = null,
    val presidentContact: String? = null,
    val members: List<Member> = emptyList(), // Updated to use the Member data class
    val supervisor: Supervisor? = null, // Updated to use the Supervisor data class
    val creationDate: Long = System.currentTimeMillis()
    // Add any other relevant fields
) : Parcelable

@Parcelize
data class Member(
    val memberName: String? = null,
    val memberPosition: String? = null,
    val memberEmail: String? = null,
    val memberContact: String? = null
) : Parcelable

@Parcelize
data class Supervisor(
    val supervisorName: String? = null,
    val supervisorEmail: String? = null,
    val supervisorContact: String? = null
) : Parcelable