package io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.search.CompactResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings

class CompactSearchViewHolder(
    itemView: View
) : SearchViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    val text = itemView.findViewById<TextView>(R.id.text)!!
    val subtitle = itemView.findViewById<TextView>(R.id.subtitle)!!

    override fun onBind(
        result: SearchResult,
        activity: MainActivity,
    ) {
        result as CompactResult
        icon.setImageDrawable(null)

        activity.graphicsLoader.load(itemView.context, result.launcherItem) {
            icon.post {
                icon.setImageDrawable(it.icon)
                icon.colorFilter = if (activity.settings.doMonochrome) {
                    ColorMatrixColorFilter(ColorMatrix().apply {
                        setSaturation(0f)
                    })
                } else null
            }
        }

        text.text = result.title
        text.setTextColor(ColorTheme.uiTitle)
        subtitle.hideIfNullOr(result.subtitle) {
            text = it
            setTextColor(ColorTheme.uiDescription)
        }
        itemView.setOnClickListener(result::open)
        itemView.setOnLongClickListener(result.onLongPress?.let { { v -> it(activity.graphicsLoader, v, activity) } })
    }

    override fun recycle(result: SearchResult) {
        icon.setImageDrawable(null)
    }
}