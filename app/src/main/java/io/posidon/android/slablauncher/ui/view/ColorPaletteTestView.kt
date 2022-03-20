package io.posidon.android.slablauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette

class ColorPaletteTestView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var palette: ColorPalette? = null
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        val palette = palette ?: return
        val all = arrayOf(
            palette.neutralVeryDark,
            palette.neutralDark,
            palette.neutralMedium,
            palette.neutralLight,
            palette.neutralVeryLight,
            palette.primary,
            palette.secondary,
        )
        val w = width.toFloat() / all.size
        for (i in all.indices) {
            paint.color = all[i]
            canvas.drawRect(i * w, 0f, (i + 1) * w, height.toFloat(), paint)
        }
    }
}