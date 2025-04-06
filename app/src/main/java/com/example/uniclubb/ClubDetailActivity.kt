package com.example.uniclubb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ClubDetailActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_MANAGE_MEMBERS = 1
    }
    private lateinit var layoutMembers: LinearLayout // Declare as a class-level variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_detail)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid ?: return
        val userEmail = currentUser.email ?: return
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userUid)

        val btnFollow = findViewById<Button>(R.id.buttonFollow)
        val textViewFollowers = findViewById<TextView>(R.id.textViewFollowers)
        val fabCreateEvent = findViewById<FloatingActionButton>(R.id.fabCreateEvent)
        val fabSendNotification = findViewById<FloatingActionButton>(R.id.fabSendNotification)
        val btnManageClub = findViewById<Button>(R.id.btnManageClub)

        // Club data
        val clubId = intent.getStringExtra("club_id") ?: return
        val clubBanner = intent.getStringExtra("club_banner")
        val clubLogo = intent.getStringExtra("club_logo")
        val clubName = intent.getStringExtra("club_name")
        val clubCategory = intent.getStringExtra("club_category")
        val clubDescription = intent.getStringExtra("club_description")
        val presidentName = intent.getStringExtra("president_name")
        val presidentEmail = intent.getStringExtra("president_email")
        val presidentContact = intent.getStringExtra("president_contact")
        val supervisorName = intent.getStringExtra("supervisor_name")
        val supervisorEmail = intent.getStringExtra("supervisor_email")
        val members = intent.getParcelableArrayListExtra<Member>("club_members")

        // UI Elements
        val imageViewBanner = findViewById<ImageView>(R.id.imageViewBanner)
        val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
        val textViewClubName = findViewById<TextView>(R.id.textViewClubName)
        val textViewCategory = findViewById<TextView>(R.id.textViewCategory)
        val textViewDescription = findViewById<TextView>(R.id.textViewDescription)
        val textViewPresident = findViewById<TextView>(R.id.textViewPresident)
        val textViewSupervisor = findViewById<TextView>(R.id.textViewSupervisor)
        val layoutMembers = findViewById<LinearLayout>(R.id.layoutMembers)

        val currentUserEmail = userEmail.lowercase()
        val isPresident = currentUserEmail == presidentEmail?.lowercase()
        val isSupervisor = currentUserEmail == supervisorEmail?.lowercase()
        val isMember = members?.any { it.memberEmail?.lowercase() == currentUserEmail } == true

        Glide.with(this).load(clubBanner).into(imageViewBanner)
        Glide.with(this).load(clubLogo).into(imageViewLogo)

        textViewClubName.text = clubName
        textViewCategory.text = clubCategory
        textViewDescription.text = clubDescription

        textViewPresident.text = "President: $presidentName\nEmail: $presidentEmail\nContact: $presidentContact"
        textViewSupervisor.text = if (!supervisorName.isNullOrEmpty()) {
            "Supervisor: $supervisorName\nEmail: $supervisorEmail"
        } else {
            "No supervisor assigned."
        }

        layoutMembers.removeAllViews()
        members?.forEach { member ->
            val memberView = LayoutInflater.from(this).inflate(R.layout.item_member, layoutMembers, false)
            memberView.findViewById<TextView>(R.id.textViewMemberName).text =
                "${member.memberName} (${member.memberPosition})"
            memberView.findViewById<TextView>(R.id.textViewMemberEmail).text =
                "Email: ${member.memberEmail}"
            memberView.findViewById<ImageView>(R.id.imageViewBadge)
                .setImageResource(R.drawable.ic_user_badge)
            layoutMembers.addView(memberView)
        }

        fun updateButtonState(isFollowing: Boolean) {
            btnFollow.text = if (isFollowing) "Following" else "Follow"
        }

        fun updateFollowerCount() {
            db.collection("users")
                .whereArrayContains("followedClubs", clubId)
                .get()
                .addOnSuccessListener { snapshot ->
                    textViewFollowers.text = "Followers: ${snapshot.size()}"
                }
                .addOnFailureListener {
                    Log.e("ClubDetailActivity", "Failed to fetch follower count", it)
                }
        }

        // Show FAB for event creation if admin or member
        if (isPresident || isSupervisor || isMember) {
            fabCreateEvent.visibility = View.VISIBLE
            fabCreateEvent.setOnClickListener {
                val intent = Intent(this, CreateEventActivity::class.java)
                intent.putExtra("clubId", clubId)
                intent.putExtra("clubName", clubName)
                intent.putExtra("clubLogoUrl", clubLogo)
                startActivity(intent)
            }
        }

        // Show FAB for sending notification if president or supervisor
        if (isPresident || isSupervisor) {
            fabSendNotification.visibility = View.VISIBLE
            fabSendNotification.setOnClickListener {
                showSendNotificationDialog(clubId, clubName)
            }
        }

        // Show Admin Button only to President or Supervisor
        if (isPresident || isSupervisor) {
            btnManageClub.visibility = View.VISIBLE
            btnManageClub.setOnClickListener {
                val intent = Intent(this, AdminClubActivity::class.java)
                intent.putExtra("clubId", clubId)
                startActivity(intent)
            }
        } else {
            btnManageClub.visibility = View.GONE
        }

        updateFollowerCount()

        userDocRef.get().addOnSuccessListener { doc ->
            val followedClubs = doc.get("followedClubs") as? List<String> ?: emptyList()
            var isFollowing = clubId in followedClubs

            updateButtonState(isFollowing)

            btnFollow.setOnClickListener {
                val updatedList = if (isFollowing) {
                    followedClubs - clubId
                } else {
                    followedClubs + clubId
                }

                userDocRef.set(
                    mapOf(
                        "email" to userEmail,
                        "followedClubs" to updatedList
                    ),
                    SetOptions.merge()
                ).addOnSuccessListener {
                    isFollowing = !isFollowing
                    updateButtonState(isFollowing)
                    updateFollowerCount()
                }.addOnFailureListener {
                    Log.e("ClubDetailActivity", "Failed to update follow status", it)
                }
            }
        }.addOnFailureListener {
            Log.e("ClubDetailActivity", "User doc fetch failed", it)
        }
    }

    // Method to handle the result when a new member is added
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_MANAGE_MEMBERS && resultCode == RESULT_OK) {
            val db = FirebaseFirestore.getInstance()
            val clubId = intent.getStringExtra("club_id") ?: return

            db.collection("clubs").document(clubId).collection("members")
                .get()
                .addOnSuccessListener { snapshot ->
                    layoutMembers.removeAllViews()
                    for (doc in snapshot) {
                        val member = doc.toObject(Member::class.java)
                        val memberView = LayoutInflater.from(this).inflate(R.layout.item_member, layoutMembers, false)
                        memberView.findViewById<TextView>(R.id.textViewMemberName).text =
                            "${member.memberName} (${member.memberPosition})"
                        memberView.findViewById<TextView>(R.id.textViewMemberEmail).text =
                            "Email: ${member.memberEmail}"
                        memberView.findViewById<ImageView>(R.id.imageViewBadge)
                            .setImageResource(R.drawable.ic_user_badge)
                        layoutMembers.addView(memberView)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load updated members", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showSendNotificationDialog(clubId: String, clubName: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_notification, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editNotificationTitle)
        val editMessage = dialogView.findViewById<EditText>(R.id.editNotificationMessage)

        AlertDialog.Builder(this)
            .setTitle("Send Notification")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val title = editTitle.text.toString().trim()
                val message = editMessage.text.toString().trim()

                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(this, "Please enter both title and message", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                sendNotificationToFirestore(clubId, title, message, clubName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendNotificationToFirestore(clubId: String, title: String, message: String, clubName: String?) {
        val db = FirebaseFirestore.getInstance()
        val notification = hashMapOf(
            "clubId" to clubId,
            "clubName" to clubName,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("ClubDetailActivity", "Failed to send notification", e)
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }
}

