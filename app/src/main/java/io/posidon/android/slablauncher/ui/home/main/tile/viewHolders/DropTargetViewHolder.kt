package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.DashArea.Companion.WIDTH_TO_HEIGHT
import io.posidon.android.slablauncher.util.view.HorizontalAspectRatioLayout

class DropTargetViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    val aspect = itemView.findViewById<HorizontalAspectRatioLayout>(R.id.aspect)!!.apply {
        widthToHeight = WIDTH_TO_HEIGHT
    }

    init {
        itemView.background = itemView.context.getDrawable(R.drawable.tile_drop_target)
        itemView.backgroundTintMode = PorterDuff.Mode.MULTIPLY
    }
}

fun bindDropTargetViewHolder(
    holder: DropTargetViewHolder,
) {
    holder.itemView.backgroundTintList = ColorStateList.valueOf(ColorTheme.adjustColorForContrast(ColorPalette.wallColor, ColorPalette.wallColor))
}