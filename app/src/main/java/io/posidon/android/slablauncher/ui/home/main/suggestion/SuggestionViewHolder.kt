package io.posidon.android.slablauncher.ui.home.main.suggestion

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute
import io.posidon.android.computable.syncCompute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.LauncherItem.Banner.Companion.ALPHA_MULTIPLIER
import io.posidon.android.slablauncher.data.items.getBanner
import io.posidon.android.slablauncher.data.items.getCombinedIcon
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeTileBackground
import io.posidon.android.slablauncher.util.storage.Settings

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
                icon.setImageDrawable(if (item is App) item.getCombinedIcon() else it)
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
            val backgroundColor = ColorTheme.tintWithColor(ColorTheme.searchBarFG, it) and 0xffffff or 0x33000000.toInt()
            card.post {
                card.backgroundTintList = ColorStateList.valueOf(backgroundColor)
            }
        }
    }

    fun recycle(item: LauncherItem) {
        item.icon.offload()
    }
}