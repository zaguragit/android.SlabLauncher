package io.posidon.android.slablauncher.ui.home.sideList

import android.content.Context
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.search.*
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.sideList.SideListAdapter.Companion.SCREEN_ALL_APPS
import io.posidon.android.slablauncher.ui.home.sideList.SideListAdapter.Companion.SCREEN_SEARCH
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import posidon.android.conveniencelib.getStatusBarHeight
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs

class SideListFragment : Fragment() {

    private lateinit var adapter: SideListAdapter

    private lateinit var searcher: Searcher

    private lateinit var recyclerView: RecyclerView

    private lateinit var launcherContext: LauncherContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val a = requireActivity() as MainActivity
        launcherContext = a.launcherContext
        searcher = Searcher(
            launcherContext,
            ::AppProvider,
            ::ContactProvider,
            ::DuckDuckGoProvider,
            ::MathProvider,
            update = ::updateResults
        )
    }

    private val appList
        get() = launcherContext.appManager.apps

    override fun onCreateView(
        inflater: LayoutInflater,
        c: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.activity_search, c, false).apply {
        searcher.onCreate(requireActivity())
        recyclerView = findViewById(R.id.recycler)!!
        val a = requireActivity() as MainActivity
        a.setOnColorThemeUpdateListener(SideListFragment::class.simpleName!!, ::updateColorTheme)
        a.setOnPageScrollListener(SideListFragment::class.simpleName!!, ::onOffsetUpdate)
        a.setOnBlurUpdateListener(SideListFragment::class.simpleName!!, ::updateBlur)
        a.setOnAppsLoadedListener(SideListFragment::class.simpleName!!) {
            searcher.onAppsLoaded(a, it)
            reloadResults()
        }
        a.setOnSearchQueryListener(SideListFragment::class.simpleName!!) {
            if (it.isNullOrBlank())
                setAppsList()
            else thread(isDaemon = true) {
                searcher.query(it)
            }
        }
        adapter = SideListAdapter(a, this@SideListFragment)
        recyclerView.run {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            this.adapter = this@SideListFragment.adapter
        }
        setAppsList()

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

        recyclerView.onFlingListener = object : RecyclerView.OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    recyclerView.windowInsetsController?.hide(WindowInsets.Type.ime())
                }
                return false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateColorTheme()
        configureWindow()
        requireActivity().onBackPressedDispatcher.addCallback(owner = viewLifecycleOwner) {
            if (adapter.currentScreen == SCREEN_SEARCH)
                setAppsList()
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
        val tileMargin = resources.getDimension(R.dimen.item_card_margin).toInt()
        recyclerView.setPadding(
            tileMargin,
            tileMargin + requireContext().getStatusBarHeight(),
            tileMargin,
            tileMargin,
        )
    }

    private var lastQuery = SearchQuery.EMPTY
    private fun updateResults(query: SearchQuery, list: List<SearchResult>) = runOnUiThread {
        lastQuery = query
        adapter.updateSearchResults(query, list)
    }

    fun setAppsList() {
        adapter.updateApps(appList)
    }

    private var mainThreadQueue = LinkedList<() -> Unit>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        runQueuedActions()
    }

    private fun runQueuedActions() {
        while (mainThreadQueue.isNotEmpty()) {
            requireActivity().runOnUiThread(mainThreadQueue.remove())
        }
    }

    private fun runOnUiThread(action: () -> Unit) = activity?.runOnUiThread(action) ?: mainThreadQueue.add(action)

    private fun reloadResults() {
        when (adapter.currentScreen) {
            SCREEN_ALL_APPS -> runOnUiThread(::setAppsList)
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
    }

    fun updateColorTheme() {
        runOnUiThread {
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }
    }

    private fun updateBlur() {
        reloadResults()
    }
}