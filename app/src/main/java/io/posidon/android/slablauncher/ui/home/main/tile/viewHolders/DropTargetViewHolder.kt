package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.HomeArea.Companion.ITEM_HEIGHT

class DropTargetViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    init {
        itemView.updateLayoutParams {
            height = ITEM_HEIGHT.toPixels(itemView)
        }
        itemView.background = itemView.context.getDrawable(R.drawable.tile_drop_target)
        itemView.backgroundTintMode = PorterDuff.Mode.MULTIPLY
    }
}

fun bindDropTargetViewHolder(
    holder: DropTargetViewHolder,
) {
    holder.itemView.backgroundTintList = ColorStateList.valueOf(ColorTheme.adjustColorForContrast(ColorPalette.wallColor, ColorPalette.wallColor))
}