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
import io.posidon.android.slablauncher.BuildConfig
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.util.storage.Settings
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.pow

object SuggestionsManager {

    private const val MAX_CONTEXT_COUNT = 6

    private var contextMap = ContextMap<LauncherItem>(ContextArray.CONTEXT_DATA_SIZE, ContextArray::differentiator)
    private var patternBased = emptyList<LauncherItem>()
    private val contextLock = ReentrantLock()
    private var appsByPackage: List<App> = emptyList()

    fun get(): List<LauncherItem> = patternBased

    fun onItemOpened(context: Context, item: LauncherItem) {
        if (item !is App || item.packageName != BuildConfig.APPLICATION_ID)
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
        suggestionData: Settings,
        appsByPackage: List<App>
    ) {
        this.appsByPackage = appsByPackage
        loadFromStorage(suggestionData, context, appManager)
        updateSuggestions(context)
    }

    fun onResume(context: Context, onEnd: () -> Unit) {
        thread(isDaemon = true, name = "SuggestionManager: onResume") {
            updateSuggestions(context)
            onEnd()
        }
    }

    fun onPause(suggestionData: Settings, context: Context) {
        saveToStorage(suggestionData, context)
    }

    private fun saveItemOpenContext(context: Context, item: LauncherItem) {
        val data = ContextArray()
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

        val currentData = ContextArray()
        getCurrentContext(context, currentData)

        val timeBased = if (stats != null) appsByPackage.sortedBy {
            val lastUse = stats[it.packageName]?.lastTimeUsed
            if (lastUse == null) 1f else run {
                val c = Calendar.getInstance()
                c.timeInMillis = System.currentTimeMillis() - lastUse
                (c[Calendar.MINUTE] / 20f).coerceAtMost(1f).pow(2)
            }
        }.filter { it.packageName != BuildConfig.APPLICATION_ID } else emptyList()

        contextLock.withLock {
            this.patternBased = run {
                val sortedEntries = contextMap.entries.sortedBy { (item, data) ->
                    contextMap.calculateDistance(currentData, data)
                }
                sortedEntries.mapTo(ArrayList()) { it.key }.run {
                    addAll(timeBased)
                    distinct()
                }
            }
        }
    }

    private fun getCurrentContext(context: Context, out: ContextArray) {
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

        out.hour = currentHourIn24Format
        out.battery = batteryLevel.toFloat()
        out.hasHeadset = isHeadSetConnected
        out.hasWifi = isWifiOn
        out.isPluggedIn = isPluggedIn
        out.isWeekend = isWeekend
    }

    private fun loadFromStorage(
        suggestionData: Settings,
        context: Context,
        appManager: LauncherContext.AppManager
    ) {
        suggestionData.getStrings("stats:app_opening_contexts")?.let {
            val contextMap = ContextMap<LauncherItem>(ContextArray.CONTEXT_DATA_SIZE, ContextArray::differentiator)
            it.forEach { app ->
                appManager.tryParseApp(app)?.let { item ->
                    suggestionData.getStrings("stats:app_opening_context:$app")
                        ?.map(String::toFloat)?.let { floats ->
                            contextMap[item] = floats.chunked(ContextArray.CONTEXT_DATA_SIZE).map(::ContextArray)
                        }
                } ?: suggestionData.edit(context) {
                    setStrings("stats:app_opening_context:$app", null)
                }
            }
            this.contextMap = contextMap
        }
    }

    private fun saveToStorage(suggestionData: Settings, context: Context) {
        suggestionData.edit(context) {
            "stats:app_opening_contexts" set contextMap
                .map { it.key.toString() }
                .toTypedArray()
            contextMap.forEach { (packageName, data) ->
                "stats:app_opening_context:$packageName" set data
                    .flatMap(ContextArray::toList)
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