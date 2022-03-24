package io.posidon.android.slablauncher.util.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View

internal class FastColorDrawable(
    color: Int
) : Drawable() {

    override fun draw(canvas: Canvas) = canvas.drawPaint(paint)

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
        invalidateSelf()
    }

    override fun getColorFilter() = paint.colorFilter

    override fun getMinimumWidth() = 0
    override fun getMinimumHeight() = 0

    override fun getAlpha() = paint.alpha

    private val paint = Paint().apply { this.color = color }

    override fun getConstantState() = ConstantState(paint.color)

    class ConstantState(val color: Int) : Drawable.ConstantState() {
        override fun newDrawable() = FastColorDrawable(color)
        override fun getChangingConfigurations() = 0
    }
}

internal inline fun View.setBackgroundColorFast(color: Int) {
    background = FastColorDrawable(color)
}