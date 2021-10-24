package io.posidon.android.slablauncher.providers.color

import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.color.theme.DarkColorTheme
import io.posidon.android.slablauncher.providers.color.theme.LightColorTheme

data class ColorThemeOptions(
    val mode: DayNight
) {
    enum class DayNight {
        AUTO,
        DARK,
        LIGHT,
    }

    fun createColorTheme(palette: ColorPalette): ColorTheme {
        return if (mode == DayNight.LIGHT) LightColorTheme(palette)
        else DarkColorTheme(palette)
    }

    override fun toString() = "${javaClass.simpleName} { mode: $mode }"
}