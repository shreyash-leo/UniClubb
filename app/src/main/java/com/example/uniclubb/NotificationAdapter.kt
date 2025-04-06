package com.example.uniclubb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uniclubb.Notification  // Correct import of the Notification class
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle: TextView = itemView.findViewById(R.id.textNotificationTitle)
        val textMessage: TextView = itemView.findViewById(R.id.textNotificationMessage)
        val textClub: TextView = itemView.findViewById(R.id.textNotificationClub)
        val textTimestamp: TextView = itemView.findViewById(R.id.textNotificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.textTitle.text = notification.title
        holder.textMessage.text = notification.message
        holder.textClub.text = notification.clubName

        val formattedTime = notification.timestamp?.toDate()?.let {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(it)
        } ?: "Unknown time"

        holder.textTimestamp.text = formattedTime
    }

    override fun getItemCount(): Int = notifications.size
}
