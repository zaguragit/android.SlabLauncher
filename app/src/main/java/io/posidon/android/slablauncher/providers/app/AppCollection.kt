package io.posidon.android.slablauncher.providers.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.os.UserHandle
import androidx.core.graphics.*
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import io.posidon.android.launcherutils.IconTheming
import io.posidon.android.launcherutils.appLoading.AppLoader
import io.posidon.android.launcherutils.appLoading.IconConfig
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeIcons
import io.posidon.android.slablauncher.util.storage.DoReshapeAdaptiveIconsSetting.doReshapeAdaptiveIcons
import io.posidon.android.slablauncher.util.storage.Settings
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class AppCollection(
    appCount: Int,
    val settings: Settings
) : AppLoader.AppCollection<AppCollection.ExtraIconData> {
    val list = ArrayList<App>(appCount)
    val byName = HashMap<String, MutableList<App>>()

    inline operator fun get(i: Int) = list[i]
    inline val size get() = list.size

    private val tmpLab = DoubleArray(3)

    private val doReshapeAdaptiveIcons = settings.doReshapeAdaptiveIcons
    private val doMonochromeIcons = settings.doMonochromeIcons

    override fun addApp(
        context: Context,
        packageName: String,
        name: String,
        profile: UserHandle,
        label: String,
        icon: Drawable,
        extra: AppLoader.ExtraAppInfo<ExtraIconData>,
    ) {
        val extraIconData = extra.extraIconData
        if (!extra.isUserRunning) {
            icon.convertToGrayscale()
            if (doMonochromeIcons) {
                icon.alpha = 128
            }
            extraIconData.color = run {
                val a = (extraIconData.color.luminance * 255).toInt()
                Color.rgb(a, a, a)
            }
            val b = extraIconData.background
            if (b is FastColorDrawable) {
                extraIconData.background = makeDrawable(extraIconData.color)
            } else b?.convertToGrayscale()
        } else if (doMonochromeIcons) {
            icon.convertToGrayscale()
            extraIconData.color = extraIconData.color and 0xffffff
            val b = extraIconData.background
            /*if (b is FastColorDrawable) {
                val a = (extraIconData.color.luminance * 255).toInt()
                extraIconData.background = makeDrawable(Color.rgb(a, a, a))
            } else b?.convertToGrayscale()*/
        }

        val app = App(
            packageName,
            name,
            profile,
            label,
            icon,
            extraIconData.background,
            extraIconData.color
        )

        list.add(app)
        putInMap(app)
    }

    override fun modifyIcon(
        icon: Drawable,
        packageName: String,
        name: String,
        profile: UserHandle,
        expandableBackground: Drawable?
    ): Pair<Drawable, ExtraIconData> {
        var color = 0
        var icon = icon
        var background: Drawable? = null

        when {
            expandableBackground != null -> {
                background = expandableBackground
                val palette = Palette.from(background.toBitmap(8, 8)).generate()
                val d = palette.dominantSwatch
                color = d?.rgb ?: color
            }
            icon is AdaptiveIconDrawable &&
            doReshapeAdaptiveIcons &&
            icon.background != null &&
            icon.foreground != null -> {
                val (i, b, c) = reshapeAdaptiveIcon(icon)
                icon = i
                background = b ?: background
                color = c
            }
            else -> {
                val palette = Palette.from(icon.toBitmap(64, 64)).generate()
                color = palette.getDominantColor(0)
                if (color.red == color.blue && color.blue == color.green && color.green > 0xd0) {
                    color = 0
                }
            }
        }

        return icon to ExtraIconData(
            background, color
        )
    }

    private inline fun makeDrawable(color: Int) =
        if (doMonochromeIcons) ColorDrawable(color)
        else FastColorDrawable(color)

    private fun ensureNotPlainWhite(
        color: Int,
        icon: AdaptiveIconDrawable
    ): Int {
        if (color == 0xffffffff.toInt()) {
            val c = Palette.from(icon.foreground.toBitmap(24, 24)).generate()
                .getDominantColor(color)
            ColorUtils.colorToLAB(c, tmpLab)
            tmpLab[0] = (tmpLab[0] * 1.5).coerceAtLeast(70.0)
            return ColorUtils.LABToColor(tmpLab[0], tmpLab[1], tmpLab[2])
        }
        return color
    }

    /**
     * @return (icon, expandable background, color)
     */
    private fun reshapeAdaptiveIcon(icon: AdaptiveIconDrawable): Triple<Drawable, Drawable?, Int> {
        var color = 0
        val b = icon.background
        val isForegroundDangerous = run {
            val fg = icon.foreground.toBitmap(24, 24)
            val width = fg.width
            val height = fg.height
            val canvas = Canvas(fg)
            canvas.drawRect(4f, 4f, width - 4f, height - 4f, Paint().apply {
                xfermode = PorterDuff.Mode.CLEAR.toXfermode()
            })
            val pixels = IntArray(width * height)
            fg.getPixels(pixels, 0, width, 0, 0, width, height)
            for (pixel in pixels) {
                if (pixel.alpha != 0) {
                    return@run true
                }
            }
            false
        }
        val (foreground, background) = when (b) {
            is ColorDrawable -> {
                color = ensureNotPlainWhite(b.color, icon)
                (if (isForegroundDangerous) icon else scale(icon.foreground)) to makeDrawable(color)
            }
            is ShapeDrawable -> {
                color = ensureNotPlainWhite(b.paint.color, icon)
                (if (isForegroundDangerous) icon else scale(icon.foreground)) to makeDrawable(color)
            }
            is GradientDrawable -> {
                color = b.color?.defaultColor ?: Palette.from(b.toBitmap(8, 8)).generate().getDominantColor(0)
                (if (isForegroundDangerous) icon else scale(icon.foreground)) to b
            }
            else -> if (b != null) {
                val bitmap = b.toBitmap(24, 24)
                val px = b.toBitmap(1, 1).getPixel(0, 0)
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                var isOneColor = true
                ColorUtils.colorToLAB(px, tmpLab)
                var minL = tmpLab[0]
                var maxL = tmpLab[0]
                var minA = tmpLab[1]
                var maxA = tmpLab[1]
                var minB = tmpLab[2]
                var maxB = tmpLab[2]
                for (pixel in pixels) {
                    if (pixel != px) {
                        ColorUtils.colorToLAB(pixel, tmpLab)
                        val (l, a, b) = tmpLab
                        when {
                            l < minL -> minL = l
                            l > maxL -> maxL = l
                        }
                        when {
                            a < minA -> minA = a
                            a > maxA -> maxA = a
                        }
                        when {
                            b < minB -> minB = b
                            b > maxB -> maxB = b
                        }
                        isOneColor = false
                    }
                }
                val lt = 7f
                val at = 5f
                val bt = 5f
                if (isOneColor) {
                    color = ensureNotPlainWhite(px, icon)
                    (if (isForegroundDangerous) icon else scale(icon.foreground)) to makeDrawable(color)
                } else if (maxL - minL <= lt && maxA - minA <= at && maxB - minB <= bt) {
                    color = px
                    (if (isForegroundDangerous) icon else scale(icon.foreground)) to b
                } else {
                    color = Palette.from(bitmap).generate().getDominantColor(0)
                    icon to null
                }
            } else icon to null
        }

        return Triple(foreground, background, color)
    }

    private fun putInMap(app: App) {
        val list = byName[app.packageName]
        if (list == null) {
            byName[app.packageName] = arrayListOf(app)
            return
        }
        val thisAppI = list.indexOfFirst {
            it.name == app.name && it.userHandle.hashCode() == app.userHandle.hashCode()
        }
        if (thisAppI == -1) {
            list.add(app)
            return
        }
        list[thisAppI] = app
    }

    override fun finalize(context: Context) {
        list.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }
    }

    private val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
    }
    private val maskp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    override fun themeIcon(
        icon: Drawable,
        iconConfig: IconConfig,
        iconPackInfo: IconTheming.IconPackInfo,
        context: Context
    ): Drawable {
        return try {
            var orig = Bitmap.createBitmap(
                icon.intrinsicWidth,
                icon.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            icon.draw(Canvas(orig))
            val scaledBitmap =
                Bitmap.createBitmap(iconConfig.size, iconConfig.size, Bitmap.Config.ARGB_8888)
            Canvas(scaledBitmap).run {
                if (iconPackInfo.back != null) {
                    val b = iconPackInfo.back!!
                    drawBitmap(
                        b,
                        Rect(0, 0, b.width, b.height),
                        Rect(0, 0, iconConfig.size, iconConfig.size),
                        p
                    )
                }
                val scaledOrig =
                    Bitmap.createBitmap(iconConfig.size, iconConfig.size, Bitmap.Config.ARGB_8888)
                Canvas(scaledOrig).run {
                    val s = (iconConfig.size * iconPackInfo.scaleFactor).toInt()
                    orig = Bitmap.createScaledBitmap(orig, s, s, true)
                    drawBitmap(
                        orig,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        p
                    )
                    if (iconPackInfo.mask != null) {
                        val b = iconPackInfo.mask!!
                        drawBitmap(
                            b,
                            Rect(0, 0, b.width, b.height),
                            Rect(0, 0, iconConfig.size, iconConfig.size),
                            maskp
                        )
                    }
                }
                drawBitmap(
                    Bitmap.createScaledBitmap(scaledOrig, iconConfig.size, iconConfig.size, true),
                    0f,
                    0f,
                    p
                )
                if (iconPackInfo.front != null) {
                    val b = iconPackInfo.front!!
                    drawBitmap(
                        b,
                        Rect(0, 0, b.width, b.height),
                        Rect(0, 0, iconConfig.size, iconConfig.size),
                        p
                    )
                }
                scaledOrig.recycle()
            }
            BitmapDrawable(context.resources, scaledBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            icon
        }
    }

    companion object {

        fun Drawable.convertToGrayscale() {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        }

        private fun scale(fg: Drawable): Drawable {
            return InsetDrawable(
                fg,
                -1 / 3f
            )
        }
    }

    class ExtraIconData(
        var background: Drawable?,
        var color: Int,
    )
}