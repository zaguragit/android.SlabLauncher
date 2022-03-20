package io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search

import android.app.WallpaperManager
import android.os.Build
import android.view.View
import androidx.core.view.isVisible
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.search.DebugResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search.instantAnswer.AnswerSearchViewHolder
import io.posidon.android.slablauncher.ui.view.ColorPaletteTestView
import io.posidon.android.slablauncher.ui.view.SeeThroughView

class ColorPaletteSearchViewHolder(
    itemView: View
) : SearchViewHolder(itemView) {

    val paletteViews = arrayOf<ColorPaletteTestView>(
        itemView.findViewById(R.id.palette_view_0)!!,
        itemView.findViewById(R.id.palette_view_1)!!,
        itemView.findViewById(R.id.palette_view_2)!!,
        itemView.findViewById(R.id.palette_view_3)!!,
    )

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    override fun onBind(
        result: SearchResult,
        activity: MainActivity,
    ) {
        result as DebugResult

        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
        blurBG.offset = 1f
        activity.setOnPageScrollListener(AnswerSearchViewHolder::class.simpleName!!) { blurBG.offset = it }

        paletteViews[0].palette = ColorPalette.getDefaultColorPalette()
        paletteViews[1].palette = ColorPalette.getWallColorPalette(itemView.context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wallpaperManager = WallpaperManager.getInstance(itemView.context)
            paletteViews[2].palette = ColorPalette.getSystemWallColorPalette(
                itemView.context,
                wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)!!
            )
            paletteViews[2].isVisible = true
        } else {
            paletteViews[2].isVisible = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            paletteViews[3].palette = ColorPalette.getMonetColorPalette(itemView.context)
            paletteViews[3].isVisible = true
        } else {
            paletteViews[3].isVisible = false
        }
    }
}