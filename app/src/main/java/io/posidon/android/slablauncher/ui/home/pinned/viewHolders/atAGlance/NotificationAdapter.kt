package io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationData

class NotificationAdapter : RecyclerView.Adapter<NotificationViewHolder>() {

    private var notifications = emptyList<NotificationData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        NotificationViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.notification, parent, false))

    override fun onBindViewHolder(holder: NotificationViewHolder, i: Int) =
        holder.onBind(notifications[i])

    override fun getItemCount() = notifications.size

    fun updateNotifications(notifications: List<NotificationData>) {
        this.notifications = notifications
        notifyDataSetChanged()
    }
}
