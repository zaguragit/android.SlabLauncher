package io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.posidon.android.computable.compute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.search.CompactResult
import io.posidon.android.slablauncher.data.search.ContactResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.app.AppCollection.Companion.convertToGrayscale
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeIcons

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
        result.icon.compute { resultIcon ->
            if (activity.settings.doMonochromeIcons && result !is ContactResult) {
                resultIcon.convertToGrayscale()
            } else resultIcon.colorFilter = null
            icon.post {
                icon.setImageDrawable(resultIcon)
            }
        }
        text.text = result.title
        text.setTextColor(ColorTheme.uiTitle)
        subtitle.hideIfNullOr(result.subtitle) {
            text = it
            setTextColor(ColorTheme.uiDescription)
        }
        itemView.setOnClickListener(result::open)
        itemView.setOnLongClickListener(result.onLongPress?.let { { v -> it(v, activity) } })
    }

    override fun recycle(result: SearchResult) {
        result as CompactResult
        icon.setImageDrawable(null)
        result.icon.offload()
    }
}