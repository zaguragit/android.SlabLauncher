package io.posidon.android.slablauncher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.alpha

class WindowView : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private val paint = Paint().apply {
        isAntiAlias = false
        color = 0xff000000.toInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    override fun setAlpha(alpha: Float) {
        paint.color = paint.color and 0xffffff or ((0xff * alpha).toInt() shl 24)
        invalidate()
    }

    override fun getAlpha() = paint.color.alpha / 255f

    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}