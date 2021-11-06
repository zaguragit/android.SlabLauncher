package io.posidon.android.slablauncher.ui.view.tile

import android.graphics.Rect
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
    val iconPosition = Rect()

    private inline fun mix(x: Float, y: Float, f: Float) = f * y + (1 - f) * x

    fun setDefaultToNotification(f: Float, width: Int, height: Int) {
        val sideMargin = view.sideMargin
        val tmpTextBounds = Rect()
        view.getLabelBounds(tmpTextBounds)

        val innerWidth = width - sideMargin.toInt() * 2

        val originalLabelWidth = tmpTextBounds.width().coerceAtMost(innerWidth)
        val originalLabelHeight = tmpTextBounds.height()

        val iconSize = mix(min(width, height) / 2f, view.smallIconSize, f)

        val x = mix((width - iconSize) / 2f, sideMargin, f).toInt()
        val y = mix((height - iconSize - originalLabelHeight) / 2f, sideMargin, f).toInt()

        val iconBottom = (y + iconSize).toInt()

        iconPosition.set(
            x, y, (x + iconSize).toInt(), iconBottom
        )

        labelX = mix(
            (width - originalLabelWidth) / 2f,
            sideMargin * 1.5f + iconSize,
            f
        )
        labelY = mix(
            height - (height - (y + iconSize) - originalLabelHeight) / 2f,
            (originalLabelHeight.toFloat() + iconSize) / 2f + sideMargin,
            f
        )

        val maxLabelWidth = innerWidth - (labelX - sideMargin)

        val originalTitleHeight = view.getExtraTitleHeight()

        extraTitleX = sideMargin
        extraTextX = sideMargin

        extraTitleY = mix(
            height.toFloat(),
            iconBottom.toFloat() + originalTitleHeight,
            f
        )

        val originalTextHeight = view.getExtraTextHeight()

        extraTextY = extraTitleY + originalTextHeight + sideMargin / 2f

        view.updateEllipsis(maxLabelWidth, innerWidth.toFloat())
    }
}