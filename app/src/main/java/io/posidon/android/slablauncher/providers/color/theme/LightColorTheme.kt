package io.posidon.android.slablauncher.providers.color.theme

import android.content.Context
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme.Companion.hueTintClosest
import posidon.android.conveniencelib.Colors

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

    override val buttonColor = palette.primary

    override val appCardBase = palette.neutralVeryLight

    override val searchBarBG = palette.neutralLight
    override val searchBarFG = palette.neutralDark

    override fun adjustColorForContrast(base: Int, tint: Int): Int {
        return if (Colors.getLuminance(base) > .6f) {
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

    override fun textColorForBG(context: Context, background: Int): Int {
        return if (Colors.getLuminance(background) > .6f)
            context.getColor(R.color.feed_card_text_dark_description)
        else context.getColor(R.color.feed_card_text_light_description)
    }

    override fun titleColorForBG(context: Context, background: Int): Int {
        return if (Colors.getLuminance(background) > .6f)
            context.getColor(R.color.feed_card_text_dark_title)
        else context.getColor(R.color.feed_card_text_light_title)
    }

    override fun hintColorForBG(context: Context, background: Int): Int {
        return if (Colors.getLuminance(background) > .6f)
            context.getColor(R.color.feed_card_text_dark_hint)
        else context.getColor(R.color.feed_card_text_light_hint)
    }
}