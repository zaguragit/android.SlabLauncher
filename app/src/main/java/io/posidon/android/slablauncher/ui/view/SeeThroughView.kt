package io.posidon.android.slablauncher.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import io.posidon.android.slablauncher.R
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.getNavigationBarHeight
import posidon.android.conveniencelib.getStatusBarHeight

class SeeThroughView : View {
    constructor(c: Context) : super(c)
    constructor(c: Context, a: AttributeSet?) : this(c, a, 0, 0)
    constructor(c: Context, a: AttributeSet?, da: Int) : this(c, a, da, 0)
    constructor(c: Context, a: AttributeSet?, da: Int, dr: Int) : super(c, a, da, dr)

    var drawable: Drawable? = null
        set(value) {
            field = value
            updateBounds()
            invalidate()
        }

    var offset = 0f
        set(value) {
            field = value
            updateBounds()
            invalidate()
        }

    private val lastScreenLocation = IntArray(2)

    private var bounds = Rect()

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateBounds()
        invalidate()
    }

    fun updateBounds() {
        val d = drawable
        if (d != null) {
            val dw = Device.screenWidth(context)
            val h = Device.screenHeight(context) +
                ((context as? Activity)?.getNavigationBarHeight()?.toFloat() ?: 0f)
            val w = (h * d.intrinsicWidth / d.intrinsicHeight.toFloat()).coerceAtLeast(dw.toFloat())
            getLocationOnScreen(lastScreenLocation)
            val l = lastScreenLocation[0] + offset * (w - dw)
            val t = lastScreenLocation[1]
            bounds.set(-l.toInt(), -t, (w - l).toInt(), (h - t).toInt())
        } else {
            bounds.set(0, 0, 0, 0)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas) {
        val d = drawable
        if (d != null) {
            val (x, y) = lastScreenLocation
            getLocationOnScreen(lastScreenLocation)
            if (lastScreenLocation[0] != x || lastScreenLocation[1] != y) {
                updateBounds()
            }
            d.bounds = bounds
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