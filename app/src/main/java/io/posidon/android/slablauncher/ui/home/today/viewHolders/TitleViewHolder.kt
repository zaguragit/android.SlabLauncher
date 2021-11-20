package io.posidon.android.slablauncher.ui.home.today.viewHolders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme

class TitleViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    val text = itemView.findViewById<TextView>(R.id.text)!!

    fun onBind(title: String) {
        text.text = title
        text.setTextColor(ColorTheme.uiTitle)
    }
}