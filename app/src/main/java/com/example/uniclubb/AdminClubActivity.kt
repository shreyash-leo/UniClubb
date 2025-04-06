// AdminClubActivity.kt
package com.example.uniclubb

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminClubActivity : AppCompatActivity() {

    private lateinit var btnEditClub: Button
    private lateinit var btnManageMembers: Button
    private lateinit var btnViewEvents: Button
    private lateinit var btnViewNotifications: Button
    private var clubId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_club)

        clubId = intent.getStringExtra("clubId")

        if (clubId == null) {
            Toast.makeText(this, "Club ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnEditClub = findViewById(R.id.btnEditClub)
        btnManageMembers = findViewById(R.id.btnManageMembers)
        btnViewEvents = findViewById(R.id.btnViewEvents)
        btnViewNotifications = findViewById(R.id.btnViewNotifications)

        btnEditClub.setOnClickListener {
            startActivityWithClubId(EditClubInfoActivity::class.java)
        }

        btnManageMembers.setOnClickListener {
            startActivityWithClubId(ManageMembersActivity::class.java)
        }

        btnViewEvents.setOnClickListener {
            startActivityWithClubId(ClubEventsActivity::class.java)
        }

        btnViewNotifications.setOnClickListener {
            startActivityWithClubId(ClubNotificationsActivity::class.java)
        }
    }

    private fun startActivityWithClubId(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.putExtra("clubId", clubId)
        startActivity(intent)
    }
}
