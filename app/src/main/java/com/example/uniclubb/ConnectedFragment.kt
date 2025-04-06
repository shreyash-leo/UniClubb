package com.example.uniclubb

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConnectedFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClubAdapter
    private val connectedList = mutableListOf<Club>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connected, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewConnectedClubs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClubAdapter(connectedList)
        recyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchConnectedClubs()
    }

    private fun fetchConnectedClubs() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid ?: return
        val userEmail = currentUser.email?.lowercase() ?: return

        val userRef = db.collection("users").document(userUid)

        userRef.get().addOnSuccessListener { userDoc ->
            val followedClubs = userDoc.get("followedClubs") as? List<String> ?: emptyList()

            db.collection("clubs").get()
                .addOnSuccessListener { documents ->
                    connectedList.clear()

                    for (doc in documents) {
                        val club = doc.toObject(Club::class.java).copy(id = doc.id)

                        val isPresident = club.presidentEmail?.lowercase() == userEmail
                        val isSupervisor = club.supervisor?.supervisorEmail?.lowercase() == userEmail
                        val isMember = club.members.any { it.memberEmail?.lowercase() == userEmail }
                        val isFollowed = doc.id in followedClubs

                        if (isPresident || isSupervisor || isMember || isFollowed) {
                            connectedList.add(club)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("ConnectedFragment", "Error fetching clubs", e)
                }
        }.addOnFailureListener {
            Log.e("ConnectedFragment", "Error fetching user document", it)
        }
    }
}
