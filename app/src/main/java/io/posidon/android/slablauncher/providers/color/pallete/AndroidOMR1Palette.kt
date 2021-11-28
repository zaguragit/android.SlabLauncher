package io.posidon.android.slablauncher.providers.color.pallete

import android.app.WallpaperColors
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils

@RequiresApi(Build.VERSION_CODES.O_MR1)
class AndroidOMR1Palette(
    context: Context,
    colors: WallpaperColors
) : ColorPalette {

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

        primary = colorList.last()
        secondary = if (colorList.size <= 2) primary else colorList[1]

        lab.run {
            ColorUtils.colorToLAB(neutralColor, this)
            set(0, get(0).coerceAtMost(20.0))
            neutralDark = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(0, get(0) - 10.0)
            neutralVeryDark = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        lab.run {
            ColorUtils.colorToLAB(neutralColor, this)
            set(0, get(0).coerceAtLeast(30.0).coerceAtMost(60.0))
            neutralMedium = ColorUtils.LABToColor(this[0], this[1], this[2])
        }

        lab.run {
            ColorUtils.colorToLAB(neutralColor, this)
            set(0, get(0).coerceAtLeast(80.0))
            neutralLight = ColorUtils.LABToColor(this[0], this[1], this[2])
            set(0, get(0) + 15.0)
            neutralVeryLight = ColorUtils.LABToColor(this[0], this[1], this[2])
        }
    }
}