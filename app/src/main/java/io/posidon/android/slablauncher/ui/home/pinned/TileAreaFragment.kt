package io.posidon.android.slablauncher.ui.home.pinned

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
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.util.blur.AcrylicBlur
import io.posidon.android.slablauncher.util.storage.*
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import posidon.android.conveniencelib.*
import kotlin.concurrent.thread

var acrylicBlur: AcrylicBlur? = null
    private set

class TileAreaFragment : Fragment() {

    lateinit var tileArea: TileArea
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
        tileArea = TileArea(this, this@TileAreaFragment, launcherContext)

        val a = requireActivity() as MainActivity
        a.setOnColorThemeUpdateListener(TileAreaFragment::class.simpleName!!, ::updateColorTheme)
        a.setOnBlurUpdateListener(TileAreaFragment::class.simpleName!!, ::updateBlur)
        a.setOnAppsLoadedListener(TileAreaFragment::class.simpleName!!) {
            a.runOnUiThread(::updatePinned)
        }
        a.setOnPageScrollListener(TileAreaFragment::class.simpleName!!, ::onOffsetUpdate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateBlur()
        updateColorTheme()
        configureWindow()
    }

    override fun onResume() {
        super.onResume()
        SuggestionsManager.onResume(requireContext()) {
            requireActivity().runOnUiThread {
                tileArea.atAGlance.updateSuggestions(launcherContext.appManager.pinnedItems)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
    }

    private fun configureWindow() {
        val t = resources.getDimension(R.dimen.item_card_margin).toInt()
        tileArea.pinnedRecycler.setPadding(t, 0, t, t)
        tileArea.atAGlance.view.setPadding(t, requireContext().getStatusBarHeight(), t, 0)
        tileArea.atAGlance.view.doOnLayout {
            it.updateLayoutParams {
                val tileMargin = it.context.resources.getDimension(R.dimen.item_card_margin)
                val tileWidth = (Device.screenWidth(it.context) - tileMargin * 2) / TileArea.COLUMNS - tileMargin * 2
                val tileHeight = tileWidth / TileArea.WIDTH_TO_HEIGHT
                val dockHeight = TileArea.DOCK_ROWS * (tileHeight + tileMargin * 2)
                height = requireView().height - (tileMargin + dockHeight.toInt()).toInt() + 1
            }
        }
    }

    private fun updateBlur() {
        activity?.runOnUiThread {
            tileArea.pinnedAdapter.notifyItemRangeChanged(0, tileArea.pinnedAdapter.itemCount)
        }
    }

    private fun updateColorTheme() {
        activity?.runOnUiThread {
            tileArea.atAGlance.updateColorTheme()
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