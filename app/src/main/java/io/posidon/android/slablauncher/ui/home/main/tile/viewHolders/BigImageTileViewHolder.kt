package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.Settings

class BigImageTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TileViewHolder {

    private val card = itemView.findViewById<CardView>(R.id.card)!!

    private val backgroundImage = itemView.findViewById<ImageView>(R.id.background_image)!!

    private fun updateGraphics(
        iconData: IconData<GraphicsLoader.Extra>,
    ) {
        itemView.post {
            backgroundImage.setImageDrawable(iconData.extra.tile)
        }
    }

    override fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
        itemView.isVisible = true
        card.setCardBackgroundColor(ColorTheme.cardBG)

        graphicsLoader.load(itemView.context, item, ::updateGraphics)

        itemView.setOnLongClickListener { v ->
            onLongPress(v, item, activity, graphicsLoader, onDragStart)
            true
        }
    }

    fun onLongPress(
        v: View,
        item: LauncherItem,
        activity: Activity,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
        if (item is App) {
            val color = graphicsLoader.load(itemView.context, item).extra.color
            val backgroundColor = ColorTheme.tintCard(color)
            ItemLongPress.onItemLongPress(
                v,
                backgroundColor,
                ColorTheme.titleColorForBG(backgroundColor),
                item,
                activity.getNavigationBarHeight(),
                graphicsLoader,
            )
        } else ItemLongPress.onItemLongPress(v, item)
        onDragStart(v)
    }

    override fun recycle() {}
}