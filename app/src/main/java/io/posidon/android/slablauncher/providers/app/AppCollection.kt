package io.posidon.android.slablauncher.providers.app

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.*
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
import io.posidon.android.slablauncher.util.storage.Settings
import posidon.android.conveniencelib.clone
import posidon.android.conveniencelib.drawable.MaskedDrawable
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
            extraIconData.tile.makeGrayscale()
            extraIconData
        } else extra.extraIconData

        val icon = if (!extra.isUserRunning) icon.copy {
            it.makeGrayscale()
        } else icon

        val app = App(
            packageName,
            name,
            profile,
            label,
            icon,
            extraIconData.dependentUse { it.tile },
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
        val image: LayerDrawable
        val maskable: LayerDrawable

        when {
            expandableBackground != null -> {
                val bitmap = expandableBackground.toBitmap(8, 8)
                val palette = Palette.from(bitmap).generate()
                val d = palette.dominantSwatch
                color = d?.rgb ?: color
                if (expandableBackground !is BitmapDrawable || expandableBackground.bitmap != bitmap) bitmap.recycle()
                image = LayerDrawable(arrayOf(
                    expandableBackground,
                    icon
                )).apply {
                    val i = (intrinsicWidth / 4f).toInt()
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                }
                val bg = (expandableBackground.clone() ?: expandableBackground).mutate()
                val fg = (icon.clone() ?: icon).mutate()
                maskable = LayerDrawable(arrayOf(
                    bg,
                    fg
                )).apply {
                    val i = (intrinsicWidth / 8f).toInt()
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
            }
            icon is AdaptiveIconDrawable &&
            icon.background != null -> {
                val iconForeground: Drawable? = icon.foreground
                val background: Drawable
                when (val b = icon.background) {
                    is ColorDrawable -> {
                        color = ensureNotPlainWhite(b.color, icon)
                        background = FastColorDrawable(b.color)
                    }
                    is ShapeDrawable -> {
                        color = ensureNotPlainWhite(b.paint.color, icon)
                        background = FastColorDrawable(b.paint.color)
                    }
                    is GradientDrawable -> {
                        val bitmap = b.toBitmap(8, 8)
                        color = b.color?.defaultColor ?: Palette.from(bitmap).generate().getDominantColor(0)
                        if (b !is BitmapDrawable || b.bitmap != bitmap) bitmap.recycle()
                        background = icon.background
                    }
                    else -> {
                        val bitmap = b.toBitmap(24, 24)
                        color = Palette.from(bitmap).generate().getDominantColor(0)
                        if (b !is BitmapDrawable || b.bitmap != bitmap) bitmap.recycle()
                        background = icon.background
                    }
                }
                image = LayerDrawable(arrayOf(
                    background,
                    iconForeground
                ))
                val bg = (background.clone() ?: background).mutate()
                val fg = (iconForeground?.clone() ?: iconForeground)?.mutate()
                maskable = LayerDrawable(arrayOf(
                    bg,
                    fg
                )).apply {
                    val i = -(intrinsicWidth / 6f).toInt()
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
            }
            else -> {
                val bitmap = icon.toBitmap(32, 32)
                val palette = Palette.from(bitmap).generate()
                color = palette.getDominantColor(0)
                if (color.red == color.blue && color.blue == color.green && color.green > 0xd0) {
                    color = 0
                }
                if (icon !is BitmapDrawable || icon.bitmap != bitmap) bitmap.recycle()
                image = LayerDrawable(arrayOf(
                    FastColorDrawable(color),
                    icon
                )).apply {
                    val i = (intrinsicWidth / 4f).toInt()
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                }
                val bg = FastColorDrawable(color)
                val fg = (icon.clone() ?: icon).mutate()
                maskable = LayerDrawable(arrayOf(
                    bg,
                    fg
                )).apply {
                    val i = (intrinsicWidth / 8f).toInt()
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
            }
        }
        return MaskedDrawable(
            maskable,
            IconTheming.getSystemAdaptiveIconPath(maskable.intrinsicWidth, maskable.intrinsicHeight),
        ) to ExtraIconData(color, image)
    }

    private fun ensureNotPlainWhite(
        color: Int,
        icon: AdaptiveIconDrawable
    ): Int {
        if (color == 0xffffffff.toInt()) {
            val fg = icon.foreground
            val bitmap = fg.toBitmap(24, 24)
            val c = Palette.from(bitmap).generate()
                .getDominantColor(color)
            if (fg !is BitmapDrawable || fg.bitmap != bitmap) bitmap.recycle()
            ColorUtils.colorToLAB(c, tmpLab)
            tmpLab[0] = (tmpLab[0] * 1.5).coerceAtLeast(70.0)
            return ColorUtils.LABToColor(tmpLab[0], tmpLab[1], tmpLab[2])
        }
        return color
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

        inline fun Drawable.makeGrayscale(): Drawable {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
            return this
        }
    }

    class ExtraIconData(
        var color: Int,
        var tile: LayerDrawable,
    )
}