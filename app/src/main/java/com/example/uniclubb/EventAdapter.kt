package com.example.uniclubb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class EventAdapter(
    private val events: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flyer: ImageView = view.findViewById(R.id.imageFlyer)
        val clubLogo: ImageView = view.findViewById(R.id.imageClubLogo)
        val clubName: TextView = view.findViewById(R.id.textClubName)
        val eventTitle: TextView = view.findViewById(R.id.textEventTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        Glide.with(holder.itemView).load(event.flyerUrl).into(holder.flyer)
        Glide.with(holder.itemView).load(event.clubLogoUrl).into(holder.clubLogo)
        holder.clubName.text = event.clubName
        holder.eventTitle.text = event.title

        holder.itemView.setOnClickListener { onEventClick(event) }

        // Simple fade-in animation
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()
    }

    override fun getItemCount(): Int = events.size
}
