package io.posidon.android.slablauncher.providers.color.theme

import android.content.Context
import androidx.core.graphics.ColorUtils
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme.Companion.hueTintClosest
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme.Companion.tintWithColor
import posidon.android.conveniencelib.Colors

class DarkColorTheme(
    val palette: ColorPalette,
) : ColorTheme {

    override val accentColor = palette.primary

    override val uiBG = palette.neutralVeryDark
    override val uiTitle = palette.neutralVeryLight
    override val uiDescription = palette.neutralLight
    override val uiHint = palette.neutralMedium

    override val cardBG = palette.neutralDark
    override val cardTitle = palette.neutralVeryLight
    override val cardDescription = palette.neutralLight
    override val cardHint = palette.neutralMedium

    override val buttonColor = palette.primary

    override val appCardBase = palette.neutralMedium

    override val searchBarBG = palette.neutralDark
    override val searchBarFG = palette.neutralLight

    override fun adjustColorForContrast(base: Int, tint: Int): Int {
        return if (Colors.getLuminance(base) > .7f) {
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

    override fun tileColor(iconBackgroundColor: Int): Int {
        if (iconBackgroundColor == 0) return palette.neutralMedium
        return hueTintClosest(iconBackgroundColor, arrayOf(
            palette.neutralVeryDark,
            palette.neutralDark,
            palette.neutralMedium,
            palette.neutralLight,
            palette.neutralVeryLight,
            palette.primary,
            palette.secondary,
            ColorPalette.wallColor
        ))
    }

    override fun textColorForBG(context: Context, background: Int): Int {
        return tintWithColor(if (Colors.getLuminance(background) > .6f)
            context.getColor(R.color.feed_card_text_dark_description)
        else context.getColor(R.color.feed_card_text_light_description), background)
    }

    override fun titleColorForBG(context: Context, background: Int): Int {
        return tintWithColor(if (Colors.getLuminance(background) > .6f)
            context.getColor(R.color.feed_card_text_dark_title)
        else context.getColor(R.color.feed_card_text_light_title), background)
    }

    override fun hintColorForBG(context: Context, background: Int): Int {
        return tintWithColor(if (Colors.getLuminance(background) > .6f)
            context.getColor(R.color.feed_card_text_dark_hint)
        else context.getColor(R.color.feed_card_text_light_hint), background)
    }
}