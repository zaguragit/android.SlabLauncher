package io.posidon.android.slablauncher.ui.today.viewHolders.search

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.search.CompactResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.applyIfNotNull

class CompactSearchViewHolder(
    itemView: View,
    val activity: Activity,
) : SearchViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    val text = itemView.findViewById<TextView>(R.id.text)!!
    val subtitle = itemView.findViewById<TextView>(R.id.subtitle)!!

    override fun onBind(result: SearchResult) {
        result as CompactResult
        icon.setImageDrawable(result.icon)
        text.text = result.title
        text.setTextColor(ColorTheme.uiTitle)
        applyIfNotNull(subtitle, result.subtitle) { s, t ->
            s.text = t
            subtitle.setTextColor(ColorTheme.uiDescription)
        }
        itemView.setOnClickListener(result::open)
        itemView.setOnLongClickListener(result.onLongPress?.let { { v -> it(v, activity) } })
    }
}