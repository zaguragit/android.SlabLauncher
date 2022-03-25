package io.posidon.android.slablauncher.ui.home.main.suggestion

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress

class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    val label = itemView.findViewById<TextView>(R.id.label)!!

    val card = itemView.findViewById<ViewGroup>(R.id.card)!!

    fun onBind(
        item: LauncherItem,
        navbarHeight: Int,
    ) {

        label.text = item.label
        label.setTextColor(ColorTheme.searchBarFG)
        icon.setImageDrawable(null)
        item.icon.compute {
            icon.post {
                icon.setImageDrawable(it)
            }
        }

        itemView.setOnClickListener {
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener { v ->
            item.color.compute {
                val backgroundColor = ColorTheme.tintPopup(it)
                ItemLongPress.onItemLongPress(
                    v,
                    backgroundColor,
                    ColorTheme.titleColorForBG(backgroundColor),
                    item,
                    navbarHeight,
                )
            }
            true
        }

        item.color.compute {
            val f = ColorTheme.tintWithColor(ColorTheme.searchBarFG, it)
            val backgroundColor = f and 0xffffff or 0x33000000
            val fg = ColorUtils.blendARGB(ColorTheme.searchBarFG, f, 0.5f)
            card.post {
                card.backgroundTintList = ColorStateList.valueOf(backgroundColor)
                label.setTextColor(fg)
            }
        }
    }

    fun recycle(item: LauncherItem) {
        item.icon.offload()
    }
}