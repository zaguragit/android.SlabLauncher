package io.posidon.android.slablauncher.ui.home.main.suggestion

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.luminance
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.HorizontalAspectRatioLayout
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings

class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val aspect = itemView.findViewById<HorizontalAspectRatioLayout>(R.id.aspect)!!.apply {
        widthToHeight = HomeArea.SUGGESTION_WIDTH_TO_HEIGHT
    }

    private val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    private val card = itemView.findViewById<CardView>(R.id.card)!!

    private fun updateBackground(
        item: LauncherItem,
        iconData: IconData<GraphicsLoader.Extra>,
        settings: Settings,
    ) {
        val itemColor = iconData.extra.color.let {
            when {
                settings.doMonochrome -> {
                    val a = (it.luminance * 255).toInt()
                    Color.argb(0, a, a, a)
                }
                else -> it
            }
        }
        val backgroundColor = ColorTheme.tileColor(itemColor)
        imageView.post {
            card.setCardBackgroundColor(backgroundColor)
            imageView.setImageDrawable(iconData.extra.tile)
            imageView.alpha = 1f
            card.cardElevation = itemView.context.resources.getDimension(R.dimen.item_card_elevation)

            if (settings.doMonochrome) {
                imageView.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    setSaturation(0f)
                })
                if (item is App && !itemView.context.isUserRunning(item.userHandle)) {
                    imageView.alpha = 0.7f
                    card.cardElevation = 0f
                    card.setCardBackgroundColor(0)
                }
            } else imageView.colorFilter = null
        }
    }

    fun onBind(
        item: LauncherItem,
        navbarHeight: Int,
        graphicsLoader: GraphicsLoader,
        settings: Settings,
    ) {
        imageView.setImageDrawable(null)
        card.setCardBackgroundColor(ColorTheme.cardBG)

        graphicsLoader.load(itemView.context, item) {
            updateBackground(item, it, settings)
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
        imageView.setImageDrawable(null)
    }
}