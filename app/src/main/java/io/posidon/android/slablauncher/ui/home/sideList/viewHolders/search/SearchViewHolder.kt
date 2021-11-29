package io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.ui.home.MainActivity

abstract class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun onBind(result: SearchResult, activity: MainActivity)
}