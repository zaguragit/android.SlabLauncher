package io.posidon.android.slablauncher.ui.view.tile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

//
// DEFAULT
// ._________________________.
// |                         |
// |                         |
// |          icon           |
// |                         |
// |                         |
// |          label          |
// |_________________________|
//
// NOTIFICATION
// ._________________________.
// | icon  label             |
// |                         |
// | extraTitle              |
// | extraText               |
// |                         |
// |                         |
// |_________________________|
//

class TileContentView : View {

    var icon: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }

    var widthToHeight = 1f

    constructor(context: Context)
        : super(context)
    constructor(context: Context, attrs: AttributeSet?)
        : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
        : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec((MeasureSpec.getSize(widthMeasureSpec) / widthToHeight).toInt(), MeasureSpec.EXACTLY),
        )
    }

    private val mover = TileContentMover(this)

    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas) {
        icon?.also {
            drawDrawable(canvas, it, mover.iconPosition)
        }
    }

    private fun drawDrawable(canvas: Canvas, drawable: Drawable, rect: Rect) {
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(rect.left, rect.top, rect.right, rect.bottom)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mover.updateDimensions( right - left, bottom - top)
    }
}