package io.posidon.android.slablauncher.ui.home.main.dash.viewHolder

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationGroupData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme

class NotificationViewHolder(
    parent: ViewGroup
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
    .inflate(R.layout.notification, parent, false)) {
    val card = itemView.findViewById<CardView>(R.id.card)
    val image = itemView.findViewById<ImageView>(R.id.background_image)
    val icon = itemView.findViewById<ImageView>(R.id.icon)
    val source = itemView.findViewById<TextView>(R.id.source)
    val title = itemView.findViewById<TextView>(R.id.title)
    val text = itemView.findViewById<TextView>(R.id.text)

    fun onBind(notification: NotificationGroupData) {
        card.setCardBackgroundColor(ColorTheme.cardBG)
        source.text = notification.source
        title.text = notification.title
        text.text = if (notification.notifications.size == 1)
            notification.notifications[0].description
        else notification.notifications.mapNotNull { it.description }.joinToString("\n") { "• $it" }

        source.setTextColor(ColorTheme.uiDescription)
        title.setTextColor(ColorTheme.uiTitle)
        text.setTextColor(ColorTheme.uiDescription)
        icon.setImageDrawable(notification.notifications.first().icon)
        val img = notification.notifications.first().image
        if (img == null) {
            image.isVisible = false
        } else {
            image.setImageDrawable(img)
            image.isVisible = true
        }
    }
}