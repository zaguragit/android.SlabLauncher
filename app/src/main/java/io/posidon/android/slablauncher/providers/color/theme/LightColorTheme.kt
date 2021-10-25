package io.posidon.android.slablauncher.providers.color.theme

import android.content.Context
import androidx.core.graphics.ColorUtils
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import posidon.android.conveniencelib.Colors

class LightColorTheme(
    palette: ColorPalette,
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
        return if (Colors.getLuminance(base) > .7f) {
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(tint, lab)
            lab[0] = lab[0].coerceAtMost(20.0)
            ColorUtils.LABToColor(lab[0], lab[1], lab[2])
        } else {
            val hsl = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(tint, hsl)
            hsl[2] = hsl[2].coerceAtLeast(.92f - hsl[1] * .2f)
            ColorUtils.HSLToColor(hsl)
        }
    }

    override fun tintAppDrawerItem(color: Int): Int {
        val baseLab = DoubleArray(3)
        ColorUtils.colorToLAB(appCardBase, baseLab)
        val colorLab = DoubleArray(3)
        ColorUtils.colorToLAB(color, colorLab)
        val accentLab = DoubleArray(3)
        ColorUtils.colorToLAB(accentColor, accentLab)
        val r = ColorTheme.splitTint(baseLab, colorLab, accentLab)
        return ColorUtils.LABToColor(r[0], r[1], r[2])
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