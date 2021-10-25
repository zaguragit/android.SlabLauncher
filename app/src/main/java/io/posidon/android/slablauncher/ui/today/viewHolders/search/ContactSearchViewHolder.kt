package io.posidon.android.slablauncher.ui.today.viewHolders.search

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.search.ContactResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme

class ContactSearchViewHolder(
    itemView: View,
) : SearchViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    val text = itemView.findViewById<TextView>(R.id.text)!!

    override fun onBind(result: SearchResult) {
        result as ContactResult
        icon.setImageDrawable(result.contact.icon)
        //RequestOptions.circleCropTransform()
        text.text = result.title
        text.setTextColor(ColorTheme.uiTitle)
        itemView.setOnClickListener(result::open)
    }
}