package io.posidon.android.slablauncher.ui.today.viewHolders.search

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
        Glide.with(itemView)
            .load(result.iconUri)
            .placeholder(ContextCompat.getDrawable(itemView.context, R.drawable.placeholder_contact)!!.apply {
                setTint(ColorTheme.uiHint)
            })
            .apply(RequestOptions.circleCropTransform())
            .into(icon)
        text.text = result.title
        text.setTextColor(ColorTheme.uiTitle)
        itemView.setOnClickListener(result::open)
    }
}