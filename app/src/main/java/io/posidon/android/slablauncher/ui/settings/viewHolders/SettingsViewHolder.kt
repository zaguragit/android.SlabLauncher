package io.posidon.android.slablauncher.ui.settings.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.ui.settings.SettingsItem

abstract class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun onBind(item: SettingsItem<*>)
}