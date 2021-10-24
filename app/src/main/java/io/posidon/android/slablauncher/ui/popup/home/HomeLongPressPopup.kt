package io.posidon.android.slablauncher.ui.popup.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.PopupUtils
import io.posidon.android.slablauncher.ui.popup.listPopup.ListPopupAdapter
import io.posidon.android.slablauncher.ui.popup.listPopup.ListPopupItem
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.IconPackPickerActivity
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.util.storage.ColorThemeDayNightSetting.colorThemeDayNight
import io.posidon.android.slablauncher.util.storage.ColorThemeDayNightSetting.setColorThemeDayNight
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.colorTheme
import io.posidon.android.slablauncher.util.storage.DoReshapeAdaptiveIconsSetting.doReshapeAdaptiveIcons
import io.posidon.android.slablauncher.util.storage.Settings
import posidon.android.conveniencelib.Device
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

object HomeLongPressPopup {

    fun show(
        parent: View,
        touchX: Float,
        touchY: Float,
        navbarHeight: Int,
        settings: Settings,
        reloadColorPalette: () -> Unit,
        updateColorTheme: (ColorPalette) -> Unit,
        reloadApps: () -> Unit,
    ) {
        val content = LayoutInflater.from(parent.context).inflate(R.layout.list_popup, null)
        val window = PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        PopupUtils.setCurrent(window)

        content.findViewById<SeeThroughView>(R.id.blur_bg).run {
            drawable = acrylicBlur?.fullBlurDrawable
            viewTreeObserver.addOnPreDrawListener {
                invalidate()
                true
            }
        }

        val cardView = content.findViewById<CardView>(R.id.card)
        cardView.setCardBackgroundColor(ColorTheme.cardBG)
        content.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ListPopupAdapter().apply {
                val updateLock = ReentrantLock()
                fun update() {
                    updateItems(createMainAdapter(parent.context, settings,
                        reloadColorPalette = {
                            thread(name = "Reloading color palette", isDaemon = true) {
                                updateLock.withLock {
                                    reloadColorPalette()
                                    cardView.post {
                                        cardView.setCardBackgroundColor(ColorTheme.cardBG)
                                        update()
                                    }
                                }
                            }
                        },
                        updateColorTheme = {
                            updateColorTheme(ColorPalette.getCurrent())
                            cardView.post {
                                cardView.setCardBackgroundColor(ColorTheme.cardBG)
                                update()
                            }
                        },
                        reloadApps = reloadApps,
                    ))
                }
                update()
            }
        }

        val gravity = Gravity.CENTER
        val x = touchX.toInt() - Device.screenWidth(parent.context) / 2
        val y = touchY.toInt() - Device.screenHeight(parent.context) / 2 + navbarHeight
        window.showAtLocation(parent, gravity, x, y)
    }

    private fun createMainAdapter(
        context: Context,
        settings: Settings,
        reloadColorPalette: () -> Unit,
        updateColorTheme: () -> Unit,
        reloadApps: () -> Unit,
    ): List<ListPopupItem> {
        return listOf(
            ListPopupItem(
                context.getString(R.string.color_theme_gen),
                description = context.resources.getStringArray(R.array.color_theme_gens)[settings.colorTheme],
                icon = ContextCompat.getDrawable(context, R.drawable.ic_color_dropper),
            ) {
                AlertDialog.Builder(context)
                    .setSingleChoiceItems(R.array.color_theme_gens, settings.colorTheme) { d, i ->
                        settings.edit(context) {
                            colorTheme = context.resources.getStringArray(R.array.color_theme_gens_data)[i].toInt()
                            reloadColorPalette()
                        }
                        d.dismiss()
                    }
                    .show()
            },
            ListPopupItem(
                context.getString(R.string.color_theme_day_night),
                description = context.resources.getStringArray(R.array.color_theme_day_night)[settings.colorThemeDayNight.ordinal],
                icon = ContextCompat.getDrawable(context, R.drawable.ic_lightness),
            ) {
                AlertDialog.Builder(context)
                    .setSingleChoiceItems(R.array.color_theme_day_night, settings.colorThemeDayNight.ordinal) { d, i ->
                        settings.edit(context) {
                            setColorThemeDayNight(context.resources.getStringArray(R.array.color_theme_day_night_data)[i].toInt())
                            updateColorTheme()
                        }
                        d.dismiss()
                    }
                    .show()
            },
            ListPopupItem(context.getString(R.string.icons), isTitle = true),
            ListPopupItem(
                context.getString(R.string.icon_packs),
                icon = ContextCompat.getDrawable(context, R.drawable.ic_shapes),
            ) {
                context.startActivity(Intent(context, IconPackPickerActivity::class.java))
            },
            ListPopupItem(
                context.getString(R.string.reshape_adaptive_icons),
                description = context.getString(R.string.reshape_adaptive_icons_explanation),
                icon = ContextCompat.getDrawable(context, R.drawable.ic_shapes),
                value = settings.doReshapeAdaptiveIcons,
                onToggle = { v, value ->
                    settings.edit(context) {
                        doReshapeAdaptiveIcons = value
                        reloadApps()
                    }
                }
            ),
        )
    }
}