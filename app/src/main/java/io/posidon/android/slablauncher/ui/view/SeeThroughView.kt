package io.posidon.android.slablauncher.ui.view

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.getNavigationBarHeight

class SeeThroughView : View {
    constructor(c: Context) : super(c)
    constructor(c: Context, a: AttributeSet?) : this(c, a, 0, 0)
    constructor(c: Context, a: AttributeSet?, da: Int) : this(c, a, da, 0)
    constructor(c: Context, a: AttributeSet?, da: Int, dr: Int) : super(c, a, da, dr)

    var drawable: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }

    var offset = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val lastScreenLocation = IntArray(2)

    override fun isDirty(): Boolean {
        return super.isDirty() || run {
            val location = IntArray(2)
            getLocationOnScreen(location)
            !lastScreenLocation.contentEquals(location)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val d = drawable
        if (d != null) {
            val dw = Device.screenWidth(context)
            val h = Device.screenHeight(context) + ((context as? Activity)?.getNavigationBarHeight() ?: 0)
            val w = (h * (d.intrinsicWidth / d.intrinsicHeight.toFloat())).toInt()
            getLocationOnScreen(lastScreenLocation)
            val l = lastScreenLocation[0] + (offset * (w - dw)).toInt()
            val t = lastScreenLocation[1]
            d.setBounds(-l, -t, w - l, h - t)
            d.draw(canvas)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            calculate(suggestedMinimumWidth, widthMeasureSpec),
            calculate(suggestedMinimumHeight, heightMeasureSpec),
        )
    }

    private fun calculate(minSize: Int, measureSpec: Int): Int {
        var result = minSize
        val mode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        when (mode) {
            MeasureSpec.UNSPECIFIED -> result = minSize
            MeasureSpec.AT_MOST -> result = minSize
            MeasureSpec.EXACTLY -> result = specSize
        }
        return result
    }
}