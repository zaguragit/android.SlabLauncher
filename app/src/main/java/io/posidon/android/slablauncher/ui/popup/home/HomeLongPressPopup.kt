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
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.PopupUtils
import io.posidon.android.slablauncher.ui.popup.listPopup.ListPopupAdapter
import io.posidon.android.slablauncher.ui.popup.listPopup.ListPopupItem
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.IconPackPickerActivity
import io.posidon.android.slablauncher.util.storage.ColorExtractorSetting.colorTheme
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.colorThemeDayNight
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.setColorThemeDayNight
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.monochromatism
import io.posidon.android.slablauncher.util.storage.DoShowKeyboardOnAllAppsScreenOpenedSetting.doAutoKeyboardInAllApps
import io.posidon.android.slablauncher.util.storage.DoSuggestionStripSetting.doSuggestionStrip
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.dp
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.min

class HomeLongPressPopup(
    private val update: HomeLongPressPopup.() -> Unit
) {

    private inline fun update() = update(this)

    companion object {

        fun calculateHeight(context: Context) = min(Device.screenHeight(context) / 2, context.dp(360).toInt())

        fun show(
            parent: View,
            touchX: Float,
            touchY: Float,
            settings: Settings,
            reloadColorPalette: () -> Unit,
            updateColorTheme: (ColorPalette) -> Unit,
            reloadApps: () -> Unit,
            reloadBlur: (() -> Unit) -> Unit,
            updateAtAGlanceLayout: () -> Unit,
            popupWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
            popupHeight: Int = calculateHeight(parent.context),
        ) {
            val content = LayoutInflater.from(parent.context).inflate(R.layout.list_popup, null)
            val window = PopupWindow(
                content,
                popupWidth,
                popupHeight,
                true
            )
            PopupUtils.setCurrent(window)

            val blurBG = content.findViewById<SeeThroughView>(R.id.blur_bg)

            val cardView = content.findViewById<CardView>(R.id.card)
            val popupAdapter = ListPopupAdapter()
            val updateLock = ReentrantLock()

            val popup = HomeLongPressPopup {
                blurBG.drawable = acrylicBlur?.insaneBlurDrawable
                cardView.setCardBackgroundColor(ColorTheme.cardBG)
                popupAdapter.updateItems(
                    createMainAdapter(
                        parent.context, settings,
                        reloadColorPalette = {
                            thread(name = "Reloading color palette", isDaemon = true) {
                                updateLock.withLock {
                                    reloadColorPalette()
                                    cardView.post { update() }
                                }
                            }
                        },
                        updateColorTheme = {
                            updateColorTheme(ColorPalette.getCurrent())
                            cardView.post { update() }
                        },
                        reloadApps = reloadApps,
                        reloadBlur = {
                            reloadBlur {
                                cardView.post { update() }
                            }
                        },
                        updateAtAGlanceLayout = {
                            parent.post(updateAtAGlanceLayout)
                        }
                    )
                )
            }

            content.findViewById<RecyclerView>(R.id.recycler).apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = popupAdapter
            }

            popup.update()

            val gravity = Gravity.CENTER
            val x = touchX.toInt() - Device.screenWidth(parent.context) / 2
            val y = touchY.toInt() - Device.screenHeight(parent.context) / 2
            window.showAtLocation(parent, gravity, x, y)
        }

        fun updateCurrent() {
            current?.update()
        }

        private var current: HomeLongPressPopup? = null

        private fun createMainAdapter(
            context: Context,
            settings: Settings,
            reloadColorPalette: () -> Unit,
            updateColorTheme: () -> Unit,
            reloadApps: () -> Unit,
            reloadBlur: () -> Unit,
            updateAtAGlanceLayout: () -> Unit,
        ): List<ListPopupItem> {
            return listOf(
                ListPopupItem(
                    context.getString(R.string.color_theme_gen),
                    description = context.resources.getStringArray(R.array.color_theme_gens)[settings.colorTheme],
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_color_dropper),
                ) {
                    AlertDialog.Builder(context)
                        .setSingleChoiceItems(
                            context.resources.getStringArray(R.array.color_theme_gens).copyOf(context.resources.getInteger(R.integer.color_theme_gens_available)),
                            settings.colorTheme
                        ) { d, i ->
                            settings.edit(context) {
                                colorTheme =
                                    context.resources.getStringArray(R.array.color_theme_gens_data)[i].toInt()
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
                        .setSingleChoiceItems(
                            R.array.color_theme_day_night,
                            settings.colorThemeDayNight.ordinal
                        ) { d, i ->
                            settings.edit(context) {
                                setColorThemeDayNight(context.resources.getStringArray(R.array.color_theme_day_night_data)[i].toInt())
                                updateColorTheme()
                            }
                            d.dismiss()
                        }
                        .show()
                },
                ListPopupItem(
                    context.getString(R.string.blur),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_shapes),
                    value = settings.doBlur,
                    states = 2,
                    onStateChange = { _, value ->
                        settings.edit(context) {
                            doBlur = value == 1
                            reloadBlur()
                        }
                    }
                ),
                ListPopupItem(
                    context.getString(R.string.show_app_suggestions),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_home),
                    value = settings.doSuggestionStrip,
                    states = 2,
                    onStateChange = { _, value ->
                        settings.edit(context) {
                            doSuggestionStrip = value == 1
                            updateAtAGlanceLayout()
                        }
                    }
                ),
                ListPopupItem(context.getString(R.string.tiles), isTitle = true),
                ListPopupItem(
                    context.getString(R.string.icon_packs),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_shapes),
                ) {
                    context.startActivity(Intent(context, IconPackPickerActivity::class.java))
                },
                ListPopupItem(
                    context.getString(R.string.monochrome_icons),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_color_dropper),
                    value = settings.monochromatism,
                    states = 3,
                    onStateChange = { _, value ->
                        settings.edit(context) {
                            monochromatism = value
                            reloadApps()
                        }
                    }
                ),
                ListPopupItem(context.getString(R.string.all_apps), isTitle = true),
                ListPopupItem(
                    context.getString(R.string.auto_show_keyboard),
                    description = context.getString(R.string.auto_show_keyboard_explanation),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_keyboard),
                    value = settings.doAutoKeyboardInAllApps,
                    states = 2,
                    onStateChange = { _, value ->
                        settings.edit(context) {
                            doAutoKeyboardInAllApps = value == 1
                            reloadApps()
                        }
                    }
                ),
            )
        }
    }
}