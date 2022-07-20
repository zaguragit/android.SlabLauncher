package io.posidon.android.slablauncher.ui.home.main.suggestion

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.luminance
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings

class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    val label = itemView.findViewById<TextView>(R.id.label)!!

    val card = itemView.findViewById<ViewGroup>(R.id.card)!!

    fun onBind(
        item: LauncherItem,
        navbarHeight: Int,
        graphicsLoader: GraphicsLoader,
        settings: Settings,
    ) {
        label.text = item.label
        label.setTextColor(ColorTheme.searchBarFG)
        icon.setImageDrawable(null)

        graphicsLoader.load(itemView.context, item) {
            icon.post {
                icon.setImageDrawable(it.icon)
                icon.colorFilter = if (settings.doMonochrome) {
                    ColorMatrixColorFilter(ColorMatrix().apply {
                        setSaturation(0f)
                    })
                } else null
            }
            val f =
                if (settings.doMonochrome) ColorTheme.searchBarFG
                else ColorTheme.tintWithColor(ColorTheme.searchBarFG, it.extra.color)
            val backgroundColor = f and 0xffffff or 0x33000000
            val fg = ColorUtils.blendARGB(ColorTheme.searchBarFG, f, 0.5f)
            card.post {
                card.backgroundTintList = ColorStateList.valueOf(backgroundColor)
                label.setTextColor(fg)
            }
        }

        itemView.setOnClickListener {
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener { v ->
            val color = graphicsLoader.load(itemView.context, item).extra.color
            val backgroundColor = ColorTheme.tintPopup(color)
            ItemLongPress.onItemLongPress(
                v,
                backgroundColor,
                ColorTheme.titleColorForBG(backgroundColor),
                item,
                navbarHeight,
                graphicsLoader,
            )
            true
        }
    }

    fun recycle(item: LauncherItem) {
        icon.setImageDrawable(null)
    }
}