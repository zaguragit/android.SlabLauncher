package io.posidon.android.slablauncher.providers.color.pallete

object DefaultPalette : ColorPalette {
    override val estimatedWallColor: Int = 0xff000000.toInt()

    override val neutralVeryDark: Int = 0xff000000.toInt()
    override val neutralDark: Int = 0xff252525.toInt()
    override val neutralMedium: Int = 0xff888888.toInt()
    override val neutralLight: Int = 0xffdddddd.toInt()
    override val neutralVeryLight: Int = 0xffeeeeee.toInt()

    override val primary: Int = 0xffca9aff.toInt()
    override val secondary: Int = 0xffffbb3d.toInt()
}