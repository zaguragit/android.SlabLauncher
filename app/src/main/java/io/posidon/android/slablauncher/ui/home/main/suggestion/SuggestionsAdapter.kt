package io.posidon.android.slablauncher.ui.home.main.suggestion

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.util.storage.Settings

class SuggestionsAdapter(
    val activity: Activity,
    val settings: Settings,
    val graphicsLoader: GraphicsLoader,
) : RecyclerView.Adapter<SuggestionViewHolder>() {

    private var items: List<LauncherItem> = emptyList()

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        return SuggestionViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.suggestion, parent, false))
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, i: Int) {
        val item = items[i]
        holder.onBind(
            item,
            activity.getNavigationBarHeight(),
            graphicsLoader,
            settings,
        )
    }

    override fun onViewRecycled(holder: SuggestionViewHolder) {
        val i = holder.bindingAdapterPosition
        if (i != -1)
            holder.recycle(items[i])
    }

    fun updateItems(items: List<LauncherItem> = this.items) {
        this.items = items
        notifyDataSetChanged()
    }
}