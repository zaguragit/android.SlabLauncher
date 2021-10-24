package io.posidon.android.slablauncher.providers.color.pallete

import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

class BitmapBasedPalette(wallpaper: Palette) : ColorPalette {

    override val neutralVeryDark: Int
    override val neutralDark: Int
    override val neutralMedium: Int
    override val neutralLight: Int
    override val neutralVeryLight: Int

    override val primary: Int
    override val secondary: Int

    init {
        val darkNeutralSwatch = wallpaper.darkMutedSwatch ?: wallpaper.mutedSwatch ?: wallpaper.dominantSwatch
        val darkNeutral = darkNeutralSwatch?.rgb
        val darkNeutralLab = DoubleArray(3)
        darkNeutralLab.run {
            ColorUtils.colorToLAB(darkNeutral ?: 0, this)
            set(0, get(0).coerceAtMost(20.0))
            neutralDark = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(0, (get(0) - 10.0).coerceAtMost(10.0))
            neutralVeryDark = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        val neutralSwatch = wallpaper.mutedSwatch ?: wallpaper.dominantSwatch
        neutralMedium = neutralSwatch?.rgb ?: 0

        val lightNeutralSwatch = wallpaper.lightMutedSwatch ?: wallpaper.mutedSwatch ?: wallpaper.dominantSwatch
        val lightNeutral = lightNeutralSwatch?.rgb
        val lightNeutralLab = DoubleArray(3)
        lightNeutralLab.run {
            ColorUtils.colorToLAB(lightNeutral ?: 0, this)
            set(0, get(0).coerceAtLeast(80.0))
            neutralLight = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(0, get(0) + 15.0)
            neutralVeryLight = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        val primarySwatch = wallpaper.vibrantSwatch ?: wallpaper.lightVibrantSwatch ?: wallpaper.dominantSwatch
        primary = primarySwatch?.rgb ?: 0

        val secondarySwatch = wallpaper.dominantSwatch
        secondary = secondarySwatch?.rgb ?: 0
    }
}