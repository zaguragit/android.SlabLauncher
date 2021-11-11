package io.posidon.android.slablauncher.ui.today

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.data.search.AppResult
import io.posidon.android.slablauncher.data.search.CompactResult
import io.posidon.android.slablauncher.data.search.InstantAnswerResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.providers.search.SearchQuery
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.today.viewHolders.TitleViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.media.MediaPlayerViewHolder
import io.posidon.android.slablauncher.ui.today.viewHolders.search.CompactSearchViewHolder
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
            is InstantAnswerResult -> RESULT_ANSWER
            is SuggestionsTodayItem -> SUGGESTED_APPS
            is MediaPlayerData -> MEDIA_PLAYER
            else -> throw Exception("Invalid search result")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TITLE -> TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.title, parent, false))
            RESULT_COMPACT -> CompactSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_compact, parent, false))
            RESULT_ANSWER -> AnswerSearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_answer, parent, false))
            SUGGESTED_APPS -> SuggestedAppsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.today_suggested_apps, parent, false), activity)
            MEDIA_PLAYER -> MediaPlayerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.today_media_player, parent, false))
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
            is SuggestedAppsViewHolder -> {
                val realI = if (title != null) i - 1 else i
                holder.onBind(items[realI] as SuggestionsTodayItem, activity)
            }
            is MediaPlayerViewHolder -> {
                val realI = if (title != null) i - 1 else i
                holder.onBind(items[realI] as MediaPlayerData, activity)
            }
        }
    }

    override fun getItemCount(): Int = items.size + if (title != null) 1 else 0

    var currentScreen: Int = -1
        private set

    private var suggested = emptyList<LauncherItem>()

    fun updateSearchResults(query: SearchQuery, results: List<SearchResult>) {
        currentScreen = SCREEN_SEARCH
        this.title = activity.getString(R.string.results_for_x, query.text.toString())
        this.items = results
        notifyDataSetChanged()
    }

    fun updateTodayView(appList: List<App>, force: Boolean = false) {
        val list = run {
            val s = SuggestionsManager.getSuggestions()
            val targetSize = SuggestedAppsViewHolder.COLUMNS * 3 - 1
            if (s.size > targetSize) {
                s.subList(0, targetSize)
            } else if (s.size == targetSize) s else {
                val sa = ArrayList(s)
                while (sa.size < targetSize) {
                    sa += appList.firstOrNull { it !in sa } ?: break
                }
                sa
            }
        }
        if (currentScreen == SCREEN_TODAY && list == suggested && !force) {
            return
        }
        currentScreen = SCREEN_TODAY
        setTitle(R.string.today)
        val resultCount = items.size
        this.items = emptyList()
        notifyItemRangeRemoved(1, resultCount)
        suggested = list
        this.items = listOfNotNull(
            NotificationService.mediaItem,
            SuggestionsTodayItem(
                suggested,
                fragment::setAppsList,
            ),
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
        const val MEDIA_PLAYER = -3
        const val SUGGESTED_APPS = -2
        const val TITLE = -1
        const val RESULT_ANSWER = 0
        const val RESULT_COMPACT = 1

        const val SCREEN_TODAY = 0
        const val SCREEN_SEARCH = 1
        const val SCREEN_ALL_APPS = 2
    }
}
