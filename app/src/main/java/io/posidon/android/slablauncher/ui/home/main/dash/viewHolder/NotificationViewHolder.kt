package io.posidon.android.slablauncher.ui.home.main.dash.viewHolder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R

class NotificationViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    val source = itemView.findViewById<TextView>(R.id.source)
    val title = itemView.findViewById<TextView>(R.id.title)
    val text = itemView.findViewById<TextView>(R.id.text)
    val image = itemView.findViewById<ImageView>(R.id.notification_image)
    val imageCard = itemView.findViewById<CardView>(R.id.notification_image_card)
}