package io.posidon.android.slablauncher.providers.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.os.UserHandle
import androidx.core.graphics.*
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import io.posidon.android.launcherutils.appLoading.AppLoader
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.util.drawable.FastBitmapDrawable
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
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

    private val tmpHSL = FloatArray(3)

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
            extraIconData.color = run {
                val a = extraIconData.color
                ColorUtils.colorToHSL(a, tmpHSL)
                tmpHSL[1] = 0f
                ColorUtils.HSLToColor(tmpHSL)
            }
            val b = extraIconData.background
            if (b is FastColorDrawable) {
                extraIconData.background = FastColorDrawable(extraIconData.color)
            } else b?.convertToGrayscale()
        }

        val app = createApp(
            packageName,
            name,
            profile,
            label,
            icon,
            extraIconData,
            settings
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
            settings.doReshapeAdaptiveIcons &&
            icon.background != null &&
            icon.foreground != null -> {
                val (i, b, c) = reshapeAdaptiveIcon(icon)
                icon = i
                background = b ?: background
                color = c
            }
            else -> {
                val palette = Palette.from(icon.toBitmap(128, 128)).generate()
                color = palette.getDominantColor(0)
                if (color.red == color.blue && color.blue == color.green && color.green > 0xd0) {
                    color = 0
                }
            }
        }

        return icon.let {
            if (it is BitmapDrawable) FastBitmapDrawable(it.bitmap)
            else it
        } to ExtraIconData(
            background, color
        )
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

    companion object {

        fun Drawable.convertToGrayscale() {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        }

        fun createApp(
            packageName: String,
            name: String,
            profile: UserHandle,
            label: String,
            icon: Drawable,
            extra: ExtraIconData,
            settings: Settings,
        ): App {

            return App(
                packageName,
                name,
                profile,
                label,
                icon,
                extra.background,
                extra.color
            )
        }

        private fun scale(fg: Drawable): Drawable {
            return InsetDrawable(
                fg,
                -1 / 3f
            )
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
                    color = b.color
                    if (color == 0xffffffff.toInt()) {
                        color = run {
                            val c = Palette.from(icon.foreground.toBitmap(24, 24)).generate().getDominantColor(color)
                            val lab = DoubleArray(3)
                            ColorUtils.colorToLAB(c, lab)
                            lab[0] = (lab[0] * 1.5).coerceAtLeast(70.0)
                            ColorUtils.LABToColor(lab[0], lab[1], lab[2])
                        }
                    }
                    (if (isForegroundDangerous) icon else scale(icon.foreground)) to FastColorDrawable(color)
                }
                is ShapeDrawable -> {
                    color = b.paint.color
                    if (color == 0xffffffff.toInt()) {
                        color = run {
                            val c = Palette.from(icon.foreground.toBitmap(24, 24)).generate().getDominantColor(color)
                            val lab = DoubleArray(3)
                            ColorUtils.colorToLAB(c, lab)
                            lab[0] = (lab[0] * 1.5).coerceAtLeast(70.0)
                            ColorUtils.LABToColor(lab[0], lab[1], lab[2])
                        }
                    }
                    (if (isForegroundDangerous) icon else scale(icon.foreground)) to FastColorDrawable(color)
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
                    val lab = DoubleArray(3)
                    ColorUtils.colorToLAB(px, lab)
                    var minL = lab[0]
                    var maxL = lab[0]
                    var minA = lab[1]
                    var maxA = lab[1]
                    var minB = lab[2]
                    var maxB = lab[2]
                    for (pixel in pixels) {
                        if (pixel != px) {
                            ColorUtils.colorToLAB(pixel, lab)
                            val (l, a, b) = lab
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
                        color = px
                        if (color == 0xffffffff.toInt()) {
                            color = run {
                                val c = Palette.from(icon.foreground.toBitmap(24, 24)).generate().getDominantColor(color)
                                val lab = DoubleArray(3)
                                ColorUtils.colorToLAB(c, lab)
                                lab[0] = (lab[0] * 1.5).coerceAtLeast(70.0)
                                ColorUtils.LABToColor(lab[0], lab[1], lab[2])
                            }
                        }
                        (if (isForegroundDangerous) icon else scale(icon.foreground)) to FastColorDrawable(color)
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
    }

    class ExtraIconData(
        var background: Drawable?,
        var color: Int,
    )
}