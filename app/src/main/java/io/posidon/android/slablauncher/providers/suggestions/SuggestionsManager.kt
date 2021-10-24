package io.posidon.android.slablauncher.providers.suggestions

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.util.storage.Settings
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

object SuggestionsManager {

    private const val CONTEXT_DATA_HOUR_OF_DAY = 0
    private const val CONTEXT_DATA_BATTERY = 1
    private const val CONTEXT_DATA_HAS_HEADSET = 2
    private const val CONTEXT_DATA_HAS_WIFI = 3
    private const val CONTEXT_DATA_IS_PLUGGED_IN = 4
    private const val CONTEXT_DATA_IS_WEEKEND = 5
    private const val CONTEXT_DATA_SIZE = 6
    private const val MAX_CONTEXT_COUNT = 6

    private var contextMap = ContextMap<LauncherItem>(CONTEXT_DATA_SIZE, ::differentiator)
    private var suggestions = emptyList<LauncherItem>()
    private val contextLock = ReentrantLock()

    fun getSuggestions(): List<LauncherItem> = suggestions

    fun getNonPinnedSuggestions(pinnedItems: List<LauncherItem>): List<LauncherItem> = suggestions - pinnedItems

    fun onItemOpened(context: Context, item: LauncherItem) {
        thread(isDaemon = false, name = "SuggestionManager: saving opening context") {
            contextLock.withLock {
                saveItemOpenContext(context, item)
            }
            updateSuggestions(context)
        }
    }

    fun onAppsLoaded(
        appManager: LauncherContext.AppManager,
        context: Context,
        settings: Settings
    ) {
        loadFromStorage(settings, context, appManager)
        updateSuggestions(context)
    }

    fun onResume(context: Context, onEnd: () -> Unit) {
        thread (isDaemon = true, name = "SuggestionManager: onResume") {
            updateSuggestions(context)
            onEnd()
        }
    }

    fun onPause(settings: Settings, context: Context) {
        saveToStorage(settings, context)
    }

    private fun differentiator(i: Int, a: Float, b: Float): Float {
        val base = abs(a - b)
        return if (i == CONTEXT_DATA_HOUR_OF_DAY)
            min(base, 24 - base)
        else base
    }

    private fun saveItemOpenContext(context: Context, item: LauncherItem) {
        val data = FloatArray(CONTEXT_DATA_SIZE)
        getCurrentContext(context, data)
        contextMap.push(item, data, MAX_CONTEXT_COUNT)
    }

    private fun updateSuggestions(context: Context) {
        val stats = if (checkUsageAccessPermission(context)) {
            val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

            val c = Calendar.getInstance()
            c.add(Calendar.HOUR_OF_DAY, -1)

            HashMap<String, UsageStats>().apply {
                usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    c.timeInMillis,
                    System.currentTimeMillis()
                ).forEach {
                    this[it.packageName] = it
                }
            }
        } else null

        val currentData = FloatArray(CONTEXT_DATA_SIZE)
        getCurrentContext(context, currentData)

        contextLock.withLock {
            this.suggestions = run {
                val sortedEntries = contextMap.entries.sortedBy { (item, data) ->
                    val timeF = if (stats != null) {
                        val lastUse = stats[(item as App).packageName]?.lastTimeUsed
                        if (lastUse == null) 1f else run {
                            val c = Calendar.getInstance()
                            c.timeInMillis = System.currentTimeMillis() - lastUse
                            (c[Calendar.MINUTE] / 20f).coerceAtMost(1f).pow(2)
                        }
                    } else 0f

                    contextMap.calculateDistance(currentData, data) + timeF
                }
                sortedEntries.map { it.key }
            }
        }
    }

    private fun getCurrentContext(context: Context, out: FloatArray) {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val audioManager = context.getSystemService(AudioManager::class.java)
        val rightNow = Calendar.getInstance()

        val batteryLevel = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isPluggedIn = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        val currentHourIn24Format = rightNow[Calendar.HOUR_OF_DAY] + rightNow[Calendar.MINUTE] / 60f + rightNow[Calendar.SECOND] / 60f / 60f
        val isWeekend = rightNow[Calendar.DAY_OF_WEEK].let {
            it == Calendar.SATURDAY || it == Calendar.SUNDAY
        }
        val isHeadSetConnected = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).isNotEmpty()
        val connManager = context.getSystemService(WifiManager::class.java)
        val isWifiOn = connManager!!.isWifiEnabled

        out[CONTEXT_DATA_HOUR_OF_DAY] = currentHourIn24Format / 12f
        out[CONTEXT_DATA_BATTERY] = batteryLevel / 100f
        out[CONTEXT_DATA_HAS_HEADSET] = if (isHeadSetConnected) 1f else 0f
        out[CONTEXT_DATA_HAS_WIFI] = if (isWifiOn) 1f else 0f
        out[CONTEXT_DATA_IS_PLUGGED_IN] = if (isPluggedIn) 1f else 0f
        out[CONTEXT_DATA_IS_WEEKEND] = if (isWeekend) 1f else 0f
    }

    private fun loadFromStorage(
        settings: Settings,
        context: Context,
        appManager: LauncherContext.AppManager
    ) {
        settings.getStrings("stats:app_opening_contexts")?.let {
            val contextMap = ContextMap<LauncherItem>(CONTEXT_DATA_SIZE, ::differentiator)
            it.forEach { app ->
                appManager.parseLauncherItem(app)?.let { item ->
                    settings.getStrings("stats:app_opening_context:$app")
                        ?.map(String::toFloat)?.let { floats ->
                            contextMap[item] = floats.chunked(CONTEXT_DATA_SIZE).map(List<Float>::toFloatArray)
                        }
                } ?: settings.edit(context) {
                    setStrings("stats:app_opening_context:$app", null)
                }
            }
            this.contextMap = contextMap
        }
    }

    private fun saveToStorage(settings: Settings, context: Context) {
        settings.edit(context) {
            "stats:app_opening_contexts" set contextMap
                .map { it.key.toString() }
                .toTypedArray()
            contextMap.forEach { (packageName, data) ->
                "stats:app_opening_context:$packageName" set data
                    .flatMap(FloatArray::toList)
                    .map(Float::toString)
                    .toTypedArray()
            }
        }
    }

    fun checkUsageAccessPermission(context: Context): Boolean {
        val aom = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aom.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.packageName
            )
        } else aom.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )

        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }
}