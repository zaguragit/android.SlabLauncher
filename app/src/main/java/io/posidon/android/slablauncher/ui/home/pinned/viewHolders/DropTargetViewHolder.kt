package io.posidon.android.slablauncher.ui.home.pinned.viewHolders

import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.WIDTH_TO_HEIGHT
import io.posidon.android.slablauncher.ui.home.pinned.acrylicBlur
import io.posidon.android.slablauncher.util.view.HorizontalAspectRatioLayout
import io.posidon.android.slablauncher.util.view.SeeThroughView

class DropTargetViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    val aspect = itemView.findViewById<HorizontalAspectRatioLayout>(R.id.aspect)!!.apply {
        widthToHeight = WIDTH_TO_HEIGHT
    }
}

fun bindDropTargetViewHolder(
    holder: DropTargetViewHolder,
) {
    val backgroundColor = ColorTheme.appCardBase
    holder.card.setCardBackgroundColor(backgroundColor)
    holder.blurBG.drawable = acrylicBlur?.smoothBlurDrawable
}