package io.posidon.android.slablauncher.providers.app

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.os.UserHandle
import androidx.core.graphics.*
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import io.posidon.android.computable.Computable
import io.posidon.android.computable.copy
import io.posidon.android.computable.dependentUse
import io.posidon.android.launcherutils.IconTheming
import io.posidon.android.launcherutils.appLoading.AppLoader
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeIcons
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeTileBackground
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.slablauncher.ui.view.tile.TileContentMover
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AppCollection(
    appCount: Int,
    val settings: Settings,
) : AppLoader.AppCollection<AppCollection.ExtraIconData> {
    val list = ArrayList<App>(appCount)
    val byName = HashMap<String, MutableList<App>>()

    inline operator fun get(i: Int) = list[i]
    inline val size get() = list.size

    private val tmpLab = DoubleArray(3)

    private val doMonochromeIcons = settings.doMonochromeIcons
    private val doMonochromeTileBackground = settings.doMonochromeTileBackground

    override fun addApp(
        context: Context,
        packageName: String,
        name: String,
        profile: UserHandle,
        label: String,
        icon: Computable<Drawable>,
        extra: AppLoader.ExtraAppInfo<ExtraIconData>,
    ) {
        val extraIconData = if (!extra.isUserRunning) extra.extraIconData.copy { extraIconData ->
            extraIconData.color = run {
                val a = (extraIconData.color.luminance * 255).toInt()
                Color.rgb(a, a, a)
            }
            val b = extraIconData.background
            if (b is FastColorDrawable) {
                extraIconData.background = makeDrawable(extraIconData.color)
            } else b?.convertToGrayscale()
            extraIconData
        } else if (doMonochromeTileBackground) extra.extraIconData.copy { extraIconData ->
            extraIconData.color = extraIconData.color and 0xffffff
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val b = extraIconData.background
                if (b is FastColorDrawable) {
                    val a = (extraIconData.color.luminance * 255).toInt()
                    extraIconData.background = makeDrawable(Color.rgb(a, a, a))
                } else b?.convertToGrayscale()
            }
            extraIconData
        } else extra.extraIconData

        val maxIconSize = TileContentMover.calculateBigIconSize(context)
        val scaledIcon = icon.copy {
            if (it is BitmapDrawable) {
                val oldBitmap = it.bitmap
                if (oldBitmap.width > maxIconSize || oldBitmap.height > maxIconSize) {
                    return@copy BitmapDrawable(Bitmap.createScaledBitmap(
                        oldBitmap,
                        size,
                        size,
                        true
                    ))
                }
            }
            it
        }
        val icon = if (!extra.isUserRunning) scaledIcon.copy {
            it.apply {
                convertToGrayscale()
                if (doMonochromeIcons) {
                    alpha = 128
                }
            }
        } else if (doMonochromeIcons) scaledIcon.copy {
            it.apply { convertToGrayscale() }
        } else scaledIcon

        val app = App(
            packageName,
            name,
            profile,
            label,
            icon,
            extraIconData.dependentUse { it.background },
            extraIconData.dependentUse { it.color },
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
                val bitmap = background.toBitmap(8, 8)
                val palette = Palette.from(bitmap).generate()
                val d = palette.dominantSwatch
                color = d?.rgb ?: color
                bitmap.recycle()
            }
            icon is AdaptiveIconDrawable &&
            icon.background != null &&
            icon.foreground != null -> {
                val (i, b, c) = reshapeAdaptiveIcon(icon)
                icon = i
                background = b ?: background
                color = c
            }
            else -> {
                val bitmap = icon.toBitmap(64, 64)
                val palette = Palette.from(bitmap).generate()
                color = palette.getDominantColor(0)
                if (color.red == color.blue && color.blue == color.green && color.green > 0xd0) {
                    color = 0
                }
                bitmap.recycle()
            }
        }

        return icon to ExtraIconData(
            background, color
        )
    }

    private inline fun makeDrawable(color: Int) =
        if (doMonochromeTileBackground) ColorDrawable(color)
        else FastColorDrawable(color)

    private fun ensureNotPlainWhite(
        color: Int,
        icon: AdaptiveIconDrawable
    ): Int {
        if (color == 0xffffffff.toInt()) {
            val bitmap = icon.foreground.toBitmap(24, 24)
            val c = Palette.from(bitmap).generate()
                .getDominantColor(color)
            bitmap.recycle()
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
        when (val b = icon.background) {
            is ColorDrawable -> {
                color = ensureNotPlainWhite(b.color, icon)
            }
            is ShapeDrawable -> {
                color = ensureNotPlainWhite(b.paint.color, icon)
            }
            is GradientDrawable -> {
                val bitmap = b.toBitmap(8, 8)
                color = b.color?.defaultColor ?: Palette.from(bitmap).generate().getDominantColor(0)
                bitmap.recycle()
            }
            else -> if (b != null) run {
                val bitmap = b.toBitmap(24, 24)
                color = Palette.from(bitmap).generate().getDominantColor(0)
            }
        }

        return Triple(scale(icon.foreground), icon.background, color)
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
        iconPackInfo: IconTheming.IconGenerationInfo,
        resources: Resources
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
                Bitmap.createBitmap(iconPackInfo.size, iconPackInfo.size, Bitmap.Config.ARGB_8888)
            Canvas(scaledBitmap).run {
                val uniformOptions = BitmapFactory.Options().apply {
                    inScaled = false
                }
                val back = iconPackInfo.getBackBitmap(uniformOptions)
                if (back != null) {
                    drawBitmap(
                        back,
                        Rect(0, 0, back.width, back.height),
                        Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                        p
                    )
                    back.recycle()
                }
                val scaledOrig =
                    Bitmap.createBitmap(iconPackInfo.size, iconPackInfo.size, Bitmap.Config.ARGB_8888)
                Canvas(scaledOrig).run {
                    val s = (iconPackInfo.size * iconPackInfo.scaleFactor).toInt()
                    val oldOrig = orig
                    orig = Bitmap.createScaledBitmap(orig, s, s, true)
                    oldOrig.recycle()
                    drawBitmap(
                        orig,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f,
                        p
                    )
                    val mask = iconPackInfo.getMaskBitmap(uniformOptions)
                    if (mask != null) {
                        drawBitmap(
                            mask,
                            Rect(0, 0, mask.width, mask.height),
                            Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                            maskp
                        )
                        mask.recycle()
                    }
                }
                drawBitmap(
                    Bitmap.createScaledBitmap(scaledOrig, iconPackInfo.size, iconPackInfo.size, true),
                    0f,
                    0f,
                    p
                )
                val front = iconPackInfo.getFrontBitmap(uniformOptions)
                if (front != null) {
                    drawBitmap(
                        front,
                        Rect(0, 0, front.width, front.height),
                        Rect(0, 0, iconPackInfo.size, iconPackInfo.size),
                        p
                    )
                    front.recycle()
                }
                orig.recycle()
                scaledOrig.recycle()
            }
            BitmapDrawable(resources, scaledBitmap)
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