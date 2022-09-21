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
import io.posidon.android.slablauncher.ui.settings.SettingsAdapter
import io.posidon.android.slablauncher.ui.settings.SettingsItem
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.IconPackPickerActivity
import io.posidon.android.slablauncher.util.storage.ColorExtractorSetting.colorTheme
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.colorThemeDayNight
import io.posidon.android.slablauncher.util.storage.ColorThemeSetting.setColorThemeDayNight
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.DoShowKeyboardOnAllAppsScreenOpenedSetting.doAutoKeyboardInAllApps
import io.posidon.android.slablauncher.util.storage.DoSuggestionStripSetting.doSuggestionStrip
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.monochromatism
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.BuildConfig
import io.posidon.android.slablauncher.ui.settings.flag.FlagSettingsActivity
import io.posidon.android.slablauncher.util.chooseDefaultLauncher
import io.posidon.android.slablauncher.util.storage.GreetingSetting.getDefaultGreeting
import io.posidon.android.slablauncher.util.storage.GreetingSetting.setDefaultGreeting
import io.posidon.android.slablauncher.util.storage.SuggestionColumnCount.suggestionColumnCount
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.min

class HomeLongPressPopup(
    private val update: HomeLongPressPopup.() -> Unit
) {

    private inline fun update() = update(this)

    companion object {

        fun calculateHeight(context: Context) = min(
            Device.screenHeight(context) / 2,
            360.dp.toPixels(context)
        )

        fun show(
            parent: View,
            touchX: Float,
            touchY: Float,
            settings: Settings,
            reloadColorPalette: () -> Unit,
            updateColorTheme: (ColorPalette) -> Unit,
            reloadItemGraphics: () -> Unit,
            reloadBlur: (() -> Unit) -> Unit,
            updateLayout: () -> Unit,
            updateGreeting: () -> Unit,
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
            val popupAdapter = SettingsAdapter()
            val updateLock = ReentrantLock()

            val popup = HomeLongPressPopup {
                blurBG.drawable = acrylicBlur?.smoothBlurDrawable
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
                        reloadItemGraphics = reloadItemGraphics,
                        reloadBlur = {
                            reloadBlur {
                                cardView.post { update() }
                            }
                        },
                        updateLayout = {
                            parent.post(updateLayout)
                        },
                        updateGreeting = {
                            parent.post(updateGreeting)
                        },
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
            reloadItemGraphics: () -> Unit,
            reloadBlur: () -> Unit,
            updateLayout: () -> Unit,
            updateGreeting: () -> Unit,
        ): List<SettingsItem<*>> {
            return listOfNotNull(
                SettingsItem(
                    context.getString(R.string.app_name),
                    BuildConfig.VERSION_NAME,
                    icon = context.getDrawable(R.mipmap.ic_launcher),
                    isTitle = true,
                ),
                SettingsItem(context.getString(R.string.general), isTitle = true),
                SettingsItem(
                    context.getString(R.string.color_theme_gen),
                    description = context.resources.getStringArray(R.array.color_theme_gens)[settings.colorTheme],
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_color_dropper),
                ) {
                    AlertDialog.Builder(context)
                        .setItems(
                            context.resources.getStringArray(R.array.color_theme_gens).copyOf(context.resources.getInteger(R.integer.color_theme_gens_available)),
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
                SettingsItem(
                    context.getString(R.string.color_theme_day_night),
                    description = context.resources.getStringArray(R.array.color_theme_day_night)[settings.colorThemeDayNight.ordinal],
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_lightness),
                ) {
                    AlertDialog.Builder(context)
                        .setItems(
                            R.array.color_theme_day_night,
                        ) { d, i ->
                            settings.edit(context) {
                                setColorThemeDayNight(context.resources.getStringArray(R.array.color_theme_day_night_data)[i].toInt())
                                updateColorTheme()
                            }
                            d.dismiss()
                        }
                        .show()
                },
                SettingsItem(
                    context.getString(R.string.greeting),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_home),
                    value = settings.getDefaultGreeting(context),
                    onValueChange = { _, value ->
                        settings.edit(context) {
                            setDefaultGreeting(value)
                            updateGreeting()
                        }
                    }
                ),
                SettingsItem(
                    context.getString(R.string.blur),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_shapes),
                    value = settings.doBlur,
                    states = 2,
                    onValueChange = { _, value ->
                        settings.edit(context) {
                            doBlur = value
                            reloadBlur()
                        }
                    }
                ),
                SettingsItem(context.getString(R.string.layout), isTitle = true),
                SettingsItem(
                    context.getString(R.string.show_app_suggestions),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_visible),
                    value = settings.doSuggestionStrip,
                    states = 2,
                    onValueChange = { _, value ->
                        settings.edit(context) {
                            doSuggestionStrip = value
                            updateLayout()
                        }
                    }
                ),
                SettingsItem(
                    context.getString(R.string.suggestion_count),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_slab),
                    value = settings.suggestionColumnCount - 2,
                    states = 5,
                    onValueChange = { _, value ->
                        settings.edit(context) {
                            suggestionColumnCount = value + 2
                            updateLayout()
                        }
                    }
                ),
                SettingsItem(context.getString(R.string.tiles), isTitle = true),
                SettingsItem(
                    context.getString(R.string.icon_packs),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_shapes),
                ) {
                    context.startActivity(Intent(context, IconPackPickerActivity::class.java))
                },
                SettingsItem(
                    context.getString(R.string.monochrome_icons),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_color_dropper),
                    value = settings.monochromatism,
                    states = 2,
                    onValueChange = { _, value ->
                        settings.edit(context) {
                            monochromatism = value
                            reloadItemGraphics()
                        }
                    }
                ),
                SettingsItem(context.getString(R.string.dash), isTitle = true),
                SettingsItem(
                    context.getString(R.string.flag),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_slab),
                ) {
                    context.startActivity(Intent(context, FlagSettingsActivity::class.java))
                },
                SettingsItem(context.getString(R.string.all_apps), isTitle = true),
                SettingsItem(
                    context.getString(R.string.auto_show_keyboard),
                    description = context.getString(R.string.auto_show_keyboard_explanation),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_keyboard),
                    value = settings.doAutoKeyboardInAllApps,
                    states = 2,
                    onValueChange = { _, value ->
                        settings.edit(context) {
                            doAutoKeyboardInAllApps = value
                        }
                    }
                ),
                SettingsItem(context.getString(R.string.other), isTitle = true),
                SettingsItem(
                    context.getString(R.string.choose_default_launcher),
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_home),
                ) {
                    context.chooseDefaultLauncher()
                }
            )
        }
    }
}