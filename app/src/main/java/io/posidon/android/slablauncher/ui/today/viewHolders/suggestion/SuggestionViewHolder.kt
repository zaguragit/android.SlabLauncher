package io.posidon.android.slablauncher.ui.today.viewHolders.suggestion

import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import posidon.android.conveniencelib.Colors
import posidon.android.conveniencelib.clone

class SuggestionViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    val icon = itemView.findViewById<ImageView>(R.id.icon_image)!!
    val label = itemView.findViewById<TextView>(R.id.icon_text)!!

    val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    fun onBind(
        item: LauncherItem,
        navbarHeight: Int,
    ) {
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable

        val backgroundColor = ColorTheme.tintAppDrawerItem(item.getColor())
        card.setCardBackgroundColor(backgroundColor)
        label.text = item.label
        label.setTextColor(ColorTheme.titleColorForBG(itemView.context, backgroundColor))
        icon.setImageDrawable(item.icon)

        itemView.setOnClickListener {
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

        val banner = item.getBanner()
        if (banner?.background == null) {
            imageView.isVisible = false
        } else {
            imageView.isVisible = true
            imageView.setImageDrawable(banner.background.clone())
            imageView.alpha = banner.bgOpacity
            val palette = Palette.from(banner.background.toBitmap(32, 32)).generate()
            val color = item.getColor()
            val imageColor = palette.getDominantColor(color)
            val newBackgroundColor = ColorTheme.tintAppDrawerItem(imageColor)
            val actuallyBackgroundColor = Colors.blend(imageColor, newBackgroundColor, imageView.alpha)
            val titleColor = ColorTheme.titleColorForBG(itemView.context, actuallyBackgroundColor)

            card.setCardBackgroundColor(backgroundColor)
            label.setTextColor(titleColor)
        }
        icon.isVisible = banner?.hideIcon != true
    }
}