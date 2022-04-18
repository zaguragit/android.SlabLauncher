package io.posidon.android.slablauncher.ui.home

import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.*
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.posidon.android.launcherutil.liveWallpaper.LiveWallpaper
import io.posidon.android.slablauncher.BuildConfig
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.item.AppCallback
import io.posidon.android.slablauncher.providers.color.ColorThemeOptions
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.home.main.DashAreaFragment
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.loadBlur
import io.posidon.android.slablauncher.ui.home.main.suggestion.SuggestionsAdapter
import io.posidon.android.slablauncher.ui.home.sideList.SideListFragment
import io.posidon.android.slablauncher.ui.popup.PopupUtils
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.util.StackTraceActivity
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.slablauncher.util.drawable.setBackgroundColorFast
import io.posidon.android.slablauncher.util.storage.ColorExtractorSetting.colorTheme
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.colorThemeDayNight
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.DoShowKeyboardOnAllAppsScreenOpenedSetting.doAutoKeyboardInAllApps
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.util.storage.DoSuggestionStripSetting.doSuggestionStrip
import io.posidon.android.slablauncher.util.storage.DockRowCount.dockRowCount
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min


class MainActivity : FragmentActivity() {

    companion object {
        const val SUGGESTION_COUNT = 2
    }

    lateinit var viewPager: ViewPager2

    val launcherContext = LauncherContext()
    val settings by launcherContext::settings
    val graphicsLoader = GraphicsLoader()

    private lateinit var wallpaperManager: WallpaperManager

    var colorThemeOptions = ColorThemeOptions(settings.colorThemeDayNight)

    private lateinit var blurBG: SeeThroughView
    private lateinit var searchBarContainer: View
    private lateinit var inSearchBarContainer: View
    private lateinit var searchBarText: EditText
    private lateinit var searchBarIcon: ImageView
    private lateinit var searchBarSeparator: View
    private lateinit var searchBarBlurBG: SeeThroughView
    private lateinit var suggestionsRecycler: RecyclerView

    private val suggestionsAdapter = SuggestionsAdapter(this, settings, graphicsLoader)

    private val appReloader = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadApps()
        }
    }


    @SuppressLint("ClickableViewAccessibility", "WrongConstant", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StackTraceActivity.init(applicationContext)
        settings.init(applicationContext)

        wallpaperManager = WallpaperManager.getInstance(this)
        colorThemeOptions = ColorThemeOptions(settings.colorThemeDayNight)

        setContentView(R.layout.activity_main)

        blurBG = findViewById(R.id.blur_bg)
        searchBarContainer = findViewById(R.id.search_bar_container)!!
        searchBarBlurBG = searchBarContainer.findViewById(R.id.search_bar_blur_bg)!!
        inSearchBarContainer = searchBarContainer.findViewById(R.id.in_search_bar_container)!!
        searchBarText = inSearchBarContainer.findViewById(R.id.search_bar_text)!!
        searchBarIcon = inSearchBarContainer.findViewById(R.id.search_bar_icon)!!
        searchBarSeparator = findViewById(R.id.search_bar_separator)!!

        viewPager = findViewById(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(i: Int): Fragment = when (i) {
                0 -> DashAreaFragment()
                1 -> SideListFragment()
                else -> throw IndexOutOfBoundsException("Fragment [$i] doesn't exist")
            }
        }

        viewPager.offscreenPageLimit = Int.MAX_VALUE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperManager.addOnColorsChangedListener(::onColorsChangedListener, viewPager.handler)
            thread(name = "onCreate color update", isDaemon = true) {
                ColorPalette.onColorsChanged(this, settings.colorTheme, MainActivity::updateColorTheme) {
                    wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                }
            }
            loadBlur(::updateBlur)
        }

        val launcherApps = getSystemService(LauncherApps::class.java)
        launcherApps.registerCallback(AppCallback(::loadApps, ::onPackageLoadingProgressChanged))

        registerReceiver(
            appReloader,
            IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            }
        )

        loadApps()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                val wallpaperOffset = position + positionOffset
                wallpaperManager.setWallpaperOffsets(viewPager.windowToken, wallpaperOffset, 0f)
                setBlurLevel(wallpaperOffset)
                if (blurBG.drawable != null) {
                    blurBG.offset = wallpaperOffset
                    searchBarBlurBG.offset = wallpaperOffset
                }
                if (settings.doSuggestionStrip) {
                    suggestionsRecycler.alpha = 1 - wallpaperOffset
                    suggestionsRecycler.isVisible = wallpaperOffset != 1f
                }
                onPageScrollListeners.forEach { (_, l) -> l(wallpaperOffset) }
            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        searchBarText.text = null
                        searchBarText.clearFocus()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            searchBarContainer.windowInsetsController?.hide(WindowInsets.Type.ime())
                        }
                    }
                    1 -> {
                        if (settings.doAutoKeyboardInAllApps) {
                            searchBarText.requestFocus()
                            val imm = getSystemService(
                                INPUT_METHOD_SERVICE
                            ) as InputMethodManager
                            imm.showSoftInput(searchBarText, InputMethodManager.SHOW_IMPLICIT)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                searchBarContainer.windowInsetsController?.show(WindowInsets.Type.ime())
                            }
                        }
                    }
                }
            }
        })

        viewPager.setOnTouchListener(::onTouch)

        searchBarText.run {
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    if (viewPager.currentItem == 0) {
                        viewPager.currentItem = 1
                    }
                }
            }
            doOnTextChanged { text, _, _, _ ->
                if (hasFocus())
                    onSearchQueryListeners.forEach { (_, l) -> l(text.toString()) }
            }
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = v.text.toString()
                    val hasScheme = query.startsWith("http://") || query.startsWith("https://")
                    if (query.matches(Regex("([a-z]+://)?([a-zA-Z]+\\.)+[a-zA-Z]+"))) {
                        val viewSearch = Intent(Intent.ACTION_VIEW, Uri.parse(query.let {
                            if (hasScheme) it
                            else "https://$it"
                        }))
                        v.context.startActivity(viewSearch)
                        return@setOnEditorActionListener true
                    }

                    try {
                        val viewSearch = Intent(Intent.ACTION_WEB_SEARCH)
                        viewSearch.putExtra(SearchManager.QUERY, query)
                        v.context.startActivity(viewSearch)
                    } catch (e: ActivityNotFoundException) {
                        val viewSearch = Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://duckduckgo.com/?q=${Uri.encode(query)}&t=${BuildConfig.APPLICATION_ID}"
                        ))
                        v.context.startActivity(viewSearch)
                    }
                    true
                } else false
            }
        }
        searchBarContainer.setOnClickListener {
            searchBarText.requestFocus()
        }

        blurBG.setOnApplyWindowInsetsListener { _, insets ->
            configureWindow()
            insets
        }

        suggestionsRecycler = findViewById<RecyclerView>(R.id.suggestions_recycler)!!.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = suggestionsAdapter
        }

        configureWindow()
        updateLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
    }

    private fun configureWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val i = window?.decorView?.rootWindowInsets
            i?.getInsets(WindowInsets.Type.ime())?.bottom?.coerceAtLeast(
                i.getInsets(WindowInsets.Type.systemBars()).bottom
            ) ?: 0
        } else getNavigationBarHeight()
        inSearchBarContainer.setPadding(0, 0, 0, bottomInset)
    }

    fun updateLayout() {
        suggestionsRecycler.isVisible = settings.doSuggestionStrip
        onLayoutChangeListeners.forEach { (_, l) -> l() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val isActionMain = Intent.ACTION_MAIN == intent.action
        if (isActionMain) {
            handleGestureContract(intent)
        }
    }

    private fun handleGestureContract(intent: Intent) {
        //val gnc = GestureNavContract.fromIntent(intent)
        //gnc?.sendEndPosition(scrollBar.clipBounds.toRectF(), null)
    }

    override fun onResume() {
        super.onResume()
        val shouldUpdate = settings.reload(applicationContext)
        if (shouldUpdate) {
            loadApps()
        } else SuggestionsManager.onResume(this) {
            runOnUiThread {
                updateSuggestions(launcherContext.appManager.pinnedItems)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            thread(isDaemon = true) {
                ColorPalette.onResumePreOMR1(
                    this,
                    settings.colorTheme,
                    MainActivity::updateColorTheme
                )
                loadBlur(::updateBlur)
            }
        } else {
            if (settings.doBlur && acrylicBlur == null) {
                loadBlur(::updateBlur)
            }
        }
    }

    fun loadBlur(updateBlur: () -> Unit) = loadBlur(settings, wallpaperManager, updateBlur)

    fun reloadBlur(block: () -> Unit) = loadBlur(settings, wallpaperManager) {
        updateBlur()
        block()
    }

    override fun onPause() {
        super.onPause()
        PopupUtils.dismissCurrent()
        SuggestionsManager.onPause(launcherContext.suggestionData, this)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun onColorsChangedListener(
        colors: WallpaperColors?,
        which: Int
    ) {
        if (which and WallpaperManager.FLAG_SYSTEM != 0) {
            loadBlur(::updateBlur)
            ColorPalette.onColorsChanged(this, settings.colorTheme, MainActivity::updateColorTheme) { colors }
        }
    }

    fun reloadColorPaletteSync() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            ColorPalette.onColorsChanged(this, settings.colorTheme, MainActivity::updateColorTheme) {
                wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            }
        } else ColorPalette.onResumePreOMR1(this, settings.colorTheme, MainActivity::updateColorTheme)
    }

    fun updateColorTheme(colorPalette: ColorPalette) {
        colorThemeOptions = ColorThemeOptions(settings.colorThemeDayNight)
        ColorTheme.updateColorTheme(colorThemeOptions.createColorTheme(colorPalette))
        runOnUiThread {
            viewPager.background = FastColorDrawable(ColorTheme.uiBG).apply {
                viewPager.background?.alpha?.let { alpha = it }
            }
            searchBarContainer.setBackgroundColorFast(ColorTheme.searchBarBG)
            searchBarText.run {
                setTextColor(ColorTheme.searchBarFG)
                highlightColor = ColorTheme.searchBarFG and 0x00ffffff or 0x66000000
            }
            searchBarSeparator.setBackgroundColorFast(ColorTheme.searchBarFG and 0x00ffffff or 0x33000000)
            searchBarIcon.imageTintList =
                ColorStateList.valueOf(ColorTheme.searchBarFG)
            HomeLongPressPopup.updateCurrent()
            suggestionsAdapter.notifyItemRangeChanged(0, suggestionsAdapter.itemCount)
        }
        onColorThemeUpdateListeners.forEach { (_, l) -> l() }
    }

    fun loadApps() {
        launcherContext.appManager.loadApps(this) { list ->
            invalidateItemGraphics()
            runOnUiThread {
                updateSuggestions(launcherContext.appManager.pinnedItems)
            }
            onAppsLoadedListeners.forEach { (_, l) -> l(list) }
        }
    }

    fun invalidateItemGraphics() {
        graphicsLoader.setupNewAppIconLoader(this, settings)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onPackageLoadingProgressChanged(
        packageName: String,
        user: UserHandle,
        progress: Float
    ) {

    }

    fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP)
            LiveWallpaper.tap(v, event.rawX.toInt(), event.rawY.toInt())
        return false
    }

    fun setOnColorThemeUpdateListener(key: String, listener: () -> Unit) {
        onColorThemeUpdateListeners[key] = listener
    }

    fun setOnBlurUpdateListener(key: String, listener: () -> Unit) {
        onBlurUpdateListeners[key] = listener
    }

    fun setOnLayoutChangeListener(key: String, listener: () -> Unit) {
        onLayoutChangeListeners[key] = listener
    }

    fun setOnPageScrollListener(key: String, listener: (Float) -> Unit) {
        onPageScrollListeners[key] = listener
    }

    fun setOnAppsLoadedListener(key: String, listener: (List<App>) -> Unit) {
        onAppsLoadedListeners[key] = listener
    }

    fun setOnSearchQueryListener(key: String, listener: (String?) -> Unit) {
        onSearchQueryListeners[key] = listener
    }

    private val onColorThemeUpdateListeners = HashMap<String, () -> Unit>()
    private val onBlurUpdateListeners = HashMap<String, () -> Unit>()
    private val onLayoutChangeListeners = HashMap<String, () -> Unit>()
    private val onPageScrollListeners = HashMap<String, (Float) -> Unit>()
    private val onAppsLoadedListeners = HashMap<String, (List<App>) -> Unit>()
    private val onSearchQueryListeners = HashMap<String, (String?) -> Unit>()

    fun updateSuggestions(pinnedItems: List<LauncherItem>) {
        suggestionsAdapter.updateItems((SuggestionsManager.get() - pinnedItems.let {
            val s = settings.dockRowCount * HomeArea.calculateColumns(this)
            if (it.size > s) it.subList(0, s)
            else it
        }.toSet()).let {
            if (it.size > SUGGESTION_COUNT) it.subList(0,
                SUGGESTION_COUNT
            )
            else it
        })
    }

    private fun updateBlur() {
        onBlurUpdateListeners.forEach { (_, l) -> l() }
        blurBG.doOnPreDraw {
            updateCurrentBlurBackground()
            HomeLongPressPopup.updateCurrent()
        }
    }

    private fun updateCurrentBlurBackground() {
        blurBG.drawable = acrylicBlur?.let { b ->
            LayerDrawable(
                arrayOf(
                    BitmapDrawable(resources, b.partialBlurSmall),
                    BitmapDrawable(resources, b.partialBlurMedium),
                    BitmapDrawable(resources, b.fullBlur),
                    BitmapDrawable(resources, b.insaneBlur),
                )
            )
        }
        searchBarBlurBG.drawable = acrylicBlur?.let { b ->
            LayerDrawable(
                arrayOf(
                    BitmapDrawable(resources, b.smoothBlur),
                    BitmapDrawable(resources, b.insaneBlur),
                )
            )
        }
        updateBlurLevel()
    }

    var overlayOpacity = 0f
    var blurLevel = 0f
        private set
    fun updateBlurLevel() {
        setBlurLevel(blurLevel)
        blurBG.invalidate()
        searchBarBlurBG.invalidate()
    }
    private fun setBlurLevel(f: Float) {
        blurLevel = f
        val invF = 1 - f
        val l = blurBG.drawable as? LayerDrawable ?: run {
            viewPager.background?.alpha = (127 * overlayOpacity * invF + 255 * f).toInt()
            return
        }
        val x = f * 3f
        l.getDrawable(0).alpha = if (x > 2f) 0 else (255 * x.coerceAtMost(1f)).toInt()
        l.getDrawable(1).alpha = if (x < 1f) 0 else (255 * (x - 1f).coerceAtMost(1f)).toInt()
        l.getDrawable(2).alpha = if (x < 2f) 0 else (255 * (x - 2f)).toInt()
        l.getDrawable(3).alpha = (200 * overlayOpacity * invF + 100 * f).toInt()
        viewPager.background?.alpha = (127 * overlayOpacity * invF + (191) * f).toInt()
        val sl = searchBarBlurBG.drawable as? LayerDrawable ?: return
        sl.getDrawable(0).alpha = ((35 + 220 * min(invF, 1 - overlayOpacity)) * 0.36f).toInt()
        sl.getDrawable(1).alpha = (25 + 230 * max(f, overlayOpacity) * 0.16f).toInt()
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(appReloader)
        }
        super.onDestroy()
    }
}