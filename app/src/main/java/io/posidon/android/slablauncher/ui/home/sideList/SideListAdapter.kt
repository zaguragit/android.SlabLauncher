package io.posidon.android.slablauncher.ui.home.sideList

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.search.*
import io.posidon.android.slablauncher.providers.search.SearchQuery
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.TitleViewHolder
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search.ColorPaletteSearchViewHolder
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search.CompactSearchViewHolder
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search.SearchViewHolder
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search.SimpleBoxSearchViewHolder
import io.posidon.android.slablauncher.ui.home.sideList.viewHolders.search.instantAnswer.AnswerSearchViewHolder

class SideListAdapter(
    val activity: MainActivity,
    val fragment: SideListFragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var title: String? = null
        set(value) {
            if (field != value) {
                field = value
                if (value == null)
                    notifyItemRemoved(0)
                else
                    notifyItemChanged(0)
            }
        }

    inline fun setTitle(@StringRes resId: Int) { title = if (resId == 0) null else activity.getString(resId) }

    private var items = emptyList<Any>()

    override fun getItemViewType(i: Int): Int {
        val realI = if (title != null) i - 1 else i
        if (realI == -1)
            return TITLE
        return when (items[realI]) {
            is CompactResult -> RESULT_COMPACT
            is InstantAnswerResult -> RESULT_ANSWER
            is MathResult -> RESULT_SIMPLE_BOX
            is DebugResult -> DEBUG
            else -> throw Exception("Invalid search result")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TITLE -> TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.title, parent, false))
            RESULT_COMPACT -> CompactSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_compact, parent, false))
            RESULT_ANSWER -> AnswerSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_answer, parent, false))
            RESULT_SIMPLE_BOX -> SimpleBoxSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_simple_box, parent, false))
            DEBUG -> ColorPaletteSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_debug, parent, false))
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        when (holder) {
            is SearchViewHolder -> {
                val realI = if (title != null) i - 1 else i
                holder.onBind(items[realI] as SearchResult, activity)
            }
            is TitleViewHolder -> {
                holder.onBind(title!!)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val i = holder.bindingAdapterPosition
        val realI = if (title != null) i - 1 else i
        if (i >= 0 && holder is SearchViewHolder)
            holder.recycle(items[realI] as SearchResult)
    }

    override fun getItemCount(): Int = items.size + if (title != null) 1 else 0

    var currentScreen: Int = -1
        private set

    fun updateSearchResults(query: SearchQuery, results: List<SearchResult>) {
        currentScreen = SCREEN_SEARCH
        this.title = activity.getString(R.string.results_for_x, query.text.toString())
        this.items = results
        notifyDataSetChanged()
    }

    fun updateApps(apps: List<App>) {
        currentScreen = SCREEN_ALL_APPS
        setTitle(R.string.all_apps)
        val resultCount = items.size
        this.items = emptyList()
        notifyItemRangeRemoved(1, resultCount)
        this.items = apps.map(::AppResult)
        notifyItemRangeInserted(1, items.size)
    }

    companion object {
        const val TITLE = -1
        const val RESULT_ANSWER = 0
        const val RESULT_COMPACT = 1
        const val RESULT_SIMPLE_BOX = 2
        const val DEBUG = 3

        const val SCREEN_SEARCH = 1
        const val SCREEN_ALL_APPS = 2
    }
}
