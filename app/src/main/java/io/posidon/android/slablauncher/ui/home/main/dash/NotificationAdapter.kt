package io.posidon.android.slablauncher.ui.home.main.dash

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.dash.viewHolder.NotificationViewHolder

class NotificationAdapter : RecyclerView.Adapter<NotificationViewHolder>() {

    private var data = emptyList<NotificationData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.notification, parent, false)).apply {
            itemView.setOnClickListener {
                data[bindingAdapterPosition].open()
            }
        }
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, i: Int) {
        val notification = data[i]
        holder.icon.setImageDrawable(notification.icon)
        holder.source.text = notification.source
        holder.title.text = notification.title
        holder.text.text = notification.description

        holder.icon.imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        holder.source.setTextColor(ColorTheme.cardDescription)
        holder.title.setTextColor(ColorTheme.cardTitle)
        holder.text.setTextColor(ColorTheme.cardDescription)
    }

    override fun getItemCount() = data.size

    fun updateItems(data: List<NotificationData>) {
        this.data = data
        notifyDataSetChanged()
    }
}