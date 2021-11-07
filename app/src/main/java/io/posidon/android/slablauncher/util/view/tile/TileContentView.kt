package io.posidon.android.slablauncher.util.view.tile

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.view.doOnPreDraw
import posidon.android.conveniencelib.SpringInterpolator
import posidon.android.conveniencelib.dp
import posidon.android.conveniencelib.sp

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

    var label: String? = null
        set(value) {
            field = value
            invalidate()
        }

    var extraTitle: String? = null
        set(value) {
            field = value
            updateNotificationState()
            invalidate()
        }

    var extraText: String? = null
        set(value) {
            field = value
            updateNotificationState()
            invalidate()
        }

    fun setExtraWithAnimation(extraTitle: String?, extraText: String?) {
        this.extraTitle = extraTitle
        this.extraText = extraText
        updateNotificationStateWithAnimation()
    }

    var titleColor: Int
        get() = titlePaint.color
        set(value) {
            labelPaint.color = value
            titlePaint.color = value
            invalidate()
        }

    var textColor: Int
        get() = textPaint.color
        set(value) {
            textPaint.color = value
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

    private val labelPaint = Paint().apply {
        textSize = sp(12)
        isAntiAlias = true
        isLinearText = true
        isSubpixelText = true
    }
    private val titlePaint = Paint().apply {
        textSize = sp(12)
        typeface = Typeface.create(typeface, Typeface.BOLD)
        isAntiAlias = true
        isLinearText = true
        isSubpixelText = true
    }
    private val textPaint = Paint().apply {
        textSize = sp(12)
        isAntiAlias = true
        isLinearText = true
        isSubpixelText = true
    }

    private var notificationness = 0f
    private var notificationnessAnimator: ValueAnimator? = null

    private fun createNotificationnessAnimator(target: Float) =
        ValueAnimator.ofFloat(notificationness, target).apply {
            addUpdateListener {
                notificationness = it.animatedValue as Float
                mover.setDefaultToNotification(notificationness, width, height)
                invalidate()
            }
            duration = 640L
            interpolator = SpringInterpolator()
        }

    private fun updateNotificationState() {
        if (extraText == null && extraTitle == null) {
            notificationnessAnimator?.cancel()
            notificationness = 0f
            mover.setDefaultToNotification(notificationness, width, height)
        } else {
            notificationnessAnimator?.cancel()
            notificationness = 1f
            mover.setDefaultToNotification(notificationness, width, height)
        }
    }

    private fun updateNotificationStateWithAnimation() {
        if (extraText == null && extraTitle == null) {
            if (notificationness != 0f) {
                notificationnessAnimator?.cancel()
                notificationnessAnimator = createNotificationnessAnimator(0f).apply {
                    start()
                }
            }
        } else {
            if (notificationness != 1f) {
                notificationnessAnimator?.cancel()
                notificationnessAnimator = createNotificationnessAnimator(1f).apply {
                    start()
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas) {
        icon?.also {
            drawDrawable(canvas, it, mover.iconPosition)
        }
        realLabel?.also {
            canvas.drawText(it, 0, it.length, mover.labelX, mover.labelY, labelPaint)
        }
        realExtraTitle?.also {
            canvas.drawText(it, 0, it.length, mover.extraTitleX, mover.extraTitleY, titlePaint)
        }
        realExtraText?.also {
            canvas.drawText(it, 0, it.length, mover.extraTextX, mover.extraTextY, textPaint)
        }
    }

    private fun drawDrawable(canvas: Canvas, drawable: Drawable, rect: Rect) {
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(rect.left, rect.top, rect.right, rect.bottom)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
    }

    internal fun getLabelBounds(bounds: Rect) = label?.let { labelPaint.getTextBounds(it, 0, it.length, bounds) } ?: bounds.setEmpty()

    internal fun getExtraTitleHeight() = if (extraTitle == null) 0f else titlePaint.descent() - titlePaint.ascent()
    internal fun getExtraTextHeight() = if (extraText == null) 0f else textPaint.descent() - textPaint.ascent()

    internal val smallIconSize: Float
        get() = dp(24)
    internal val sideMargin: Float
        get() = dp(8)


    private var realLabel: String? = label
    private var realExtraTitle: String? = extraTitle
    private var realExtraText: String? = extraText
    fun updateEllipsis(maxLabelWidth: Float, maxExtraWidth: Float) {
        realLabel = label?.let { ellipsize(it, labelPaint, maxLabelWidth) }
        realExtraTitle = extraTitle?.let { ellipsize(it, titlePaint, maxExtraWidth) }
        realExtraText = extraText?.let { ellipsize(it, textPaint, maxExtraWidth) }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        val l = cutOff(text, paint, maxWidth)
        val ol = text.length
        if (l >= ol) {
            return text
        }
        return StringBuilder(text)
            .delete((l - 1).coerceAtLeast(0), ol)
            .apply { while (lastOrNull()?.isWhitespace() == true) deleteAt(lastIndex) }
            .append('…')
            .toString()
    }

    private fun cutOff(text: String, paint: Paint, maxWidth: Float): Int {
        val origLength = text.length
        val widths = FloatArray(origLength)
        paint.getTextWidths(text, widths)
        var sum = widths.sum()
        var i = 0
        while (sum > maxWidth && i < origLength) {
            i++
            val ci = widths.size - i
            sum -= widths[ci]
        }
        return text.length - i
    }

    init {
        doOnPreDraw {
            mover.setDefaultToNotification(notificationness, it.width, it.height)
        }
    }
}