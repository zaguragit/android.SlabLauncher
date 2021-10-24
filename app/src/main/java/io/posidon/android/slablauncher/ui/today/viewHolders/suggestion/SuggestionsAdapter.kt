package io.posidon.android.slablauncher.ui.today.viewHolders.suggestion

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import posidon.android.conveniencelib.getNavigationBarHeight

class SuggestionsAdapter(
    val activity: Activity,
) : RecyclerView.Adapter<SuggestionViewHolder>() {

    private var items: List<LauncherItem> = emptyList()

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        return SuggestionViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.suggestion, parent, false) as CardView)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, i: Int) {
        val item = items[i]
        holder.onBind(
            item,
            activity.getNavigationBarHeight(),
        )
    }

    fun updateItems(items: List<LauncherItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}