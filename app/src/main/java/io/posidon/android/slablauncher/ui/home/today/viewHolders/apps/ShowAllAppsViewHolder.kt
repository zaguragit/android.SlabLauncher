package io.posidon.android.slablauncher.ui.home.today.viewHolders.apps

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme

class ShowAllAppsViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    val label = itemView.findViewById<TextView>(R.id.text)!!

    fun onBind(openAllApps: () -> Unit) {
        itemView.backgroundTintList = ColorStateList.valueOf(ColorTheme.uiHint and 0xffffff or 0x88000000.toInt())
        label.text = itemView.resources.getString(R.string.all_apps)
        label.setTextColor(ColorTheme.uiDescription)
        itemView.setOnClickListener {
            openAllApps()
        }
    }
}