package io.posidon.android.slablauncher.providers.color.theme

import android.content.Context
import androidx.core.graphics.ColorUtils
import io.posidon.android.slablauncher.providers.color.pallete.DefaultPalette
import kotlin.math.abs

private var colorThemeInstance: ColorTheme = DarkColorTheme(DefaultPalette)

interface ColorTheme {

    val accentColor: Int

    val uiBG: Int
    val uiTitle: Int
    val uiDescription: Int
    val uiHint: Int

    val cardBG: Int
    val cardTitle: Int
    val cardDescription: Int
    val cardHint: Int

    val buttonColor: Int

    val appCardBase: Int

    val searchBarBG: Int
    val searchBarFG: Int

    fun adjustColorForContrast(base: Int, tint: Int): Int

    fun tintAppDrawerItem(color: Int): Int

    fun textColorForBG(context: Context, background: Int): Int

    fun titleColorForBG(context: Context, background: Int): Int

    fun hintColorForBG(context: Context, background: Int): Int

    companion object : ColorTheme {

        fun updateColorTheme(colorTheme: ColorTheme) {
            colorThemeInstance = colorTheme
        }

        override val accentColor: Int
            get() = colorThemeInstance.accentColor
        override val uiBG: Int
            get() = colorThemeInstance.uiBG
        override val uiTitle: Int
            get() = colorThemeInstance.uiTitle
        override val uiDescription: Int
            get() = colorThemeInstance.uiDescription
        override val uiHint: Int
            get() = colorThemeInstance.uiHint
        override val cardBG: Int
            get() = colorThemeInstance.cardBG
        override val cardTitle: Int
            get() = colorThemeInstance.cardTitle
        override val cardDescription: Int
            get() = colorThemeInstance.cardDescription
        override val cardHint: Int
            get() = colorThemeInstance.cardHint
        override val buttonColor: Int
            get() = colorThemeInstance.buttonColor
        override val appCardBase: Int
            get() = colorThemeInstance.appCardBase
        override val searchBarBG: Int
            get() = colorThemeInstance.searchBarBG
        override val searchBarFG: Int
            get() = colorThemeInstance.searchBarFG

        override fun adjustColorForContrast(base: Int, tint: Int): Int =
            colorThemeInstance.adjustColorForContrast(base, tint)

        override fun tintAppDrawerItem(color: Int): Int =
            colorThemeInstance.tintAppDrawerItem(color)

        override fun textColorForBG(context: Context, background: Int): Int =
            colorThemeInstance.textColorForBG(context, background)

        override fun titleColorForBG(context: Context, background: Int): Int =
            colorThemeInstance.titleColorForBG(context, background)

        override fun hintColorForBG(context: Context, background: Int): Int =
            colorThemeInstance.hintColorForBG(context, background)


        fun tintWithColor(base: Int, color: Int): Int {
            val tintHSL = FloatArray(3)
            val baseHSL = FloatArray(3)
            ColorUtils.colorToHSL(color, tintHSL)
            ColorUtils.colorToHSL(base, baseHSL)
            tintHSL[2] = baseHSL[2]
            return ColorUtils.HSLToColor(tintHSL) and 0xffffff or (base and 0xff000000.toInt())
        }

        fun splitTint(base: Int, color: Int, accent: Int): Int {
            val baseLab = DoubleArray(3)
            ColorUtils.colorToLAB(base, baseLab)
            val colorLab = DoubleArray(3)
            ColorUtils.colorToLAB(color, colorLab)
            val accentLab = DoubleArray(3)
            ColorUtils.colorToLAB(accent, accentLab)
            val r = splitTint(baseLab, colorLab, accentLab)
            return ColorUtils.LABToColor(r[0], r[1], r[2])
        }

        fun splitTint(base: DoubleArray, color: DoubleArray, accent: DoubleArray): DoubleArray {
            val az = (accent[1] + 128) / 256
            val bz = (accent[2] + 128) / 256

            val ab = (base[1] + 128) / 256
            val bb = (base[2] + 128) / 256

            val ac = (color[1] + 128) / 256
            val bc = (color[2] + 128) / 256

            val azz = ((1 - abs(az - ac)) * abs(ab - ac)) * 2
            val bzz = ((1 - abs(bz - bc)) * abs(bb - bc)) * 2

            base[1] = accent[1] * azz + base[1] * (1 - azz)
            base[2] = accent[2] * bzz + base[2] * (1 - bzz)

            val oldL = base[0]
            ColorUtils.blendLAB(base, color, 0.4, base)
            base[0] = oldL

            return base
        }
    }
}