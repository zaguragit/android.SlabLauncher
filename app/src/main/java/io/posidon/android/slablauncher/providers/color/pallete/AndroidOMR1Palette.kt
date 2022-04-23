package io.posidon.android.slablauncher.providers.color.pallete

import android.app.WallpaperColors
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
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

        lab.run {
            ColorUtils.colorToLAB(neutralColor, this)
            set(1, get(1).coerceAtMost(32.0))
            set(2, get(2).coerceAtLeast(-50.0))
            set(0, (get(0) - (get(1) - 2.0)
                .coerceAtLeast(0.0) * 0.8)
                .coerceAtMost(20.0))
            neutralDark = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(1, get(1).coerceAtMost(18.0))
            set(2, get(2).coerceAtLeast(-32.0))
            set(0, (get(0) - 16.0).coerceAtMost(10.0))
            neutralVeryDark = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        lab.run {
            ColorUtils.colorToLAB(neutralColor, this)
            set(0, get(0).coerceAtLeast(80.0))
            neutralLight = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(0, get(0) + 15.0)
            neutralVeryLight = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        val mediumNeutral = ColorUtils.blendARGB(
            neutralDark,
            neutralLight,
            0.5f
        )
        lab.run {
            ColorUtils.colorToLAB(mediumNeutral, this)
            set(0, get(0).coerceAtLeast(35.0).coerceAtMost(65.0))
            neutralMedium = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

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