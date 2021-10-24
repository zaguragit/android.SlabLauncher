package io.posidon.android.slablauncher.ui.today.viewHolders.suggestion

import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.SeeThroughView

class SuggestionViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {
    val icon = itemView.findViewById<ImageView>(R.id.icon_image)!!
    val label = itemView.findViewById<TextView>(R.id.icon_text)!!
    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!

    fun onBind(
        item: LauncherItem,
        navbarHeight: Int,
    ) {
        blurBG.drawable = BitmapDrawable(itemView.resources, acrylicBlur?.insaneBlur)

        val backgroundColor = ColorTheme.tintAppDrawerItem(item.getColor())
        card.setCardBackgroundColor(backgroundColor)
        label.text = item.label
        label.setTextColor(ColorTheme.titleColorForBG(itemView.context, backgroundColor))
        icon.setImageDrawable(item.icon)

        itemView.setOnClickListener {
            SuggestionsManager.onItemOpened(it.context, item)
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener {
            ItemLongPress.onItemLongPress(
                it,
                backgroundColor,
                ColorTheme.titleColorForBG(itemView.context, backgroundColor),
                item,
                navbarHeight,
            )
            true
        }
    }
}