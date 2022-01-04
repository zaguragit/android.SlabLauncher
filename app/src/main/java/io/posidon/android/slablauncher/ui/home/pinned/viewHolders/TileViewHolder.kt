package io.posidon.android.slablauncher.ui.home.pinned.viewHolders

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.luminance
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute
import io.posidon.android.computable.syncCompute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.ContactItem
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.LauncherItem.Banner.Companion.ALPHA_MULTIPLIER
import io.posidon.android.slablauncher.data.items.getBanner
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.pinned.TileArea
import io.posidon.android.slablauncher.ui.home.pinned.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeTileBackground
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.slablauncher.util.view.SeeThroughView
import io.posidon.android.slablauncher.util.view.tile.TileContentView
import posidon.android.conveniencelib.Colors
import posidon.android.conveniencelib.getNavigationBarHeight
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TileViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    private val contentView = itemView.findViewById<TileContentView>(R.id.tile_content)!!.apply {
        widthToHeight = TileArea.WIDTH_TO_HEIGHT
    }

    private val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    private val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    fun updateBannerText(
        banner: LauncherItem.Banner,
    ) {
        contentView.setExtraWithAnimation(banner.title, banner.text)
    }

    fun updateLabel(
        item: LauncherItem,
    ) {
        contentView.label = item.label
    }

    fun updateBackground(
        item: LauncherItem,
        background: Drawable?,
        settings: Settings,
        banner: LauncherItem.Banner
    ) {
        val itemColor = item.color.syncCompute()
        val backgroundColor = ColorTheme.tileColor(itemColor)
        val title = ColorTheme.titleColorForBG(itemView.context, backgroundColor)
        val text = ColorTheme.textColorForBG(itemView.context, backgroundColor)
        val label = ColorTheme.adjustColorForContrast(backgroundColor, backgroundColor)
        val mark = ColorTheme.darkestVisibleOn(backgroundColor, backgroundColor) and 0x20ffffff
        contentView.post {
            contentView.labelColor = label
            contentView.titleColor = title
            contentView.textColor = text
            contentView.markColor = mark
            card.setCardBackgroundColor(backgroundColor)
        }
        if (background == null) {
            imageView.post {
                imageView.isVisible = false
            }
        } else {
            val imageColor = run {
                val bitmap = background.toBitmap(24, 24)
                val palette = Palette.from(bitmap).generate()
                palette.getDominantColor(itemColor)
                    .also { bitmap.recycle() }
            }
            val newBackgroundColor = ColorTheme.tileColor(imageColor and 0xffffff)
            val actuallyBackgroundColor =
                Colors.blend(imageColor.let {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        settings.doMonochromeTileBackground &&
                        item !is ContactItem
                    ) (it.luminance * 255).toInt()
                        .let { a -> Color.rgb(a, a, a) }
                    else it
                }, newBackgroundColor, imageView.alpha)
            val titleColor = ColorTheme.titleColorForBG(itemView.context, actuallyBackgroundColor)
            val textColor = ColorTheme.textColorForBG(itemView.context, actuallyBackgroundColor)
            val labelColor =
                ColorTheme.adjustColorForContrast(actuallyBackgroundColor, actuallyBackgroundColor)
            val markColor = ColorTheme.darkestVisibleOn(actuallyBackgroundColor, backgroundColor) and 0x20ffffff

            imageView.post {
                imageView.isVisible = true
                imageView.setImageDrawable(background)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (settings.doMonochromeTileBackground && item !is ContactItem) {
                        imageView.imageTintList = ColorStateList.valueOf(backgroundColor)
                        imageView.imageTintBlendMode = BlendMode.COLOR
                    } else imageView.imageTintList = null
                }
                imageView.alpha = banner.bgOpacity * ALPHA_MULTIPLIER

                card.setCardBackgroundColor(newBackgroundColor)
                contentView.labelColor = labelColor
                contentView.titleColor = titleColor
                contentView.textColor = textColor
                contentView.markColor = markColor
            }
        }
    }

    fun updateTimeMark(item: LauncherItem) {
        contentView.mark = (item as? App)?.let { SuggestionsManager.getUsageTimeMark(itemView.context, item) }
    }

    fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        onDragStart: (View) -> Unit,
    ) {
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable

        updateTimeMark(item)

        imageView.isVisible = false
        imageView.setImageDrawable(null)
        contentView.icon = null
        contentView.labelColor = ColorTheme.cardTitle
        contentView.titleColor = ColorTheme.cardTitle
        contentView.textColor = ColorTheme.cardDescription
        contentView.markColor = ColorTheme.cardDescription and 0x20ffffff
        card.setCardBackgroundColor(ColorTheme.cardBG)

        val banner = item.getBanner()

        if (banner.background.isComputed()) {
            val background = banner.background.computed()
            updateBackground(item, background, settings, banner)
        } else banner.background.compute { background ->
            updateBackground(item, background, settings, banner)
        }
        updateLabel(item)
        if (banner.hideIcon)
            contentView.icon = null
        else {
            val ic = item.icon
            if (ic.isComputed())
                contentView.icon = ic.computed()
            else ic.compute {
                contentView.post {
                    contentView.icon = it
                }
            }
        }
        contentView.extraTitle = banner.title
        contentView.extraText = banner.text

        itemView.setOnClickListener {
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener { v ->
            if (item is App) {
                item.color.compute {
                    val backgroundColor = ColorTheme.tileColor(it)
                    ItemLongPress.onItemLongPress(
                        v,
                        backgroundColor,
                        ColorTheme.titleColorForBG(itemView.context, backgroundColor),
                        item,
                        activity.getNavigationBarHeight(),
                    )
                }
            }
            else ItemLongPress.onItemLongPress(v, item)
            onDragStart(v)
            true
        }
    }

    fun recycle(item: LauncherItem) {
        blurBG.drawable = null
        imageView.setImageDrawable(null)
        contentView.icon = null
        contentView.label = null
        item.icon.offload()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T: View, R> T.hideIfNullOr(value: R?, block: T.(R) -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (value == null) {
        isVisible = false
    } else {
        isVisible = true
        block(value)
    }
}