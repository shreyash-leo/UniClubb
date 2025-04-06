package com.example.uniclubb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ClubEventsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter
    private val events = mutableListOf<Event>()
    private var clubId: String? = null

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_events)

        recyclerView = findViewById(R.id.recyclerClubEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EventAdapter(events) { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventId", event.eventId) // ✅ use correct property name
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        clubId = intent.getStringExtra("clubId")
        if (clubId != null) {
            loadClubEvents(clubId!!)
        }
    }

    private fun loadClubEvents(clubId: String) {
        db.collection("events")
            .whereEqualTo("clubId", clubId)
            .get()
            .addOnSuccessListener { result ->
                events.clear()
                for (doc in result) {
                    val event = doc.toObject(Event::class.java).copy(eventId = doc.id)
                    events.add(event)
                }
                adapter.notifyDataSetChanged()
            }
    }

}
