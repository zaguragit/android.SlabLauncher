package io.posidon.android.slablauncher.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.posidon.ksugar.delegates.observable
import kotlin.properties.Delegates

class FlagView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var colors by Delegates.observable(intArrayOf(), this::invalidate)

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        if (colors.isEmpty())
            return
        val stripeHeight = height.toFloat() / colors.size
        val w = width.toFloat()
        colors.forEachIndexed { i, color ->
            println(color)
            canvas.drawRect(0f, stripeHeight * i, w, stripeHeight * (i + 1), paint.apply {
                this.color = color
            })
        }
    }
}