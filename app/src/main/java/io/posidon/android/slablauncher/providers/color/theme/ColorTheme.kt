package io.posidon.android.slablauncher.providers.color.theme

import androidx.core.graphics.ColorUtils
import androidx.core.graphics.luminance
import io.posidon.android.slablauncher.providers.color.ColorThemeOptions
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.pallete.DefaultPalette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

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

    val separator: Int

    val buttonColor: Int
    val buttonColorCallToAction: Int

    val searchBarBG: Int
    val searchBarFG: Int

    fun adjustColorForContrast(base: Int, tint: Int): Int

    fun tileColor(iconBackgroundColor: Int): Int

    companion object : ColorTheme {

        fun create(palette: ColorPalette, isDark: Boolean): ColorTheme {
            return if (isDark) DarkColorTheme(palette)
            else LightColorTheme(palette)
        }

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
        override val separator: Int
            get() = colorThemeInstance.separator
        override val buttonColor: Int
            get() = colorThemeInstance.buttonColor
        override val buttonColorCallToAction: Int
            get() = colorThemeInstance.buttonColorCallToAction
        override val searchBarBG: Int
            get() = colorThemeInstance.searchBarBG
        override val searchBarFG: Int
            get() = colorThemeInstance.searchBarFG

        override fun adjustColorForContrast(base: Int, tint: Int): Int =
            colorThemeInstance.adjustColorForContrast(base, tint)

        override fun tileColor(iconBackgroundColor: Int): Int =
            colorThemeInstance.tileColor(iconBackgroundColor)


        fun tintPopup(color: Int): Int {
            return tintWithColor(cardBG, tileColor(color))
        }

        fun titleColorForBG(background: Int): Int {
            return (if (background.luminance > .6f) 0 else 0xffffff) or 0xff000000.toInt()
        }

        fun textColorForBG(background: Int): Int {
            return (if (background.luminance > .6f) 0 else 0xffffff) or 0xd2000000.toInt()
        }

        fun hintColorForBG(background: Int): Int {
            return (if (background.luminance > .6f) 0 else 0xffffff) or 0x55000000
        }


        fun tintWithColor(base: Int, color: Int): Int {
            val tintLAB = DoubleArray(3)
            val baseLAB = DoubleArray(3)
            ColorUtils.colorToLAB(color, tintLAB)
            ColorUtils.colorToLAB(base, baseLAB)
            if (baseLAB[0] <= tintLAB[0]) {
                tintLAB[1] *= 1.5
                tintLAB[2] *= 1.5
            }
            return ColorUtils.LABToColor(baseLAB[0], tintLAB[1], tintLAB[2]) and 0xffffff or (base and 0xff000000.toInt())
        }

        fun hueTintClosest(baseColor: Int, palette: Array<Int>): Int {
            val tmp = FloatArray(3)
            ColorUtils.colorToHSL(baseColor, tmp)
            val h = tmp[0]
            val s = tmp[1]
            val l = tmp[2]
            val isDesaturated = s < 1f || l == 1f || l < .001f
            val (targetHue, targetSaturation, targetLightness) = run {
                palette.map { color ->
                    FloatArray(3).also { ColorUtils.colorToHSL(color, it) }
                }.minByOrNull { (targetHue, targetSaturation, targetLightness) ->
                    val hd = if (isDesaturated) 0f else {
                        val rd = abs(h - targetHue)
                        min(360f - rd, rd)
                    }
                    val sd = abs(s - targetSaturation)
                    val ld = abs(l - targetLightness)
                    hd * hd * 4 + sd * sd * 1 + ld * ld * 2
                }!!
            }
            val (hue, saturation) = run {
                val targetness = (.9f - s * s * .38f).coerceAtLeast(0f)
                val diff = run {
                    val a = targetHue - h
                    val b = 360f - abs(a)
                    if (abs(a) < b) a else b * a.sign
                }
                val rotation = targetness * diff
                val x = (h + rotation) % 360
                val hue = if (x < 0) 360 + x else x
                val sameness = (1 - abs(diff) / (360f / 2f))
                val saturation = s * sameness
                hue to saturation
            }

            tmp[0] = hue
            val ss = targetSaturation + .5f
            tmp[1] = saturation * (ss * ss).coerceAtMost(1.25f)

            val iconBackground = DoubleArray(3)
            ColorUtils.colorToLAB(baseColor, iconBackground)

            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(ColorUtils.HSLToColor(tmp), lab)
            lab[0] = (iconBackground[0] + 10).coerceAtLeast(20.0)

            return ColorUtils.LABToColor(lab[0], lab[1], lab[2])
        }

        fun labClosestVibrant(baseColor: Int, palette: Array<Int>): Int {
            val tmp = DoubleArray(3)
            ColorUtils.colorToLAB(baseColor, tmp)
            val l = tmp[0]
            val a = tmp[1]
            val b = tmp[2]
            val (tl, ta, tb) = run {
                palette.map { color ->
                    DoubleArray(3).also { ColorUtils.colorToLAB(color, it) }
                }.minByOrNull { (tl, ta, tb) ->
                    val ld = abs(l - tl) / 100
                    val ad = abs(a - ta) / 128.0 / 2.0
                    val bd = abs(b - tb) / 128.0 / 2.0
                    val s = max((abs(ta) / 128.0), (abs(tb) / 128.0))
                    val si = (1 - s)
                    ad * ad + bd * bd + ld * ld * 2 + si * si * 2
                }!!
            }

            return ColorUtils.LABToColor(tl, ta, tb)
        }

        fun darkestVisibleOn(backgroundColor: Int, color: Int): Int {
            val l = backgroundColor.luminance
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(color, lab)
            lab[0] = if (l < 0.03) 100.0
            else 20.0
            return ColorUtils.LABToColor(lab[0], lab[1], lab[2])
        }
    }
}