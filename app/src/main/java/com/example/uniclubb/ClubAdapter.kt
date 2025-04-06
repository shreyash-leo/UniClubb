package com.example.uniclubb

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ClubAdapter(private val clubs: List<Club>) : RecyclerView.Adapter<ClubAdapter.ClubViewHolder>() {

    class ClubViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clubBanner: ImageView = itemView.findViewById(R.id.clubBannerImageView)
        val clubLogo: ImageView = itemView.findViewById(R.id.clubLogoImageView)
        val clubName: TextView = itemView.findViewById(R.id.clubNameTextView)
        val clubCategory: TextView = itemView.findViewById(R.id.clubCategoryTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClubViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_club, parent, false)
        return ClubViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClubViewHolder, position: Int) {
        val club = clubs[position]

        holder.clubName.text = club.clubname
        holder.clubCategory.text = club.category
        holder.itemView.setOnClickListener {
            holder.itemView.isEnabled = false
            it.postDelayed({ holder.itemView.isEnabled = true }, 500)

            val context = holder.itemView.context
            val intent = Intent(context, ClubDetailActivity::class.java)
            intent.putExtra("club", club)
            context.startActivity(intent)
        }

        // Load images using Glide with placeholder
        Glide.with(holder.itemView.context)
            .load(club.bannerImageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.clubBanner)

        Glide.with(holder.itemView.context)
            .load(club.logoImageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.clubLogo)

        // Handle click event to open ClubDetailActivity
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ClubDetailActivity::class.java).apply {
                putExtra("club_id", club.id)
                putExtra("club_banner", club.bannerImageUrl)
                putExtra("club_logo", club.logoImageUrl)
                putExtra("club_name", club.clubname)
                putExtra("club_category", club.category)
                putExtra("club_description", club.description)
                putExtra("president_name", club.presidentName)
                putExtra("president_email", club.presidentEmail)
                putExtra("president_contact", club.presidentContact)
                putExtra("supervisor_name", club.supervisor?.let { it.supervisorName } ?: "N/A")
                putExtra("supervisor_email", club.supervisor?.let { it.supervisorEmail } ?: "N/A")
                putParcelableArrayListExtra("club_members", ArrayList(club.members))
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = clubs.size
}
