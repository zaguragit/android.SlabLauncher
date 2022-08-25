package io.posidon.android.slablauncher.ui.settings.viewHolders

import android.content.res.ColorStateList
import android.graphics.drawable.*
import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.ui.settings.SettingsItem
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable

class SettingsEntryItemViewHolder(itemView: View) : SettingsViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)

    val text = itemView.findViewById<TextView>(R.id.text)
    val description = itemView.findViewById<TextView>(R.id.description)

    val entry = itemView.findViewById<EditText>(R.id.entry)

    val ripple = RippleDrawable(ColorStateList.valueOf(0), null, FastColorDrawable(0xffffffff.toInt()))

    init {
        itemView.background = ripple
    }

    override fun onBind(item: SettingsItem<*>) {
        item as SettingsItem<String>

        text.text = item.text
        description.text = item.description

        itemView.setOnClickListener {
            entry.requestFocus()
        }

        text.setTextColor(ColorTheme.cardTitle)
        entry.setTextColor(ColorTheme.cardTitle)
        entry.highlightColor = ColorTheme.accentColor and 0xffffff or 0x33000000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            entry.textCursorDrawable?.setTint(ColorTheme.accentColor)
        }

        ripple.setColor(ColorStateList.valueOf(ColorTheme.accentColor and 0xffffff or 0x33000000))

        description.hideIfNullOr(item.description) {
            text = it
            setTextColor(ColorTheme.cardDescription)
        }
        icon.hideIfNullOr(item.icon) {
            setImageDrawable(it)
            imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        }
        entry.setText(item.value)
        entry.addTextChangedListener { e ->
            item.onValueChange!!(entry, e.toString())
        }
    }
}