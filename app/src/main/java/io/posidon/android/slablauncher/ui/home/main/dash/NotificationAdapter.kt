package io.posidon.android.slablauncher.ui.home.main.dash

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.data.notification.NotificationGroupData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.dash.viewHolder.NotificationViewHolder

class NotificationAdapter(val launcherContext: LauncherContext) : RecyclerView.Adapter<NotificationViewHolder>() {

    private var data = emptyList<NotificationGroupData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(parent).apply {
            itemView.setOnClickListener {
                data[bindingAdapterPosition].run {
                    if (notifications.size == 1)
                        notifications[0].open()
                    else if (sourcePackageName != null)
                        launcherContext.appManager.getAppByPackage(sourcePackageName)?.open(parent.context, itemView)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, i: Int) {
        val notification = data[i]
        holder.onBind(notification)
    }

    override fun getItemCount() = data.size

    fun updateItems(data: List<NotificationGroupData>) {
        this.data = data
        notifyDataSetChanged()
    }
}