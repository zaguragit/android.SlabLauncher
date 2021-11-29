package io.posidon.android.slablauncher.ui.home

import android.annotation.SuppressLint
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.posidon.android.launcherutils.liveWallpaper.LiveWallpaper
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.app.AppCallback
import io.posidon.android.slablauncher.providers.app.AppCollection
import io.posidon.android.slablauncher.providers.color.ColorThemeOptions
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.pinned.TileAreaFragment
import io.posidon.android.slablauncher.ui.home.pinned.acrylicBlur
import io.posidon.android.slablauncher.ui.home.pinned.loadBlur
import io.posidon.android.slablauncher.ui.home.sideList.SideListFragment
import io.posidon.android.slablauncher.ui.popup.PopupUtils
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.util.StackTraceActivity
import io.posidon.android.slablauncher.util.storage.ColorExtractorSetting.colorTheme
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.colorThemeDayNight
import io.posidon.android.slablauncher.util.view.SeeThroughView
import kotlin.concurrent.thread

class MainActivity : FragmentActivity() {

    lateinit var viewPager: ViewPager2

    val launcherContext = LauncherContext()
    val settings by launcherContext::settings

    private lateinit var wallpaperManager: WallpaperManager

    var colorThemeOptions = ColorThemeOptions(settings.colorThemeDayNight)

    private lateinit var blurBG: SeeThroughView

    val appReloader = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadApps()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StackTraceActivity.init(applicationContext)
        settings.init(applicationContext)

        wallpaperManager = WallpaperManager.getInstance(this)
        colorThemeOptions = ColorThemeOptions(settings.colorThemeDayNight)

        setContentView(R.layout.activity_main)

        blurBG = findViewById(R.id.blur_bg)

        viewPager = findViewById(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(i: Int): Fragment = when (i) {
                0 -> TileAreaFragment()
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
                    setBlurLevel(wallpaperOffset)
                    blurBG.offset = wallpaperOffset
                }
                onPageScrollListeners.forEach { (_, l) -> l(wallpaperOffset) }
            }
        })

        viewPager.setOnTouchListener(::onTouch)
        configureWindow()
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
            if (acrylicBlur == null) {
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
        SuggestionsManager.onPause(settings, this)
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
            viewPager.setBackgroundColor(ColorTheme.uiBG and 0xffffff or 0x88000000.toInt())
            HomeLongPressPopup.updateCurrent()
        }
        onColorThemeUpdateListeners.forEach { (_, l) -> l() }
    }

    fun loadApps() {
        launcherContext.appManager.loadApps(this) { apps: AppCollection ->
            onAppsLoadedListeners.forEach { (_, l) -> l(apps) }
            Log.d("SlabLauncher", "updated apps (${apps.size} items)")
        }
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

    fun setOnPageScrollListener(key: String, listener: (Float) -> Unit) {
        onPageScrollListeners[key] = listener
    }

    fun setOnAppsLoadedListener(key: String, listener: (AppCollection) -> Unit) {
        onAppsLoadedListeners[key] = listener
    }

    private val onColorThemeUpdateListeners = HashMap<String, () -> Unit>()
    private val onBlurUpdateListeners = HashMap<String, () -> Unit>()
    private val onPageScrollListeners = HashMap<String, (Float) -> Unit>()
    private val onAppsLoadedListeners = HashMap<String, (AppCollection) -> Unit>()

    private fun updateBlur() {
        onBlurUpdateListeners.forEach { (_, l) -> l() }
        runOnUiThread {
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
        setBlurLevel(0f)
        viewPager.currentItem = 0
    }

    var overlayOpacity = 1f
    var blurLevel = 0f
        private set
    fun updateBlurLevel() {
        setBlurLevel(blurLevel)
        blurBG.invalidate()
    }
    private fun setBlurLevel(f: Float) {
        blurLevel = f
        val l = blurBG.drawable as? LayerDrawable ?: return
        val x = f * 3f
        val invF = 1 - f
        l.getDrawable(0).alpha = if (x > 2f) 0 else (255 * x.coerceAtMost(1f)).toInt()
        l.getDrawable(1).alpha = if (x < 1f) 0 else (255 * (x - 1f).coerceAtMost(1f)).toInt()
        l.getDrawable(2).alpha = if (x < 2f) 0 else (255 * (x - 2f)).toInt()
        l.getDrawable(3).alpha = (200 * overlayOpacity * invF + 100 * f).toInt().also(::println)
        viewPager.background?.alpha = (255 * overlayOpacity * invF + (255 + 128) * f).toInt()
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(appReloader)
        }
        super.onDestroy()
    }
}