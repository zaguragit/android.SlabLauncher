package io.posidon.android.slablauncher.providers.color.theme

import androidx.core.graphics.ColorUtils.*
import androidx.core.graphics.luminance
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.pallete.DefaultPalette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

private var colorThemeInstance: ColorTheme = DarkColorTheme(DefaultPalette)

interface ColorTheme {

    val accentColor: Int
    val secondaryAccentColor: Int

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
        override val secondaryAccentColor: Int
            get() = colorThemeInstance.secondaryAccentColor
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
            colorToLAB(color, tintLAB)
            colorToLAB(base, baseLAB)
            if (baseLAB[0] <= tintLAB[0]) {
                tintLAB[1] *= 1.5
                tintLAB[2] *= 1.5
            }
            return LABToColor(baseLAB[0], tintLAB[1], tintLAB[2]) and 0xffffff or (base and 0xff000000.toInt())
        }

        fun hueTintClosest(baseColor: Int, lightnessOf: Int, palette: Array<Int>): Int {
            val tmp = FloatArray(3)
            colorToHSL(baseColor, tmp)
            val h = tmp[0]
            val s = tmp[1]
            val l = tmp[2]
            val isDesaturated = s < 1f || l == 1f || l < .001f
            val (targetHue, targetSaturation) = run {
                palette.map { color ->
                    FloatArray(3).also { colorToHSL(color, it) }
                }.minByOrNull { (targetHue, targetSaturation, targetLightness) ->
                    val hd = if (isDesaturated) 0f else {
                        val rd = abs(h - targetHue)
                        min(360f - rd, rd) / 180f
                    }
                    val sd = abs(s - targetSaturation)
                    val ld = abs(l - targetLightness)
                    hd * 5 + hd * sd * sd * 2 + ld * ld
                }!!
            }
            val (hue, saturation) = run {
                val diff = run {
                    val a = targetHue - h
                    val b = -a.sign * (360 - abs(a))
                    if (abs(a) < abs(b)) a else b
                }
                val targetness = (1 - diff / 180 * s).coerceAtLeast(0f).coerceAtMost(1f)
                val rotation = targetness * diff
                val x = (h + rotation) % 360
                val hue = if (x < 0) 360 + x else x
                val saturation = min(s, targetSaturation)
                hue to saturation
            }

            tmp[0] = hue
            tmp[1] = saturation

            val lab = DoubleArray(3)
            colorToLAB(HSLToColor(tmp), lab)
            lab[0] = DoubleArray(3).also { colorToLAB(lightnessOf, it) }[0] * 0.9
            lab[0] += DoubleArray(3).also { colorToLAB(baseColor, it) }[0] * 0.1

            return LABToColor(lab[0], lab[1], lab[2])
        }
    }
}