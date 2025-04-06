package com.example.uniclubb.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uniclubb.R
import com.example.uniclubb.NotificationAdapter
import com.example.uniclubb.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()

    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextView: TextView

    private val db = FirebaseFirestore.getInstance()
    private val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        recyclerView = view.findViewById(R.id.recyclerNotifications)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(notifications)
        recyclerView.adapter = adapter

        progressBar = view.findViewById(R.id.progressLoading)
        emptyTextView = view.findViewById(R.id.textViewEmpty)

        loadUserClubAccess()

        return view
    }

    private fun loadUserClubAccess() {
        if (currentUserEmail == null) return

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyTextView.visibility = View.GONE

        db.collection("users").document(currentUserEmail)
            .get()
            .addOnSuccessListener { doc ->
                val followedClubs = doc.get("followedClubs") as? List<String> ?: emptyList()
                val presidentOf = doc.get("presidentOf") as? List<String> ?: emptyList()
                val supervisorOf = doc.get("supervisorOf") as? List<String> ?: emptyList()
                val memberOf = doc.get("memberOf") as? List<String> ?: emptyList()

                val allClubIds = mutableSetOf<String>()
                allClubIds.addAll(followedClubs)
                allClubIds.addAll(presidentOf)
                allClubIds.addAll(supervisorOf)
                allClubIds.addAll(memberOf)

                loadNotificationsFromClubs(allClubIds.toList())
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
                emptyTextView.text = "Failed to load notifications."
            }
    }

    private var clubsLoaded = 0

    private fun loadNotificationsFromClubs(clubIds: List<String>) {
        notifications.clear()

        if (clubIds.isEmpty()) {
            progressBar.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
            return
        }

        clubsLoaded = 0

        for (clubId in clubIds) {
            db.collection("clubs").document(clubId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val notification = doc.toObject(Notification::class.java).copy(id = doc.id)
                        notifications.add(notification)
                    }

                    clubsLoaded++
                    if (clubsLoaded == clubIds.size) {
                        // Sort after all loaded
                        notifications.sortByDescending { it.timestamp }
                        adapter.notifyDataSetChanged()

                        progressBar.visibility = View.GONE
                        if (notifications.isEmpty()) {
                            emptyTextView.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                        }
                    }
                }
                .addOnFailureListener {
                    clubsLoaded++
                    if (clubsLoaded == clubIds.size) {
                        adapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                        if (notifications.isEmpty()) {
                            emptyTextView.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }
}
