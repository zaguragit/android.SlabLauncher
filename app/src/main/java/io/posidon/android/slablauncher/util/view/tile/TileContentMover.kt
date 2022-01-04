package io.posidon.android.slablauncher.util.view.tile

import android.content.Context
import android.graphics.Rect
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.home.pinned.TileArea
import posidon.android.conveniencelib.Device
import kotlin.math.min

class TileContentMover(
    val view: TileContentView
) {
    var labelX: Float = 0f
        private set
    var labelY: Float = 0f
        private set

    var extraTitleX: Float = 0f
        private set
    var extraTitleY: Float = 0f
        private set
    var extraTextX: Float = 0f
        private set
    var extraTextY: Float = 0f
        private set

    var markTextX: Float = 0f
        private set
    var markTextY: Float = 0f
        private set

    val iconPosition = Rect()

    var extraTextBoxHeight: Float = 0f
        private set

    private inline fun mix(x: Float, y: Float, f: Float) = f * y + (1 - f) * x

    fun setDefaultToNotification(f: Float, width: Int, height: Int) {
        val sideMargin = view.sideMargin
        val tmpTextBounds = Rect()
        view.getLabelBounds(tmpTextBounds)

        val innerWidth = width - sideMargin.toInt() * 2

        val originalLabelWidth = tmpTextBounds.width().coerceAtMost(innerWidth)
        val originalLabelHeight = tmpTextBounds.height()

        val iconSize = mix(min(width, height) / 2f, view.smallIconSize, f)

        val x = sideMargin.toInt()//mix((width - iconSize) / 2f, sideMargin, f).toInt()
        val y = sideMargin.toInt()//mix((height - iconSize - originalLabelHeight) / 2f, sideMargin, f).toInt()

        val iconBottom = (y + iconSize).toInt()

        iconPosition.set(
            x, y, (x + iconSize).toInt(), iconBottom
        )

        labelX = mix(
            sideMargin * 1.5f,//(width - originalLabelWidth) / 2f,
            sideMargin * 1.5f + iconSize,
            f
        )
        labelY = mix(
            height - (height - (y + iconSize) - originalLabelHeight) / 2f,
            (originalLabelHeight.toFloat() + iconSize) / 2f + sideMargin,
            f
        )

        val maxLabelWidth = innerWidth - (labelX - sideMargin)

        extraTitleX = sideMargin
        extraTextX = sideMargin

        val originalTitleHeight = view.getExtraTitleHeight()
        extraTitleY = mix(
            height.toFloat(),
            iconBottom.toFloat() + originalTitleHeight,
            f
        )

        val originalTextHeight = view.getExtraTextHeight()
        extraTextY = extraTitleY + originalTextHeight + sideMargin / 2f

        extraTextBoxHeight = (height - extraTitleY - sideMargin).coerceAtLeast(0f)

        view.getMarkBounds(tmpTextBounds)
        markTextX = width - tmpTextBounds.width().toFloat()
        markTextY = tmpTextBounds.height().toFloat()

        view.updateEllipsis(maxLabelWidth, innerWidth.toFloat())
    }

    companion object {
        fun calculateBigIconSize(context: Context): Float {
            val tileMargin = context.resources.getDimension(R.dimen.item_card_margin)
            val tileWidth = (Device.screenWidth(context) - tileMargin * 2) / TileArea.COLUMNS - tileMargin * 2
            val tileHeight = tileWidth / TileArea.WIDTH_TO_HEIGHT
            return min(tileWidth, tileHeight) / 2f
        }
    }
}