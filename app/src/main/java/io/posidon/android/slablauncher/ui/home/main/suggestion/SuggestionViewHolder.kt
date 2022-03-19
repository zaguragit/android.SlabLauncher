package io.posidon.android.slablauncher.ui.home.main.suggestion

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.os.Build
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute
import io.posidon.android.computable.syncCompute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.LauncherItem.Banner.Companion.ALPHA_MULTIPLIER
import io.posidon.android.slablauncher.data.items.getBanner
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeTileBackground
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
                val backgroundColor = ColorTheme.tileColor(it)
                ItemLongPress.onItemLongPress(
                    v,
                    backgroundColor,
                    ColorTheme.titleColorForBG(itemView.context, backgroundColor),
                    item,
                    navbarHeight,
                )
            }
            true
        }

        val banner = item.getBanner()

        item.color.compute {
            val backgroundColor = ColorTheme.tileColor(it)
            card.post {
                card.setCardBackgroundColor(backgroundColor)
            }
            val background = banner.background.syncCompute()
            if (background == null) {
                imageView.post {
                    imageView.isVisible = false
                }
            } else {
                val palette = Palette.from(background.toBitmap(24, 24)).generate()
                val imageColor = palette.getDominantColor(it)
                val newBackgroundColor = ColorTheme.tileColor(imageColor)
                imageView.post {
                    imageView.isVisible = true
                    imageView.setImageDrawable(background)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (settings.doMonochromeTileBackground) {
                            imageView.imageTintList = ColorStateList.valueOf(backgroundColor)
                            imageView.imageTintBlendMode = BlendMode.COLOR
                        } else imageView.imageTintList = null
                    }
                    imageView.alpha = banner.bgOpacity * ALPHA_MULTIPLIER
                    card.setCardBackgroundColor(newBackgroundColor)
                }
            }
            icon.post {
                icon.isVisible = banner.hideIcon != true
            }
        }
    }

    fun recycle(item: LauncherItem) {
        item.icon.offload()
    }
}