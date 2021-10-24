package io.posidon.android.slablauncher.ui.today.viewHolders.search

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.data.search.SearchResult

abstract class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun onBind(result: SearchResult)
}