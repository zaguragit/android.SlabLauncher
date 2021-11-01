package io.posidon.android.slablauncher.util.drawable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

internal class FastBitmapDrawable(
    val bitmap: Bitmap
) : Drawable() {

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getIntrinsicWidth() = bitmap.width
    override fun getIntrinsicHeight() = bitmap.height
    override fun getMinimumWidth() = bitmap.width
    override fun getMinimumHeight() = bitmap.height
}