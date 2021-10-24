package io.posidon.android.slablauncher.ui.today

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.search.*
import io.posidon.android.slablauncher.providers.search.SearchQuery
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.today.viewHolders.TitleViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.search.CompactSearchViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.search.ContactSearchViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.search.SearchViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.search.instantAnswer.AnswerSearchViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.suggestion.SuggestedAppsViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.suggestion.SuggestionsTodayItem

class TodayAdapter(
    val activity: MainActivity,
    val fragment: TodayFragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var title: String? = activity.getString(R.string.today)
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
            is ContactResult -> RESULT_CONTACT
            is InstantAnswerResult -> RESULT_ANSWER
            is SuggestionsTodayItem -> SUGGESTED_APPS
            else -> throw Exception("Invalid search result")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TITLE -> TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.title, parent, false))
            RESULT_COMPACT -> CompactSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_compact, parent, false), activity)
            RESULT_CONTACT -> ContactSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_contact, parent, false))
            RESULT_ANSWER -> AnswerSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_answer, parent, false), activity)
            SUGGESTED_APPS -> SuggestedAppsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.today_suggested_apps, parent, false), activity)
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (holder is SearchViewHolder) {
            val realI = if (title != null) i - 1 else i
            holder.onBind(items[realI] as SearchResult)
        } else if (holder is TitleViewHolder) {
            holder.onBind(title!!)
        } else if (holder is SuggestedAppsViewHolder) {
            val realI = if (title != null) i - 1 else i
            holder.onBind(items[realI] as SuggestionsTodayItem)
        }
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

    fun updateTodayView() {
        currentScreen = SCREEN_TODAY
        setTitle(R.string.today)
        val resultCount = items.size
        this.items = emptyList()
        notifyItemRangeRemoved(1, resultCount)
        this.items = listOf(
            SuggestionsTodayItem(fragment::setAppsList)
        )
        notifyItemRangeInserted(1, items.size)
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
        const val SUGGESTED_APPS = -2
        const val TITLE = -1
        const val RESULT_CONTACT = 0
        const val RESULT_ANSWER = 1
        const val RESULT_COMPACT = 2

        const val SCREEN_TODAY = 0
        const val SCREEN_SEARCH = 1
        const val SCREEN_ALL_APPS = 2
    }
}
