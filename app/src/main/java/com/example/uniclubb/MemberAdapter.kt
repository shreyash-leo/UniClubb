package com.example.uniclubb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MemberAdapter(
    private val members: MutableList<Member>,  // MutableList to allow updates
    private val clubId: String?
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.memberName)
        val email: TextView = itemView.findViewById(R.id.memberEmail)
        val position: TextView = itemView.findViewById(R.id.memberPosition)
        val removeButton: ImageButton = itemView.findViewById(R.id.btnRemoveMember)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.name.text = member.memberName
        holder.email.text = member.memberEmail
        holder.position.text = member.memberPosition

        // Handle member removal on click
        holder.removeButton.setOnClickListener {
            member.memberEmail?.let { email ->
                clubId?.let { id ->
                    removeMember(id, email, holder.itemView.context, position)
                }
            }
        }
    }

    override fun getItemCount(): Int = members.size

    // Modified to accept context and position to update the RecyclerView
    private fun removeMember(clubId: String, email: String, context: Context, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val clubRef = db.collection("clubs").document(clubId)

        clubRef.get().addOnSuccessListener { doc ->
            val club = doc.toObject(Club::class.java)
            val updatedMembers = club?.members?.filter { it.memberEmail != email }

            // Update Firestore
            clubRef.update("members", updatedMembers)
                .addOnSuccessListener {
                    // Remove the member from the local list and update the UI
                    members.removeAt(position)
                    notifyItemRemoved(position)

                    Toast.makeText(
                        context,
                        "Member removed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Error removing member.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
