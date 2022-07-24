package io.posidon.android.slablauncher.util.drawable

import android.graphics.*
import android.graphics.drawable.Drawable

internal class ContactDrawable(
    val color: Int,
    val lightColor: Int,
    val character: Char,
    val textPaint: Paint
) : Drawable() {

    val paint = Paint()

    override fun draw(canvas: Canvas) {
        paint.shader = LinearGradient(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), color, lightColor, Shader.TileMode.REPEAT)
        canvas.drawColor(color)
        canvas.drawPaint(paint)
        val x = bounds.width() / 2f
        val y = (bounds.height() - (textPaint.descent() + textPaint.ascent())) / 2f
        canvas.drawText(charArrayOf(character), 0, 1, x, y, textPaint)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.OPAQUE

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getIntrinsicWidth() = 128
    override fun getIntrinsicHeight() = 128
    override fun getMinimumWidth() = 128
    override fun getMinimumHeight() = 128
}