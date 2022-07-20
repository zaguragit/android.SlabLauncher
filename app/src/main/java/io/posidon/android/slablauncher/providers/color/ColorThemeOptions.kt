package io.posidon.android.slablauncher.providers.color

data class ColorThemeOptions(
    val mode: DayNight
) {
    enum class DayNight {
        AUTO,
        DARK,
        LIGHT,
    }

    override fun toString() = "${javaClass.simpleName} { mode: $mode }"
}