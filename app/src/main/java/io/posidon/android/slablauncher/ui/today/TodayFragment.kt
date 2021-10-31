package io.posidon.android.slablauncher.ui.today

import android.app.SearchManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.search.*
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.today.TodayAdapter.Companion.SCREEN_ALL_APPS
import io.posidon.android.slablauncher.ui.today.TodayAdapter.Companion.SCREEN_SEARCH
import io.posidon.android.slablauncher.ui.today.TodayAdapter.Companion.SCREEN_TODAY
import posidon.android.conveniencelib.getNavigationBarHeight
import posidon.android.conveniencelib.getStatusBarHeight
import kotlin.concurrent.thread
import kotlin.math.abs

class TodayFragment : Fragment() {

    private lateinit var adapter: TodayAdapter

    private lateinit var searcher: Searcher

    private lateinit var container: View
    private lateinit var searchBarText: EditText
    private lateinit var searchBarIcon: ImageView
    private lateinit var recyclerView: RecyclerView

    private lateinit var launcherContext: LauncherContext

    /*

    companion object {
        const val MAX_NOTIFICATIONS = 3
    }

    val notificationAdapter = NotificationAdapter()
    val recycler = itemView.findViewById<RecyclerView>(R.id.recycler)!!.apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        /*adapter = notificationAdapter
        NotificationService.setOnUpdate(AtAGlanceViewHolder::class.simpleName!!) {
            mainActivity.runOnUiThread(::updateNotifications)
        }*/
    }

    fun updateNotifications() = notificationAdapter.updateNotifications(NotificationSorter.rearranged(NotificationService.notifications).filter { it.isConversation }.let { it.subList(0, it.size.coerceAtMost(MAX_NOTIFICATIONS)) })

     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val a = requireActivity() as MainActivity
        launcherContext = a.launcherContext
        searcher = Searcher(
            launcherContext,
            ::AppProvider,
            ::ContactProvider,
            ::DuckDuckGoProvider,
            update = ::updateResults
        )
    }

    private var appList = emptyList<App>()

    override fun onCreateView(
        inflater: LayoutInflater,
        c: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.activity_search, c, false).apply {
        searcher.onCreate(requireActivity())
        container = findViewById(R.id.search_bar_container)!!
        searchBarText = container.findViewById(R.id.search_bar_text)!!
        searchBarIcon = container.findViewById(R.id.search_bar_icon)!!
        recyclerView = findViewById(R.id.recycler)!!
        val a = requireActivity() as MainActivity
        a.setOnColorThemeUpdateListener(TodayFragment::class.simpleName!!, ::updateColorTheme)
        a.setOnPageScrollListener(TodayFragment::class.simpleName!!, ::onOffsetUpdate)
        a.setOnBlurUpdateListener(TodayFragment::class.simpleName!!, ::updateBlur)
        a.setOnAppsLoadedListener(TodayFragment::class.simpleName!!) {
            appList = it.list
            searcher.onAppsLoaded(a, it)
            reloadResults()
        }
        adapter = TodayAdapter(a, this@TodayFragment)
        recyclerView.run {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            this.adapter = this@TodayFragment.adapter
        }
        setTodayView()
        findViewById<EditText>(R.id.search_bar_text).run {
            doOnTextChanged { text, _, _, _ ->
                if (text.isNullOrEmpty())
                    setTodayView()
                else thread(isDaemon = true) {
                    searcher.query(text)
                }
            }
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val viewSearch = Intent(Intent.ACTION_WEB_SEARCH)
                    viewSearch.putExtra(SearchManager.QUERY, v.text)
                    v.context.startActivity(viewSearch)
                    true
                } else false
            }
        }

        setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    ((event.localState as? Pair<*, *>?)?.first as? View)?.visibility = View.INVISIBLE
                    return@setOnDragListener true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val pair = (event.localState as? Pair<*, *>?)
                    val v = pair?.first as? View
                    val location = pair?.second as? IntArray
                    if (v != null && location != null) {
                        val x = abs(event.x - location[0] - v.measuredWidth / 2f)
                        val y = abs(event.y - location[1] - v.measuredHeight / 2f)
                        if (x > v.width / 3.5f || y > v.height / 3.5f) {
                            ItemLongPress.currentPopup?.dismiss()
                            (requireActivity() as MainActivity).viewPager.currentItem = 0
                        }
                    }
                }
                DragEvent.ACTION_DRAG_ENDED,
                DragEvent.ACTION_DROP -> {
                    ((event.localState as? Pair<*, *>?)?.first as? View)?.visibility = View.VISIBLE
                    ItemLongPress.currentPopup?.isFocusable = true
                    ItemLongPress.currentPopup?.update()
                }
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        SuggestionsManager.onResume(requireContext()) {
            if (adapter.currentScreen == SCREEN_TODAY) {
                requireActivity().runOnUiThread {
                    adapter.updateTodayView(appList)
                }
            }
        }
    }

    private fun setTodayView() {
        adapter.updateTodayView(appList)
    }

    fun setAppsList() {
        adapter.updateApps(appList)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateColorTheme()
        configureWindow()
        view.setOnApplyWindowInsetsListener { _, insets ->
            configureWindow()
            insets
        }
        requireActivity().onBackPressedDispatcher.addCallback(owner = viewLifecycleOwner) {
            if (adapter.currentScreen == SCREEN_ALL_APPS)
                adapter.updateTodayView(appList, force = true)
            else {
                val a = requireActivity() as MainActivity
                if (a.viewPager.currentItem != 0) {
                    a.viewPager.currentItem--
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
    }

    private fun configureWindow() {
        val a = requireActivity()
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val i = a.window?.decorView?.rootWindowInsets
            i?.getInsets(WindowInsets.Type.ime())?.bottom?.coerceAtLeast(
                i.getInsets(WindowInsets.Type.systemBars()).bottom
            ) ?: 0
        } else a.getNavigationBarHeight()
        container.setPadding(0, 0, 0, bottomInset)
        recyclerView.setPadding(
            recyclerView.paddingLeft,
            requireContext().getStatusBarHeight(),
            recyclerView.paddingRight,
            recyclerView.paddingBottom,
        )
    }

    private var lastQuery = SearchQuery.EMPTY
    private fun updateResults(query: SearchQuery, list: List<SearchResult>) = requireActivity().runOnUiThread {
        lastQuery = query
        adapter.updateSearchResults(query, list)
    }


    private fun reloadResults() {
        when (adapter.currentScreen) {
            SCREEN_TODAY -> activity?.runOnUiThread {
                adapter.updateTodayView(appList, force = true)
            }
            SCREEN_ALL_APPS -> activity?.runOnUiThread {
                adapter.updateApps(appList)
            }
            SCREEN_SEARCH -> thread(isDaemon = true) {
                searcher.query(lastQuery)
            }
        }
    }

    private fun onOffsetUpdate(offset: Float) {
        val f = (1 - offset)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val r = f * 18f
            view?.setRenderEffect(
                if (r == 0f) null else
                    RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP)
            )
        }
        val i = offset * offset * 4
        view?.alpha = .5f + i * .5f
        if (offset < .5f) {
            searchBarText.clearFocus()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                container.windowInsetsController?.hide(WindowInsets.Type.ime())
            }
        }
    }

    fun updateColorTheme() {
        activity?.runOnUiThread {
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
            container.setBackgroundColor(ColorTheme.searchBarBG)
            searchBarText.run {
                setTextColor(ColorTheme.searchBarFG)
                highlightColor = ColorTheme.searchBarFG and 0x00ffffff or 0x66000000
            }
            searchBarIcon.imageTintList =
                ColorStateList.valueOf(ColorTheme.searchBarFG)
        }
    }

    private fun updateBlur() {
        reloadResults()
    }
}