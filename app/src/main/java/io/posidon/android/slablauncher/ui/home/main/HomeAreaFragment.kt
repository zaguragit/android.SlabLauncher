package io.posidon.android.slablauncher.ui.home.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
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

class DashAreaFragment : Fragment() {

    lateinit var homeArea: HomeArea
        private set

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
        homeArea = HomeArea(this as NestedScrollView, this@DashAreaFragment, launcherContext)

        val a = requireActivity() as MainActivity
        a.setOnColorThemeUpdateListener(DashAreaFragment::class.simpleName!!, ::updateColorTheme)
        a.setOnBlurUpdateListener(DashAreaFragment::class.simpleName!!, ::updateBlur)
        a.setOnAppsLoadedListener(DashAreaFragment::class.simpleName!!) {
            a.runOnUiThread(homeArea::updatePinned)
        }
        a.setOnGraphicsLoaderChangeListener(DashAreaFragment::class.simpleName!!) {
            a.runOnUiThread(homeArea::forceUpdatePinned)
        }
        a.setOnPageScrollListener(DashAreaFragment::class.simpleName!!, ::onOffsetUpdate)
        a.setOnLayoutChangeListener(DashAreaFragment::class.simpleName!!, ::updateLayout)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateBlur()
        updateColorTheme()
        configureWindow()
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
        homeArea.pinnedRecycler.setPadding(t, 0, t, t)
        homeArea.dash.view.setPadding(t, requireContext().getStatusBarHeight(), t, 0)
        updateLayout()
    }

    private fun updateLayout() {
        homeArea.pinnedRecycler.layoutManager = GridLayoutManager(
            homeArea.view.context,
            HomeArea.calculateColumns(homeArea.view.context, launcherContext.settings),
            RecyclerView.VERTICAL,
            false
        )
        homeArea.dash.view.doOnLayout {
            it.updateLayoutParams {
                val tileMargin = it.context.resources.getDimension(R.dimen.item_card_margin)
                val tileWidth = (Device.screenWidth(it.context) - tileMargin * 2) / HomeArea.calculateColumns(it.context, launcherContext.settings) - tileMargin * 2
                val tileHeight = tileWidth / HomeArea.WIDTH_TO_HEIGHT
                val dockHeight = launcherContext.settings.dockRowCount * (tileHeight + tileMargin * 2)
                height = requireView().height - (tileMargin + dockHeight.toInt()).toInt()
            }
        }
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