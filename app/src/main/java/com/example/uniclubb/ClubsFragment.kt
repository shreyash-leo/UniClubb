package com.example.uniclubb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ClubsFragment : Fragment() {

    private lateinit var fabCreateClub: AppCompatImageButton
    private lateinit var shrinkAnimation: Animation
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClubAdapter
    private val clubsList = mutableListOf<Club>()

    private val CREATE_CLUB_REQUEST = 1 // Request code for club creation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clubs, container, false)

        fabCreateClub = view.findViewById(R.id.fabCreateClub)
        shrinkAnimation = AnimationUtils.loadAnimation(context, R.anim.fab_shrink)
        recyclerView = view.findViewById(R.id.recyclerViewClubs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClubAdapter(clubsList)
        recyclerView.adapter = adapter

        fabCreateClub.setOnClickListener {
            it.startAnimation(shrinkAnimation)
            it.postDelayed({
                val intent = Intent(activity, CreateClubActivity::class.java)
                startActivityForResult(intent, CREATE_CLUB_REQUEST)
            }, shrinkAnimation.duration)
        }

        fetchClubs() // Initial fetch when fragment loads

        return view
    }

    private fun fetchClubs() {
        FirebaseFirestore.getInstance().collection("clubs")
            .get()
            .addOnSuccessListener { documents ->
                clubsList.clear()
                for (doc in documents) {
                    val club = doc.toObject(Club::class.java).copy(id = doc.id)
                    clubsList.add(club)
                    Log.d("ClubData", "Club Name: ${club.clubname}, Banner URL: ${club.bannerImageUrl}, Logo URL: ${club.logoImageUrl}")
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ClubsFragment", "Error fetching clubs", e)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CREATE_CLUB_REQUEST && resultCode == Activity.RESULT_OK) {
            val clubId = data?.getStringExtra("club_id")
            if (clubId != null) {
                refreshClubList() // Refresh only if a new club was added
            }
        }
    }

    private fun refreshClubList() {
        FirebaseFirestore.getInstance().collection("clubs")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val updatedClubs = querySnapshot.documents.mapNotNull { it.toObject(Club::class.java) }
                clubsList.clear()
                clubsList.addAll(updatedClubs)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ClubsFragment", "Failed to refresh club list", e)
            }
    }
}
