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
    var borderWidth = 0f
        set(value) {
            field = value
            invalidate()
        }

    var onColor = 0
        set(value) {
            field = value
            invalidate()
        }
    var offColor = 0
        set(value) {
            field = value
            invalidate()
        }
    var unsafeOnColor = 0
        set(value) {
            field = value
            invalidate()
        }
    var unsafeOffColor = 0
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

    var unsafeLevel = -1
        set(value) {
            field = value
            invalidate()
        }

    var radius = 0f
        set(value) {
            field = value
            invalidate()
        }

    var smallRadius = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cellMargin = 0f
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
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        paint.color = borderColor
        val b = borderWidth / 2f
        canvas.drawRoundRect(b, b, width.toFloat() - b, height.toFloat() - b, radius, radius, paint)
        paint.style = Paint.Style.FILL

        val cellWidth = (width - cellMargin) / states.toFloat()
        paint.color = onColor
        val radius = radius.coerceAtMost(height / 2f).coerceAtMost(width / 2f)
        for (i in 0 until states) {
            val lr = when (i) {
                0 -> radius
                else -> smallRadius
            }
            val rr = when (i) {
                states - 1 -> radius
                else -> smallRadius
            }
            if (i == state + 1) {
                paint.color = if (i >= unsafeLevel && unsafeLevel != -1) unsafeOffColor else offColor
            } else if (i == unsafeLevel) {
                paint.color = if (i > state) unsafeOffColor else unsafeOnColor
            }
            canvas.drawRoundRect(
                cellWidth * i + cellMargin,
                cellMargin,
                cellWidth * (i + 1) - rr,
                height.toFloat() - cellMargin,
                lr,
                lr,
                paint
            )
            canvas.drawRoundRect(
                cellWidth * i + cellMargin + lr,
                cellMargin,
                cellWidth * (i + 1),
                height.toFloat() - cellMargin,
                rr,
                rr,
                paint
            )
        }
    }

    private inline val cellHeightToWidth get() = 3f / 4f
    private inline val heightToWidth get() = cellHeightToWidth / states

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec((MeasureSpec.getSize(heightMeasureSpec) / heightToWidth).toInt(), MeasureSpec.EXACTLY),
            heightMeasureSpec,
        )
    }
}