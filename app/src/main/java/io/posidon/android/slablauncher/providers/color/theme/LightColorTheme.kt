package io.posidon.android.slablauncher.providers.color.theme

import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.luminance
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme.Companion.hueTintClosest

class LightColorTheme(
    val palette: ColorPalette,
) : ColorTheme {

    override val accentColor = palette.primary
    override val secondaryAccentColor = palette.secondary

    override val uiBG = palette.neutralDark
    override val uiTitle = palette.neutralVeryLight
    override val uiDescription = palette.neutralLight
    override val uiHint = palette.neutralMedium

    override val cardBG = 0xffffffff.toInt()
    override val cardTitle = palette.neutralVeryDark
    override val cardDescription = palette.neutralDark
    override val cardHint = palette.neutralMedium

    override val separator = palette.neutralVeryDark and 0xffffff or 0x33000000

    override val buttonColor = palette.neutralVeryLight
    override val buttonColorCallToAction = palette.secondary

    override val searchBarBG = ColorUtils.blendARGB(cardBG, accentColor, 0.6f)
    override val searchBarFG = ColorTheme.titleColorForBG(searchBarBG)

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
        else -> hueTintClosest(iconBackgroundColor, cardBG, arrayOf(
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