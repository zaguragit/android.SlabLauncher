package io.posidon.android.slablauncher.providers.color.pallete

import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import posidon.android.conveniencelib.Colors
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

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
        val lab = DoubleArray(3)

        val darkNeutralSwatch = wallpaper.darkVibrantSwatch ?: wallpaper.darkMutedSwatch ?: wallpaper.dominantSwatch
        val darkNeutral = darkNeutralSwatch?.rgb
        lab.run {
            ColorUtils.colorToLAB(darkNeutral ?: 0, this)
            set(1, get(1).coerceAtMost(32.0))
            set(0, (get(0) - (get(1) - 2.0).coerceAtLeast(0.0) * 0.8).coerceAtMost(20.0))
            neutralDark = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(1, get(1).coerceAtMost(18.0))
            set(0, (get(0) - 16.0).coerceAtMost(10.0))
            neutralVeryDark = ColorUtils.LABToColor(this[0], this[1], this[2])
        }


        val lightNeutralSwatch = wallpaper.lightVibrantSwatch ?: wallpaper.lightMutedSwatch ?: wallpaper.dominantSwatch
        val lightNeutral = lightNeutralSwatch?.rgb
        lab.run {
            ColorUtils.colorToLAB(lightNeutral ?: 0, this)
            set(0, get(0).coerceAtLeast(80.0))
            neutralLight = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(0, get(0) + 15.0)
            neutralVeryLight = ColorUtils.LABToColor(this[0], this[1], this[2])
        }


        val mediumNeutral = ColorUtils.blendARGB(
            darkNeutral ?: 0xffffffff.toInt(),
            lightNeutral ?: 0xff000000.toInt(),
            0.5f
        )
        lab.run {
            ColorUtils.colorToLAB(mediumNeutral, this)
            set(0, get(0).coerceAtLeast(35.0).coerceAtMost(65.0))
            neutralMedium = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        val primarySwatch = wallpaper.vibrantSwatch
            ?: wallpaper.darkVibrantSwatch
            ?: wallpaper.lightVibrantSwatch
            ?: wallpaper.dominantSwatch

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