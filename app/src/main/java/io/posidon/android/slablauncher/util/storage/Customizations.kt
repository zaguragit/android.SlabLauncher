package io.posidon.android.slablauncher.util.storage

import io.posidon.android.slablauncher.providers.color.ColorThemeOptions

object ColorThemeSetting {
    val Settings.colorTheme: Int
        get() = get(KEY_COLOR_THEME, COLOR_THEME_DEFAULT)

    var Settings.SettingsEditor.colorTheme: Int
        get() = settings[KEY_COLOR_THEME, COLOR_THEME_DEFAULT]
        set(value) = KEY_COLOR_THEME set value

    private const val KEY_COLOR_THEME = "color_theme"

    const val COLOR_THEME_PLAIN = 0
    const val COLOR_THEME_WALLPAPER_TINT = 1
    const val COLOR_THEME_WALLPAPER_TINT_SYSTEM_ASSISTED = 2

    const val COLOR_THEME_DEFAULT = COLOR_THEME_WALLPAPER_TINT
}

object ColorThemeDayNightSetting {
    val Settings.colorThemeDayNight: ColorThemeOptions.DayNight
        get() = ColorThemeOptions.DayNight.values()[get(KEY, DEFAULT)]

    var Settings.SettingsEditor.colorThemeDayNight: ColorThemeOptions.DayNight
        get() = ColorThemeOptions.DayNight.values()[settings[KEY, DEFAULT]]
        inline set(value) = setColorThemeDayNight(value.ordinal)

    fun Settings.SettingsEditor.setColorThemeDayNight(i: Int) { KEY set i }

    private const val KEY = "color_theme:day_night"

    const val DEFAULT = 0
}

object ScrollbarControllerSetting {
    val Settings.scrollbarController: Int
        get() = get(KEY_SCROLLBAR_CONTROLLER, SCROLLBAR_CONTROLLER_DEFAULT)

    var Settings.SettingsEditor.scrollbarController: Int
        get() = settings[KEY_SCROLLBAR_CONTROLLER, SCROLLBAR_CONTROLLER_DEFAULT]
        set(value) = KEY_SCROLLBAR_CONTROLLER set value

    private const val KEY_SCROLLBAR_CONTROLLER = "scrollbar_controller"

    const val SCROLLBAR_CONTROLLER_ALPHABETIC = 0
    const val SCROLLBAR_CONTROLLER_BY_HUE = 1

    const val SCROLLBAR_CONTROLLER_DEFAULT = SCROLLBAR_CONTROLLER_ALPHABETIC
}

object DoReshapeAdaptiveIconsSetting {
    val Settings.doReshapeAdaptiveIcons: Boolean
        get() = get(KEY, DEFAULT)

    var Settings.SettingsEditor.doReshapeAdaptiveIcons: Boolean
        get() = settings[KEY, DEFAULT]
        set(value) = KEY set value

    private const val KEY = "icons:reshape_adaptive"

    const val DEFAULT = false
}