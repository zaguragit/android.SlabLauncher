package io.posidon.android.slablauncher.providers.color.pallete

import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import dev.kdrag0n.colorkt.rgb.LinearSrgb.Companion.toLinear
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.ucs.lab.Oklab.Companion.toOklab
import dev.kdrag0n.colorkt.ucs.lch.Oklch.Companion.toOklch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class BitmapBasedPalette(
    wallpaper: Palette,
) : ColorPalette {
    override val estimatedWallColor = wallpaper.getDominantColor(0xff000000.toInt())

    override val neutralVeryDark: Int
    override val neutralDark: Int
    override val neutralMedium: Int
    override val neutralLight: Int
    override val neutralVeryLight: Int

    override val primary: Int
    override val secondary: Int

    init {

        val darkNeutralSwatch = wallpaper.darkMutedSwatch ?: wallpaper.darkVibrantSwatch ?: wallpaper.dominantSwatch
        val darkNeutral = darkNeutralSwatch?.rgb
        val darkLab = Srgb(darkNeutral ?: 0).toLinear().toOklab()
            .toOklch()
            .let { it.copy(chroma = it.chroma.coerceAtMost(0.03)) }
            .toOklab()
        neutralDark = darkLab.copy(
            L = darkLab.L.coerceAtLeast(0.18).coerceAtMost(0.24),
        ).toLinearSrgb().toSrgb().toRgb8() or 0xff000000.toInt()
        neutralVeryDark = darkLab.copy(
            L = darkLab.L.coerceAtMost(0.1),
        ).toLinearSrgb().toSrgb().toRgb8() or 0xff000000.toInt()

        val lightNeutralSwatch = wallpaper.lightVibrantSwatch ?: wallpaper.lightMutedSwatch ?: wallpaper.dominantSwatch
        val lightNeutral = lightNeutralSwatch?.rgb
        val lightLab = Srgb(lightNeutral ?: 0).toLinear().toOklab()
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
            darkNeutral ?: 0xffffffff.toInt(),
            lightNeutral ?: 0xff000000.toInt(),
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

        val primarySwatch = wallpaper.vibrantSwatch
            ?: wallpaper.darkVibrantSwatch
            ?: wallpaper.lightVibrantSwatch
            ?: wallpaper.dominantSwatch

        val lab = DoubleArray(3)
        val lab2 = DoubleArray(3)

        ColorUtils.colorToLAB(neutralDark, lab)
        ColorUtils.colorToLAB(neutralLight, lab2)
        ColorUtils.blendLAB(lab, lab2, 0.5, lab2)

        lab.run {
            ColorUtils.colorToLAB(primarySwatch?.rgb ?: 0, this)
            set(0, get(0).coerceAtLeast(72.0))
            this[1] += abs(lab2[1] / 128.0).pow(1.0/3.0) * lab2[1].sign * 128.0 * 0.5
            this[2] += abs(lab2[2] / 128.0).pow(1.0/3.0) * lab2[2].sign * 128.0 * 0.5
            this[1] *= 1.5
            set(1, get(1).coerceAtMost(112.0))
            this[2] *= 1.5
            primary = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        val secondarySwatch = wallpaper.lightVibrantSwatch
            ?: wallpaper.vibrantSwatch
            ?: wallpaper.darkVibrantSwatch
            ?: wallpaper.dominantSwatch

        lab.run {
            ColorUtils.colorToLAB(secondarySwatch?.rgb ?: 0, this)
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