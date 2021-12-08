package io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance.suggestion

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.os.Build
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.LauncherItem.Banner.Companion.ALPHA_MULTIPLIER
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeIcons
import io.posidon.android.slablauncher.util.storage.Settings

class SuggestionViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    val icon = itemView.findViewById<ImageView>(R.id.image)!!

    val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    fun onBind(
        item: LauncherItem,
        navbarHeight: Int,
        settings: Settings,
    ) {
        val backgroundColor = ColorTheme.tileColor(item.getColor())

        card.setCardBackgroundColor(backgroundColor)

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
            imageView.setImageDrawable(banner.background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (settings.doMonochromeIcons) {
                    imageView.imageTintList = ColorStateList.valueOf(backgroundColor)
                    imageView.imageTintBlendMode = BlendMode.COLOR
                } else imageView.imageTintList = null
            }
            imageView.alpha = banner.bgOpacity * ALPHA_MULTIPLIER
            val palette = Palette.from(banner.background.toBitmap(24, 24)).generate()
            val color = item.getColor()
            val imageColor = palette.getDominantColor(color)
            val newBackgroundColor = ColorTheme.tileColor(imageColor)
            card.setCardBackgroundColor(newBackgroundColor)
        }
        icon.isVisible = banner?.hideIcon != true
    }
}