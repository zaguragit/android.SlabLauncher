package io.posidon.android.slablauncher.providers.color.theme

import android.content.Context
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.luminance
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme.Companion.hueTintClosest
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme.Companion.tintWithColor

class LightColorTheme(
    val palette: ColorPalette,
) : ColorTheme {

    override val accentColor = palette.primary

    override val uiBG = palette.neutralLight
    override val uiTitle = palette.neutralVeryDark
    override val uiDescription = palette.neutralDark
    override val uiHint = palette.neutralMedium

    override val cardBG = palette.neutralVeryLight
    override val cardTitle = palette.neutralVeryDark
    override val cardDescription = palette.neutralDark
    override val cardHint = palette.neutralMedium

    override val separator = palette.neutralVeryDark and 0xffffff or 0x33000000

    override val buttonColor = palette.neutralVeryLight
    override val buttonColorCallToAction = palette.secondary

    override val searchBarBG = palette.neutralVeryLight
    override val searchBarFG = palette.neutralDark

    override fun adjustColorForContrast(base: Int, tint: Int): Int {
        return if (base.luminance > .6f) {
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(tint, lab)
            lab[0] = lab[0].coerceAtMost(20.0)
            ColorUtils.LABToColor(lab[0], lab[1], lab[2])
        } else {
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(tint, lab)
            lab[0] = 100.0
            lab[1] *= .75
            lab[2] *= .75
            ColorUtils.LABToColor(lab[0], lab[1], lab[2])
        }
    }

    override fun tileColor(iconBackgroundColor: Int) = when {
        iconBackgroundColor == 0 -> palette.neutralMedium
        iconBackgroundColor.alpha == 0 -> ColorTheme.labClosestVibrant(iconBackgroundColor, arrayOf(
            palette.neutralVeryDark,
            palette.neutralDark,
            palette.neutralMedium,
            palette.neutralLight,
            palette.neutralVeryLight,
            palette.primary,
            palette.secondary,
            ColorPalette.wallColor,
        ))
        else -> hueTintClosest(iconBackgroundColor, arrayOf(
            palette.neutralVeryDark,
            palette.neutralDark,
            palette.neutralMedium,
            palette.neutralLight,
            palette.neutralVeryLight,
            palette.primary,
            palette.secondary,
            ColorPalette.wallColor,
        ))
    }
}