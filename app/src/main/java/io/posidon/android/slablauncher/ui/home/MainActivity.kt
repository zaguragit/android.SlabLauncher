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
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.launcherutil.liveWallpaper.LiveWallpaper
import io.posidon.android.slablauncher.BuildConfig
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.providers.color.ColorThemeOptions
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.AppCallback
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.providers.personality.Statement
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.main.HomeAreaFragment
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.loadBlur
import io.posidon.android.slablauncher.ui.home.sideList.SideListFragment
import io.posidon.android.slablauncher.ui.popup.PopupUtils
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.util.StackTraceActivity
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.slablauncher.util.storage.ColorExtractorSetting.colorTheme
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.colorThemeDayNight
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.DoShowKeyboardOnAllAppsScreenOpenedSetting.doAutoKeyboardInAllApps
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.concurrent.thread


class MainActivity : FragmentActivity() {

    lateinit var viewPager: ViewPager2

    val launcherContext = LauncherContext()
    val settings by launcherContext::settings
    val graphicsLoader = GraphicsLoader()

    private lateinit var wallpaperManager: WallpaperManager

    private lateinit var blurBG: SeeThroughView
    private lateinit var searchBarContainer: CardView
    private lateinit var searchBarText: EditText
    private lateinit var searchBarIcon: ImageView
    private lateinit var searchBarBlurBG: SeeThroughView
    private lateinit var statementView: TextView

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
        currentIsDark = checkDarkMode()

        setContentView(R.layout.activity_main)

        blurBG = findViewById(R.id.blur_bg)
        searchBarContainer = findViewById(R.id.search_bar_container)!!
        searchBarText = searchBarContainer.findViewById(R.id.search_bar_text)!!
        searchBarIcon = searchBarContainer.findViewById(R.id.search_bar_icon)!!
        statementView = searchBarContainer.findViewById(R.id.statement)!!

        viewPager = findViewById(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(i: Int): Fragment = when (i) {
                0 -> HomeAreaFragment()
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
                if (blurBG.drawable != null) {
                    blurBG.offset = wallpaperOffset
                }
                onPageScrollListeners.forEach { (_, l) -> l(wallpaperOffset) }
                statementView.alpha = 1 - wallpaperOffset
                statementView.isVisible = wallpaperOffset != 1f
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

        configureWindow()
        updateLayout()
    }

    private fun checkDarkMode(): Boolean {
        return when (settings.colorThemeDayNight) {
            ColorThemeOptions.DayNight.AUTO -> {
                val f = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                f != Configuration.UI_MODE_NIGHT_NO
            }
            ColorThemeOptions.DayNight.DARK -> true
            ColorThemeOptions.DayNight.LIGHT -> false
        }
    }

    private var currentIsDark = true

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
        if (settings.colorThemeDayNight == ColorThemeOptions.DayNight.AUTO) {
            val f = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val newIsDark = f != Configuration.UI_MODE_NIGHT_NO
            if (newIsDark != currentIsDark) {
                currentIsDark = newIsDark
                updateColorTheme(ColorPalette.getCurrent())
            }
        }
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
        searchBarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = bottomInset + (resources.getDimension(R.dimen.item_card_margin) * 2).toInt()
        }
    }

    override fun onResume() {
        super.onResume()
        val shouldUpdate = settings.reload(applicationContext)
        if (shouldUpdate) {
            loadApps()
        }
        updateGreeting()
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

    override fun onPause() {
        super.onPause()
        PopupUtils.dismissCurrent()
        SuggestionsManager.onPause(launcherContext.suggestionData, this)
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(appReloader)
        }
        super.onDestroy()
    }

    private fun loadBlur(updateBlur: () -> Unit) = loadBlur(settings, wallpaperManager, updateBlur)

    fun reloadBlur(block: () -> Unit) = loadBlur(settings, wallpaperManager) {
        updateBlur()
        block()
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
        currentIsDark = checkDarkMode()
        ColorTheme.updateColorTheme(ColorTheme.create(colorPalette, isDark = currentIsDark))
        runOnUiThread {
            viewPager.background = FastColorDrawable(ColorTheme.uiBG).apply {
                viewPager.background?.alpha?.let { alpha = it }
            }
            searchBarContainer.setCardBackgroundColor(ColorTheme.searchBarBG)
            searchBarText.run {
                setTextColor(ColorTheme.searchBarFG)
                highlightColor = ColorTheme.accentColor and 0x00ffffff or 0x66000000
            }
            statementView.setTextColor(ColorTheme.searchBarFG)
            searchBarIcon.imageTintList =
                ColorStateList.valueOf(ColorTheme.searchBarFG)
            HomeLongPressPopup.updateCurrent()
        }
        onColorThemeUpdateListeners.forEach { (_, l) -> l() }
    }

    fun updateGreeting() {
        statementView.text = Statement.get(this, Calendar.getInstance(), settings)
    }

    fun loadApps() {
        launcherContext.appManager.loadApps(this) { list ->
            invalidateItemGraphics()
            onAppsLoadedListeners.forEach { (_, l) -> l(list) }
        }
    }

    fun invalidateItemGraphics() {
        graphicsLoader.setupNewAppIconLoader(this, settings)
        onGraphicsLoaderChangedListeners.forEach { (_, l) -> l(graphicsLoader) }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onPackageLoadingProgressChanged(
        packageName: String,
        user: UserHandle,
        progress: Float
    ) {

    }

    private fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP)
            LiveWallpaper.tap(v, event.rawX.toInt(), event.rawY.toInt())
        return false
    }

    fun setOnColorThemeUpdateListener       (key: String, listener: () -> Unit) { onColorThemeUpdateListeners[key] = listener }
    fun setOnBlurUpdateListener             (key: String, listener: () -> Unit) { onBlurUpdateListeners[key] = listener }
    fun setOnLayoutChangeListener           (key: String, listener: () -> Unit) { onLayoutChangeListeners[key] = listener }
    fun setOnPageScrollListener             (key: String, listener: (Float) -> Unit) { onPageScrollListeners[key] = listener }
    fun setOnAppsLoadedListener             (key: String, listener: (List<App>) -> Unit) { onAppsLoadedListeners[key] = listener }
    fun setOnGraphicsLoaderChangeListener   (key: String, listener: (GraphicsLoader) -> Unit) { onGraphicsLoaderChangedListeners[key] = listener }
    fun setOnSearchQueryListener            (key: String, listener: (String?) -> Unit) { onSearchQueryListeners[key] = listener }

    private val onColorThemeUpdateListeners = HashMap<String, () -> Unit>()
    private val onBlurUpdateListeners = HashMap<String, () -> Unit>()
    private val onLayoutChangeListeners = HashMap<String, () -> Unit>()
    private val onPageScrollListeners = HashMap<String, (Float) -> Unit>()
    private val onAppsLoadedListeners = HashMap<String, (List<App>) -> Unit>()
    private val onGraphicsLoaderChangedListeners = HashMap<String, (GraphicsLoader) -> Unit>()
    private val onSearchQueryListeners = HashMap<String, (String?) -> Unit>()

    private fun updateBlur() {
        onBlurUpdateListeners.forEach { (_, l) -> l() }
        blurBG.doOnPreDraw {
            updateCurrentBlurBackground()
            HomeLongPressPopup.updateCurrent()
        }
    }

    private fun updateCurrentBlurBackground() {
        viewPager.background?.alpha = if (acrylicBlur == null) 255 else 190
        blurBG.drawable = acrylicBlur?.let { b ->
            LayerDrawable(
                arrayOf(
                    BitmapDrawable(resources, b.partialBlurMedium),
                    BitmapDrawable(resources, b.fullBlur).apply {
                        alpha = 150
                    },
                    BitmapDrawable(resources, b.insaneBlur).apply {
                        alpha = 120
                    },
                )
            )
        }
        blurBG.invalidate()
    }

    fun getSearchBarInset(): Int =
        (resources.getDimension(R.dimen.search_bar_height) + resources.getDimension(R.dimen.item_card_margin) * 2).toInt()

    fun updateLayout() {
        onLayoutChangeListeners.forEach { (_, l) -> l() }
    }
}