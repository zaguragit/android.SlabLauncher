package io.posidon.android.slablauncher.ui.view.multiswitch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.posidon.ksugar.delegates.observable
import kotlin.properties.Delegates

class MultiSwitch : View {
    constructor(c: Context) : super(c)
    constructor(c: Context, a: AttributeSet?) : this(c, a, 0, 0)
    constructor(c: Context, a: AttributeSet?, da: Int) : this(c, a, da, 0)
    constructor(c: Context, a: AttributeSet?, da: Int, dr: Int) : super(c, a, da, dr)

    var backgroundColor = 0
        private set

    var borderColor by Delegates.observable(0) { _ -> invalidate() }
    var borderWidth by Delegates.observable(0f) { _ -> invalidate() }
    var onColor by Delegates.observable(0) { _ -> invalidate() }
    var offColor by Delegates.observable(0) { _ -> invalidate() }
    var unsafeOnColor by Delegates.observable(0) { _ -> invalidate() }
    var unsafeOffColor by Delegates.observable(0) { _ -> invalidate() }
    var radius by Delegates.observable(0f) { _ -> invalidate() }
    var smallRadius by Delegates.observable(0f) { _ -> invalidate() }
    var cellMargin by Delegates.observable(0f) { _ -> invalidate() }

    var state by Delegates.observable(0) { new ->
        stateChangeListener?.invoke(this, new)
        invalidate()
    }

    var states by Delegates.observable(1) { _ -> invalidate() }
    var unsafeLevel by Delegates.observable(-1) { _ -> invalidate() }

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