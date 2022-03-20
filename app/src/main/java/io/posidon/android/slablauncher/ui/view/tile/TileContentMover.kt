package io.posidon.android.slablauncher.ui.view.tile

import android.content.Context
import android.graphics.Rect
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import posidon.android.conveniencelib.Device
import kotlin.math.min

class TileContentMover(
    val view: TileContentView
) {

    val iconPosition = Rect()

    fun updateDimensions(width: Int, height: Int) {
        val iconSize = min(width, height) / 1.25f

        val x = (width - iconSize) / 2f
        val y = (height - iconSize) / 2f

        val iconBottom = (y + iconSize).toInt()

        iconPosition.set(
            x.toInt(), y.toInt(), (x + iconSize).toInt(), iconBottom
        )
    }

    companion object {
        fun calculateBigIconSize(context: Context): Float {
            val tileMargin = context.resources.getDimension(R.dimen.item_card_margin)
            val tileWidth = (Device.screenWidth(context) - tileMargin * 2) / HomeArea.COLUMNS - tileMargin * 2
            val tileHeight = tileWidth / HomeArea.WIDTH_TO_HEIGHT
            return min(tileWidth, tileHeight) / 1.25f
        }
    }
}