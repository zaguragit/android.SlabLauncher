package io.posidon.android.slablauncher.util.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View

internal class FastColorDrawable(
    val color: Int
) : Drawable() {

    override fun draw(canvas: Canvas) = canvas.drawColor(realColor)

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {
        val baseAlpha = color ushr 24
        val useAlpha = baseAlpha * alpha shr 8
        val useColor = realColor shl 8 ushr 8 or (useAlpha shl 24)
        if (realColor != useColor) {
            realColor = useColor
            invalidateSelf()
        }
    }
    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getMinimumWidth() = 0
    override fun getMinimumHeight() = 0

    override fun getAlpha() = realColor ushr 24

    private var realColor = color
}

internal inline fun View.setBackgroundColorFast(color: Int) {
    background = FastColorDrawable(color)
}