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

        clubId = intent.getStringExtra("clubId")
        if (clubId == null) {
            Toast.makeText(this, "Club ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerManageMembers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MemberAdapter(members, clubId!!)
        recyclerView.adapter = adapter

        addMemberLayout = findViewById(R.id.addMemberLayout)
        memberNameEditText = findViewById(R.id.memberNameEditText)
        memberPositionEditText = findViewById(R.id.memberPositionEditText)
        memberEmailEditText = findViewById(R.id.memberEmailEditText)
        memberContactEditText = findViewById(R.id.memberContactEditText)
        addMemberButton = findViewById(R.id.addMemberButton)

        addMemberButton.setOnClickListener {
            val name = memberNameEditText.text.toString().trim()
            val position = memberPositionEditText.text.toString().trim()
            val email = memberEmailEditText.text.toString().trim()
            val contact = memberContactEditText.text.toString().trim()

            if (name.isNotBlank() && position.isNotBlank() && email.isNotBlank() && contact.isNotBlank()) {
                val isDuplicate = members.any { it.memberEmail.equals(email, ignoreCase = true) }
                if (isDuplicate) {
                    Toast.makeText(this, "Member already exists", Toast.LENGTH_SHORT).show()
                } else {
                    addMember(name, position, email, contact)
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        loadMembers()
    }

    private fun loadMembers() {
        db.collection("clubs").document(clubId!!).collection("members")
            .get()
            .addOnSuccessListener { snapshot ->
                members.clear()
                for (doc in snapshot) {
                    val member = doc.toObject(Member::class.java)
                    members.add(member)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMember(name: String, position: String, email: String, contact: String) {
        val newMember = Member(name, email, position)

        val memberMap = hashMapOf(
            "memberName" to name,
            "memberPosition" to position,
            "memberEmail" to email,
            "memberContact" to contact
        )

        db.collection("clubs").document(clubId!!).collection("members")
            .add(memberMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Member Added", Toast.LENGTH_SHORT).show()
                members.add(newMember)
                adapter.notifyItemInserted(members.size - 1)

                // Update the parent club document with new member list
                db.collection("clubs").document(clubId!!)
                    .update("members", members)
                    .addOnSuccessListener {
                        resetInputFields()
                        setResult(RESULT_OK) // Notify ClubDetailActivity to refresh
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to sync members with main doc", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error Adding Member", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetInputFields() {
        memberNameEditText.text.clear()
        memberPositionEditText.text.clear()
        memberEmailEditText.text.clear()
        memberContactEditText.text.clear()
    }
}
