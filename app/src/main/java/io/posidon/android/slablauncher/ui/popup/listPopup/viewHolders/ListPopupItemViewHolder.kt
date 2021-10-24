package io.posidon.android.slablauncher.ui.popup.listPopup.viewHolders

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.applyIfNotNull
import io.posidon.android.slablauncher.ui.popup.listPopup.ListPopupItem

class ListPopupItemViewHolder(itemView: View) : ListPopupViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)

    val text = itemView.findViewById<TextView>(R.id.text)
    val description = itemView.findViewById<TextView>(R.id.description)

    val ripple = RippleDrawable(ColorStateList.valueOf(0), null, ColorDrawable(0xffffffff.toInt()))

    init {
        itemView.background = ripple
    }

    override fun onBind(item: ListPopupItem) {
        text.text = item.text
        description.text = item.description

        text.setTextColor(ColorTheme.cardTitle)

        itemView.setOnClickListener(item.onClick)

        ripple.setColor(ColorStateList.valueOf(ColorTheme.accentColor and 0xffffff or 0x33000000))

        applyIfNotNull(description, item.description) { view, value ->
            view.text = value
            description.setTextColor(ColorTheme.cardDescription)
        }
        applyIfNotNull(icon, item.icon) { view, value ->
            view.setImageDrawable(value)
            view.imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        }
    }
}