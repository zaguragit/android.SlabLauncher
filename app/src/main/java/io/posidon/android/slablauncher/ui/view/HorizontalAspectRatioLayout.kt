package io.posidon.android.slablauncher.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class HorizontalAspectRatioLayout : FrameLayout {

    var widthToHeight = 1f

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec((MeasureSpec.getSize(widthMeasureSpec) / widthToHeight).toInt(), MeasureSpec.EXACTLY),
        )
    }
}