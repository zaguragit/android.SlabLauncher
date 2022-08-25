package io.posidon.android.slablauncher.ui.settings.viewHolders

import android.view.View
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.ui.settings.SettingsItem

class SettingsTitleViewHolder(itemView: View) : SettingsViewHolder(itemView) {

    val text = itemView.findViewById<TextView>(R.id.text)
    val description = itemView.findViewById<TextView>(R.id.description)
    val separator = itemView.findViewById<View>(R.id.separator)

    override fun onBind(item: SettingsItem<*>) {
        text.text = item.text
        description.text = item.description

        text.setTextColor(ColorTheme.adjustColorForContrast(ColorTheme.cardBG, ColorTheme.secondaryAccentColor))
        separator.setBackgroundColor(ColorTheme.separator)

        itemView.setOnClickListener(item.onClick)

        description.hideIfNullOr(item.description) {
            text = it
            setTextColor(ColorTheme.cardDescription)
        }
    }
}