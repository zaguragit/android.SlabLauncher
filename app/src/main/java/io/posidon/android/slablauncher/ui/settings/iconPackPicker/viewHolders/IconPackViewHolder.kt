package io.posidon.android.slablauncher.ui.settings.iconPackPicker.viewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.IconPackPickerActivity

class IconPackViewHolder(itemView: View, val type: Int) : RecyclerView.ViewHolder(itemView) {
    val icon = itemView.findViewById<ImageView>(R.id.icon)
    val text = itemView.findViewById<TextView>(R.id.text)

    fun bind(iconPack: IconPackPickerActivity.IconPack) {
        text.text = iconPack.label
        icon.setImageDrawable(iconPack.icon)
        text.setTextColor(ColorTheme.uiDescription)
    }
}