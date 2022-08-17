package io.posidon.android.slablauncher.ui.home.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.util.blur.AcrylicBlur
import io.posidon.android.slablauncher.util.storage.*
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.DockRowCount.dockRowCount
import io.posidon.android.conveniencelib.*
import kotlin.concurrent.thread

var acrylicBlur: AcrylicBlur? = null
    private set

class HomeAreaFragment : Fragment() {

    companion object {
        fun calculateDockHeight(context: Context, settings: Settings): Int {
            val tileMargin = context.resources.getDimension(R.dimen.item_card_margin)
            val tileWidth = (Device.screenWidth(context) - tileMargin * 2) / HomeArea.calculateColumns(context, settings) - tileMargin * 2
            val tileHeight = tileWidth / HomeArea.WIDTH_TO_HEIGHT
            val dockHeight = settings.dockRowCount * (tileHeight + tileMargin * 2)
            return (tileMargin + dockHeight.toInt()).toInt()
        }
    }

    private lateinit var homeArea: HomeArea
    private lateinit var launcherContext: LauncherContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val a = requireActivity() as MainActivity
        launcherContext = a.launcherContext
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.activity_launcher, container, false).apply {
        homeArea = HomeArea(this as NestedScrollView, this@HomeAreaFragment, launcherContext)

        val a = requireActivity() as MainActivity
        a.setOnColorThemeUpdateListener(HomeAreaFragment::class.simpleName!!, ::updateColorTheme)
        a.setOnBlurUpdateListener(HomeAreaFragment::class.simpleName!!, ::updateBlur)
        a.setOnAppsLoadedListener(HomeAreaFragment::class.simpleName!!) {
            a.runOnUiThread(homeArea::updatePinned)
        }
        a.setOnGraphicsLoaderChangeListener(HomeAreaFragment::class.simpleName!!) {
            a.runOnUiThread(homeArea::forceUpdatePinned)
        }
        a.setOnPageScrollListener(HomeAreaFragment::class.simpleName!!, ::onOffsetUpdate)
        a.setOnLayoutChangeListener(HomeAreaFragment::class.simpleName!!, homeArea::updateLayout)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateBlur()
        updateColorTheme()
        configureWindow()
        view.viewTreeObserver?.addOnWindowFocusChangeListener(::onWindowFocusChanged)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        homeArea.onWindowFocusChanged(hasFocus)
    }

    override fun onResume() {
        super.onResume()
        homeArea.dash.onResume()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
    }

    private fun configureWindow() {
        val t = resources.getDimension(R.dimen.item_card_margin).toInt()
        val r = resources.getDimension(R.dimen.item_card_radius).toInt()
        homeArea.pinnedRecycler.doOnLayout {
            homeArea.pinnedRecycler.setPadding(t, 0, t, (requireActivity() as MainActivity).getSearchBarInset() - t - r)
        }
        homeArea.dash.view.setPadding(t, requireContext().getStatusBarHeight(), t, 0)
        homeArea.updateLayout()
    }

    private fun updateBlur() {
        activity?.runOnUiThread {
            homeArea.updateBlur()
        }
    }

    private fun updateColorTheme() {
        activity?.runOnUiThread {
            homeArea.dash.updateColorTheme()
            homeArea.pinnedAdapter.notifyItemRangeChanged(0, homeArea.pinnedAdapter.itemCount)
        }
    }

    private fun onOffsetUpdate(offset: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val r = offset * 18f
            view?.setRenderEffect(
                if (r == 0f) null else
                    RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP)
            )
        }
        val i = (1 - offset * offset)
        view?.alpha = i * 1.1f
    }
}

fun Activity.loadBlur(settings: Settings, wallpaperManager: WallpaperManager, updateBlur: () -> Unit) = thread(isDaemon = true, name = "Blur thread") {
    if (!settings.doBlur || ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) != PackageManager.PERMISSION_GRANTED) {
        acrylicBlur ?: return@thread
        acrylicBlur = null
        updateBlur()
        return@thread
    }
    val drawable = wallpaperManager.peekDrawable()
    if (drawable == null) {
        acrylicBlur ?: return@thread
        acrylicBlur = null
        updateBlur()
        return@thread
    }
    AcrylicBlur.blurWallpaper(this, drawable) {
        acrylicBlur = it
        updateBlur()
    }
}