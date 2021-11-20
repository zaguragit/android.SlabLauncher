package io.posidon.android.slablauncher.ui.home.today.viewHolders.apps

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import posidon.android.conveniencelib.getNavigationBarHeight

class SuggestedAppsAdapter(
    val activity: Activity,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<LauncherItem> = emptyList()
    var openAllApps: () -> Unit = {}

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(i: Int) = if (i == items.size)
        R.layout.today_suggested_apps_show_all
    else R.layout.today_suggested_apps_item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.today_suggested_apps_item -> SuggestedAppViewHolder(LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false) as CardView)
            R.layout.today_suggested_apps_show_all -> ShowAllAppsViewHolder(LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false))
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (i == items.size) {
            holder as ShowAllAppsViewHolder
            holder.onBind(openAllApps)
            return
        }
        val item = items[i]
        holder as SuggestedAppViewHolder
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