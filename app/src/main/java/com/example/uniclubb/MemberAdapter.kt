package com.example.uniclubb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MemberAdapter(
    private val members: MutableList<Member>,
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

        holder.removeButton.setOnClickListener {
            member.memberEmail?.let { email ->
                clubId?.let { id ->
                    removeMember(id, email, holder.itemView.context, position)
                }
            }
        }
    }

    override fun getItemCount(): Int = members.size

    private fun removeMember(clubId: String, email: String, context: Context, position: Int) {
        val db = FirebaseFirestore.getInstance()

        // Step 1: Remove from subcollection
        db.collection("clubs").document(clubId)
            .collection("members")
            .whereEqualTo("memberEmail", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    doc.reference.delete()
                }

                // Step 2: Remove from local list and update UI
                val removedMember = members.removeAt(position)
                notifyItemRemoved(position)

                // Step 3: Update the main club document's 'members' array
                db.collection("clubs").document(clubId)
                    .update("members", members)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Member removed!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update main club doc", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error removing member from subcollection", Toast.LENGTH_SHORT).show()
            }
    }
}
