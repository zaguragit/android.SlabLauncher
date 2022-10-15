package io.posidon.android.slablauncher.providers.color.pallete

import android.app.WallpaperColors
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import dev.kdrag0n.colorkt.rgb.LinearSrgb.Companion.toLinear
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.ucs.lab.Oklab.Companion.toOklab
import dev.kdrag0n.colorkt.ucs.lch.Oklch.Companion.toOklch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

@RequiresApi(Build.VERSION_CODES.O_MR1)
class AndroidOMR1Palette(
    context: Context,
    colors: WallpaperColors
) : ColorPalette {

    override val estimatedWallColor = colors.primaryColor.toArgb()

    override val neutralVeryDark: Int
    override val neutralDark: Int
    override val neutralMedium: Int
    override val neutralLight: Int
    override val neutralVeryLight: Int

    override val primary: Int
    override val secondary: Int

    init {
        val colorList = listOfNotNull(colors.primaryColor.toArgb(), colors.secondaryColor?.toArgb(), colors.tertiaryColor?.toArgb()).toMutableList()
        val lab = DoubleArray(3)
        colorList.sortBy {
            ColorUtils.colorToLAB(it, lab)
            lab[1] * lab[1] + lab[2] * lab[2]
        }
        val neutralColor = colorList.first()

        val darkLab = Srgb(neutralColor).toLinear().toOklab()
            .toOklch()
            .let { it.copy(chroma = it.chroma.coerceAtMost(0.03)) }
            .toOklab()
        neutralDark = darkLab.copy(
            L = darkLab.L.coerceAtLeast(0.18).coerceAtMost(0.24),
        ).toLinearSrgb().toSrgb().toRgb8() or 0xff000000.toInt()
        neutralVeryDark = darkLab.copy(
            L = darkLab.L.coerceAtMost(0.1),
        ).toLinearSrgb().toSrgb().toRgb8() or 0xff000000.toInt()

        val lightLab = Srgb(neutralColor).toLinear().toOklab()
            .toOklch()
            .let { it.copy(chroma = it.chroma.coerceAtMost(0.07)) }
            .toOklab()
        neutralLight = lightLab.copy(L = lightLab.L.coerceAtLeast(0.8))
            .toLinearSrgb()
            .toSrgb()
            .toRgb8() or 0xff000000.toInt()
        neutralVeryLight = lightLab.copy(L = (lightLab.L.coerceAtLeast(0.8) + 0.15))
            .toLinearSrgb()
            .toSrgb()
            .toRgb8() or 0xff000000.toInt()

        val mediumNeutral = ColorUtils.blendARGB(
            neutralDark,
            neutralLight,
            0.5f
        )
        val mediumLab = Srgb(mediumNeutral).toLinear().toOklab()
            .toOklch()
            .let { it.copy(chroma = it.chroma.coerceAtMost(0.045)) }
            .toOklab()
        neutralMedium = mediumLab
            .copy(L = mediumLab.L.coerceAtLeast(0.25).coerceAtMost(0.42))
            .toLinearSrgb()
            .toSrgb()
            .toRgb8() or 0xff000000.toInt()

        val primaryBase = colorList.last()
        val secondaryBase = if (colorList.size <= 2) primaryBase else colorList[1]

        val lab2 = DoubleArray(3)
        ColorUtils.colorToLAB(neutralDark, lab)
        ColorUtils.colorToLAB(neutralLight, lab2)
        ColorUtils.blendLAB(lab, lab2, 0.5, lab2)

        lab.run {
            ColorUtils.colorToLAB(primaryBase, this)
            set(0, get(0).coerceAtLeast(72.0))
            this[1] += abs(lab2[1] / 128.0).pow(1.0/3.0) * lab2[1].sign * 128.0 * 0.5
            this[2] += abs(lab2[2] / 128.0).pow(1.0/3.0) * lab2[2].sign * 128.0 * 0.5
            this[1] *= 1.5
            set(1, get(1).coerceAtMost(112.0))
            this[2] *= 1.5
            primary = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        lab.run {
            ColorUtils.colorToLAB(secondaryBase, this)
            set(0, get(0).coerceAtLeast(86.0))
            this[1] += abs(lab2[1] / 128.0).pow(1.0/3.0) * lab2[1].sign * 128.0 * 0.5
            this[2] += abs(lab2[2] / 128.0).pow(1.0/3.0) * lab2[2].sign * 128.0 * 0.5
            this[1] *= 2.0
            set(1, get(1).coerceAtMost(110.0))
            this[2] *= 2.0
            secondary = ColorUtils.LABToColor(this[0], this[1], this[2])
        }
    }
}