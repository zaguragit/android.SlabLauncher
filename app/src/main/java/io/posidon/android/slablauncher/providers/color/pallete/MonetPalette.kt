package io.posidon.android.slablauncher.providers.color.pallete

import android.content.res.Resources
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
class MonetPalette(
    resources: Resources,
) : ColorPalette {
    override val estimatedWallColor = resources.getColor(android.R.color.system_neutral1_500, null)

    override val neutralVeryDark = resources.getColor(android.R.color.system_neutral1_1000, null)
    override val neutralDark = resources.getColor(android.R.color.system_neutral1_800, null)
    override val neutralMedium = resources.getColor(android.R.color.system_neutral1_500, null)
    override val neutralLight = resources.getColor(android.R.color.system_neutral1_200, null)
    override val neutralVeryLight = resources.getColor(android.R.color.system_neutral1_0, null)

    override val primary = resources.getColor(android.R.color.system_accent1_300, null)
    override val secondary = resources.getColor(android.R.color.system_accent2_300, null)
}