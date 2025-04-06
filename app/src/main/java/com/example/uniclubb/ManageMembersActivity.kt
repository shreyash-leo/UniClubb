package com.example.uniclubb

import android.os.Bundle
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ManageMembersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MemberAdapter
    private val members = mutableListOf<Member>()
    private var clubId: String? = null
    private val db = FirebaseFirestore.getInstance()

    private lateinit var addMemberLayout: LinearLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var memberPositionEditText: EditText
    private lateinit var memberEmailEditText: EditText
    private lateinit var memberContactEditText: EditText
    private lateinit var addMemberButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_members)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerManageMembers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MemberAdapter(members, clubId)
        recyclerView.adapter = adapter

        // Get club ID from Intent
        clubId = intent.getStringExtra("clubId")
        if (clubId == null) {
            Toast.makeText(this, "Club ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load members from Firestore
        loadMembers()

        // Initialize Add Member Views
        addMemberLayout = findViewById(R.id.addMemberLayout)
        memberNameEditText = findViewById(R.id.memberNameEditText)
        memberPositionEditText = findViewById(R.id.memberPositionEditText)
        memberEmailEditText = findViewById(R.id.memberEmailEditText)
        memberContactEditText = findViewById(R.id.memberContactEditText)
        addMemberButton = findViewById(R.id.addMemberButton)

        // Set up Add Member button click listener
        addMemberButton.setOnClickListener {
            val name = memberNameEditText.text.toString()
            val position = memberPositionEditText.text.toString()
            val email = memberEmailEditText.text.toString()
            val contact = memberContactEditText.text.toString()

            // Validate input fields
            if (name.isNotBlank() && position.isNotBlank() && email.isNotBlank() && contact.isNotBlank()) {
                addMember(name, position, email, contact)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load members from Firestore
    private fun loadMembers() {
        db.collection("clubs").document(clubId!!).collection("members")
            .get()
            .addOnSuccessListener { snapshot ->
                members.clear() // Clear the existing list to avoid duplicates
                for (doc in snapshot) {
                    val member = doc.toObject(Member::class.java)
                    members.add(member) // Add new members to the list
                }
                adapter.notifyDataSetChanged() // Notify adapter to update RecyclerView
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show()
            }
    }

    // Add a new member to Firestore
    private fun addMember(name: String, position: String, email: String, contact: String) {
        val newMember = hashMapOf(
            "name" to name,
            "position" to position,
            "email" to email,
            "contact" to contact
        )

        // Add the new member to Firestore
        db.collection("clubs").document(clubId!!).collection("members")
            .add(newMember)
            .addOnSuccessListener {
                Toast.makeText(this, "Member Added", Toast.LENGTH_SHORT).show()
                loadMembers() // Reload member list after adding
                resetInputFields() // Clear input fields
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error Adding Member", Toast.LENGTH_SHORT).show()
            }
    }

    // Reset input fields after adding a member
    private fun resetInputFields() {
        memberNameEditText.text.clear()
        memberPositionEditText.text.clear()
        memberEmailEditText.text.clear()
        memberContactEditText.text.clear()
    }
}
