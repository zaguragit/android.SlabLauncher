package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings

class ShortcutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)!!

    fun onBind(
        item: LauncherItem,
        graphicsLoader: GraphicsLoader,
        settings: Settings,
    ) {
        val iconData = graphicsLoader.load(itemView.context, item)
        icon.setImageDrawable(iconData.icon)
        itemView.setOnClickListener {
            item.open(it.context, it)
        }
        icon.alpha = 1f
        if (settings.doMonochrome) {
            icon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
            if (item is App && !itemView.context.isUserRunning(item.userHandle)) {
                icon.alpha = 0.7f
            }
        } else icon.colorFilter = null
    }
}
