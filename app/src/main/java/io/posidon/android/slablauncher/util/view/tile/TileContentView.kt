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
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.doOnLayout
import posidon.android.conveniencelib.dp
import posidon.android.conveniencelib.onEnd
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

    var extraTitle: String?
        get() = _extraTitle
        set(value) {
            _extraTitle = value
            updateNotificationState()
            invalidate()
        }

    var extraText: String?
        get() = _extraText
        set(value) {
            _extraText = value
            updateNotificationState()
            invalidate()
        }

    fun setExtraWithAnimation(extraTitle: String?, extraText: String?) {
        if (extraTitle == null && extraText == null) {
            if (notificationness != 0f) {
                notificationnessAnimator?.cancel()
                notificationnessAnimator = createNotificationnessAnimator(0f).apply {
                    onEnd {
                        _extraTitle = null
                        _extraText = null
                        invalidate()
                    }
                    start()
                }
            }
        } else {
            _extraTitle = extraTitle
            _extraText = extraText
            if (notificationness != 1f) {
                notificationnessAnimator?.cancel()
                notificationnessAnimator = createNotificationnessAnimator(1f).apply {
                    start()
                }
            }
        }
    }

    var labelColor: Int
        get() = labelPaint.color
        set(value) {
            labelPaint.color = value
            invalidate()
        }

    var titleColor: Int
        get() = titlePaint.color
        set(value) {
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

    private var _extraTitle: String? = null
    private var _extraText: String? = null

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
            duration = 240L
            interpolator = AccelerateDecelerateInterpolator()
        }

    private fun updateNotificationState() {
        if (_extraText == null && _extraTitle == null) {
            notificationnessAnimator?.cancel()
            notificationness = 0f
            doOnLayout {
                mover.setDefaultToNotification(notificationness, width, height)
            }
        } else {
            notificationnessAnimator?.cancel()
            notificationness = 1f
            doOnLayout {
                mover.setDefaultToNotification(notificationness, width, height)
            }
        }
    }

    private val tmpPaint = Paint()
    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas) {
        icon?.also {
            drawDrawable(canvas, it, mover.iconPosition)
        }
        realLabel?.also {
            canvas.drawText(it, 0, it.length, mover.labelX, mover.labelY, labelPaint)
        }
        realExtraTitle?.also {
            tmpPaint.set(titlePaint)
            tmpPaint.alpha = (tmpPaint.alpha * notificationness).toInt()
            canvas.drawText(it, 0, it.length, mover.extraTitleX, mover.extraTitleY, tmpPaint)
        }
        realExtraText?.also {
            tmpPaint.set(textPaint)
            tmpPaint.alpha = (tmpPaint.alpha * notificationness).toInt()
            for (i in it.indices) {
                val line = it[i]
                canvas.drawText(line, 0, line.length, mover.extraTextX, mover.extraTextY + getExtraTextHeight() * i, tmpPaint)
            }
        }
    }

    private fun drawDrawable(canvas: Canvas, drawable: Drawable, rect: Rect) {
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(rect.left, rect.top, rect.right, rect.bottom)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
    }

    internal fun getLabelBounds(bounds: Rect) = label?.let { labelPaint.getTextBounds(it, 0, it.length, bounds) } ?: bounds.setEmpty()

    internal fun getExtraTitleHeight() = if (_extraTitle == null) 0f else titlePaint.descent() - titlePaint.ascent()
    internal fun getExtraTextHeight() = if (_extraText == null) 0f else textPaint.descent() - textPaint.ascent()

    internal fun getMaxExtraTextLines() = getExtraTextHeight().also { if (it == 0f) return 0 }.let { mover.extraTextBoxHeight / it }.toInt()

    internal val smallIconSize: Float
        get() = dp(24)
    internal val sideMargin: Float
        get() = dp(8)


    private var realLabel: String? = label
    private var realExtraTitle: String? = _extraTitle
    private var realExtraText: List<String>? = _extraText?.let { listOf(it) }
    fun updateEllipsis(maxLabelWidth: Float, maxExtraWidth: Float) {
        realLabel = label?.let { ellipsize(it, labelPaint, maxLabelWidth) }
        realExtraTitle = _extraTitle?.let { ellipsize(it, titlePaint, maxExtraWidth) }
        realExtraText = _extraText?.let { treatMultiline(it, textPaint, maxExtraWidth) }
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

    private fun treatMultiline(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (maxWidth < 0) return listOf(text)
        val maxLines = getMaxExtraTextLines()
        val lines = text.lines().let { if (it.size > maxLines) it.subList(it.size - maxLines, it.size) else it }.flatMapTo(ArrayList()) { widthSplit(it, paint, maxWidth) }
        var removedLine = false
        if (lines.size > maxLines) {
            removedLine = true
            while (lines.size > maxLines)
                lines.removeLast()
        }
        val lastLine = lines.lastOrNull() ?: return lines
        val l = cutOff(lastLine, paint, maxWidth)
        val ol = lastLine.length
        if (l < ol || removedLine) {
            lines[lines.lastIndex] = StringBuilder(lastLine)
                .delete((l - 1).coerceAtLeast(0), ol)
                .apply { while (lastOrNull()?.isWhitespace() == true) deleteAt(lastIndex) }
                .append('…')
                .toString()
        }
        return lines
    }

    private tailrec fun widthSplit(text: String, paint: Paint, maxWidth: Float, list: MutableList<String> = ArrayList()): MutableList<String> {
        val c = cutOff(text, paint, maxWidth)
        if (c == text.length) {
            list += text
            return list
        }
        list += text.substring(0, c)
        return widthSplit(text.substring(c, text.length), paint, maxWidth, list)
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mover.setDefaultToNotification(notificationness, right - left, bottom - top)
    }
}