package io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme

class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)
    val title = itemView.findViewById<TextView>(R.id.title)
    val text = itemView.findViewById<TextView>(R.id.text)

    fun onBind(notification: NotificationData) {
        icon.setImageDrawable(notification.sourceIcon)
        title.text = notification.source
        text.text = notification.description

        val fg = ColorTheme.titleColorForBG(itemView.context, ColorPalette.wallColor)
        icon.imageTintList = ColorStateList.valueOf(fg)
        title.setTextColor(fg)
        text.setTextColor(fg)
    }
}