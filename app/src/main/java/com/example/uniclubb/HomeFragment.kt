package com.example.uniclubb

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: EventAdapter
    private val eventList = mutableListOf<Event>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recyclerEvents)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventAdapter(eventList) { event ->
            val intent = Intent(requireContext(), EventDetailActivity::class.java)
            intent.putExtra("eventId", event.eventId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            loadEvents()
        }

        // Load events on first launch
        swipeRefreshLayout.isRefreshing = true
        loadEvents()

        return view
    }

    private fun loadEvents() {
        FirebaseFirestore.getInstance().collection("events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                eventList.clear()
                for (doc in snapshots) {
                    val event = doc.toObject(Event::class.java)
                    eventList.add(event)
                }
                adapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener {
                swipeRefreshLayout.isRefreshing = false
            }
    }
}
