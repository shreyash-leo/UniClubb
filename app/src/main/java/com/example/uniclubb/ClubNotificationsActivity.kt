package com.example.uniclubb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ClubNotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()

    private val db = FirebaseFirestore.getInstance()
    private var clubId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_notifications)

        recyclerView = findViewById(R.id.recyclerClubNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notifications)
        recyclerView.adapter = adapter

        clubId = intent.getStringExtra("clubId")

        clubId?.let { loadClubNotifications(it) }
    }

    private fun loadClubNotifications(clubId: String) {
        db.collection("clubs").document(clubId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                notifications.clear()
                for (doc in result) {
                    val notification = doc.toObject(Notification::class.java).copy(id = doc.id)
                    notifications.add(notification)
                }
                adapter.notifyDataSetChanged()
            }
    }
}
