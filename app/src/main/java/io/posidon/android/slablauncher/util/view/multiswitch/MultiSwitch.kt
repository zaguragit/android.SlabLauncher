package io.posidon.android.slablauncher.util.view.multiswitch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class MultiSwitch : View {
    constructor(c: Context) : super(c)
    constructor(c: Context, a: AttributeSet?) : this(c, a, 0, 0)
    constructor(c: Context, a: AttributeSet?, da: Int) : this(c, a, da, 0)
    constructor(c: Context, a: AttributeSet?, da: Int, dr: Int) : super(c, a, da, dr)

    var backgroundColor = 0
        private set
    
    var borderColor = 0
        set(value) {
            field = value
            invalidate()
        }
    
    var onColor = 0
        set(value) {
            field = value
            invalidate()
        }
    var unsafeColor = 0
        set(value) {
            field = value
            invalidate()
        }

    var state = 0
        set(value) {
            field = value
            stateChangeListener?.invoke(this, value)
            invalidate()
        }
    var states = 1
        set(value) {
            field = value
            invalidate()
        }
    
    var radius = 0f
        set(value) {
            field = value
            invalidate()
        }
    
    fun setOnStateChangeListener(listener: ((MultiSwitch, Int) -> Unit)?) {
        stateChangeListener = listener
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        invalidate()
    }

    private var stateChangeListener: ((MultiSwitch, Int) -> Unit)? = null

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    
    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas) {
        paint.color = backgroundColor
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)

        val cellWidth = width / states.toFloat()
        paint.color = onColor
        for (i in 0..state) {
            canvas.drawRoundRect(cellWidth * i, 0f, cellWidth * (i + 1), height.toFloat(), radius, radius, paint)
        }
    }

    private inline val heightToWidth get() = 1f / states

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec((MeasureSpec.getSize(heightMeasureSpec) / heightToWidth).toInt(), MeasureSpec.EXACTLY),
            heightMeasureSpec,
        )
    }
}