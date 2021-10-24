package io.posidon.android.slablauncher.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.home.pinned.TileArea
import io.posidon.android.slablauncher.util.blur.AcrylicBlur
import io.posidon.android.slablauncher.util.storage.*
import posidon.android.conveniencelib.*
import kotlin.concurrent.thread

var acrylicBlur: AcrylicBlur? = null
    private set

class LauncherFragment : Fragment() {

    private lateinit var tileArea: TileArea

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
        tileArea = TileArea(this, this@LauncherFragment, launcherContext)

        val a = requireActivity() as MainActivity
        a.setOnColorThemeUpdateListener(LauncherFragment::class.simpleName!!, ::updateColorTheme)
        a.setOnBlurUpdateListener(LauncherFragment::class.simpleName!!, ::updateBlur)
        a.setOnAppsLoadedListener(LauncherFragment::class.simpleName!!) {
            a.runOnUiThread(::updatePinned)
        }
        a.setOnPageScrollListener(LauncherFragment::class.simpleName!!, ::onOffsetUpdate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateBlur()
        updateColorTheme()
        configureWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
    }

    private fun configureWindow() {
        val tileMargin = resources.getDimension(R.dimen.item_card_margin).toInt()
        tileArea.pinnedRecycler.setPadding(tileMargin, tileMargin + requireContext().getStatusBarHeight(), tileMargin, tileMargin + requireActivity().getNavigationBarHeight())
    }

    private fun updateBlur() {
        requireActivity().runOnUiThread {
            tileArea.pinnedAdapter.notifyItemRangeChanged(0, tileArea.pinnedAdapter.itemCount)
        }
    }

    private fun updateColorTheme() {
        requireActivity().runOnUiThread {
            tileArea.pinnedAdapter.notifyItemRangeChanged(0, tileArea.pinnedAdapter.itemCount)
        }
    }

    private fun updatePinned() = tileArea.updatePinned()

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

    companion object {
        fun Activity.loadBlur(wallpaperManager: WallpaperManager, updateBlur: () -> Unit) = thread(isDaemon = true, name = "Blur thread") {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                if (acrylicBlur == null) return@thread
                acrylicBlur = null
                runOnUiThread(updateBlur)
                return@thread
            }
            val drawable = wallpaperManager.peekDrawable()
            if (drawable == null) {
                if (acrylicBlur == null) return@thread
                acrylicBlur = null
                runOnUiThread(updateBlur)
                return@thread
            }
            AcrylicBlur.blurWallpaper(this, drawable) {
                acrylicBlur = it
                updateBlur()
            }
        }
    }
}