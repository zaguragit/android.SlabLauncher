package io.posidon.android.slablauncher.util.storage

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

private val settingsFileLock = ReentrantLock()

class Settings {
    companion object {
        const val SAVE_FILE = "settings"
    }

    private abstract class Single <V> (
        val value: V
    ) {
        abstract fun toFloat(): Float
        abstract fun toInt(): Int
        abstract fun toBool(): Boolean
        override fun toString() = value.toString()
    }

    private class SingleInt(value: Int) : Single<Int>(value) {
        override fun toFloat(): Float = value.toFloat()
        override fun toInt(): Int = value
        override fun toBool(): Boolean = value != 0
    }

    private class SingleFloat(value: Float) : Single<Float>(value) {
        override fun toFloat(): Float = value
        override fun toInt(): Int = value.toInt()
        override fun toBool(): Boolean = value != 0f
    }

    private class SingleBool(value: Boolean) : Single<Boolean>(value) {
        override fun toFloat(): Float = if (value) 1f else 0f
        override fun toInt(): Int = if (value) 1 else 0
        override fun toBool(): Boolean = value
    }

    private class SingleString(value: String) : Single<String>(value) {
        override fun toFloat(): Float = value.toFloat()
        override fun toInt(): Int = value.toInt()
        override fun toBool(): Boolean = value.toBoolean()
        override fun toString() = value
    }

    private val singles: HashMap<String, Single<*>> = HashMap()

    private val lists: HashMap<String, Array<String>> = HashMap()

    private var isInitialized: Boolean = false

    private val editor = SettingsEditor(this)

    class SettingsEditor(val settings: Settings) {

        operator fun set(key: String, value: Int) {
            settings.singles[key] = SingleInt(value)
        }

        operator fun set(key: String, value: Float) {
            settings.singles[key] = SingleFloat(value)
        }

        operator fun set(key: String, value: Boolean) {
            settings.singles[key] = SingleBool(value)
        }

        operator fun set(key: String, value: String?) {
            if (value == null) settings.singles.keys.remove(key)
            else settings.singles[key] = SingleString(value)
        }

        operator fun set(key: String, value: Array<String>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = value
        }

        fun setStrings(key: String, value: Array<String>?) {
            if (value == null) settings.lists.keys.remove(key)
            else settings.lists[key] = value
        }

        @JvmName("set1")
        inline infix fun String.set(value: Int) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Float) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Boolean) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: String?) = set(this, value)

        @JvmName("set1")
        inline infix fun String.set(value: Array<String>?) = set(this, value)
    }

    fun edit(context: Context, block: SettingsEditor.() -> Unit) {
        thread(name = "Settings edit thread") {
            settingsFileLock.withLock {
                block(editor)
                PrivateStorage.write(context, SAVE_FILE, ::serializeData)
            }
        }
    }

    fun saveNow(context: Context) = settingsFileLock.withLock {
        PrivateStorage.write(context, SAVE_FILE, ::serializeData)
    }

    inline operator fun get(key: String, default: Int): Int = getInt(key) ?: default
    inline operator fun get(key: String, default: Float): Float = getFloat(key) ?: default
    inline operator fun get(key: String, default: Boolean): Boolean = getBoolean(key) ?: default
    inline operator fun get(key: String, default: String): String = getString(key) ?: default

    inline fun getIntOr(key: String, default: () -> Int): Int = getInt(key) ?: default()
    inline fun getFloatOr(key: String, default: () -> Float): Float = getFloat(key) ?: default()
    inline fun getBoolOr(key: String, default: () -> Boolean): Boolean = getBoolean(key) ?: default()
    inline fun getStringOr(key: String, default: () -> String): String = getString(key) ?: default()


    fun getInt(key: String): Int? {
        return singles[key]?.toInt()
    }

    fun getFloat(key: String): Float? {
        return singles[key]?.toFloat()
    }

    fun getBoolean(key: String): Boolean? {
        return singles[key]?.toBool()
    }

    fun getString(key: String): String? {
        return singles[key]?.toString()
    }

    fun getStrings(key: String): Array<String>? {
        return lists[key]
    }

    fun init(context: Context) {
        settingsFileLock.withLock {
            if (!isInitialized) {
                PrivateStorage.read(context, SAVE_FILE, ::initializeData)
                isInitialized = true
            }
        }
    }

    private fun initializeData(it: ObjectInputStream): Boolean {
        val root = JSONObject(it.readUTF())
        return fill(singles, root.getJSONArray("singles")) {
            val string = getString(it)
            string.toBooleanStrictOrNull()?.let(::SingleBool)
                ?: string.toIntOrNull()?.let(::SingleInt)
                ?: string.toFloatOrNull()?.let(::SingleFloat)
                ?: string.let(::SingleString)
        } or
        fill(lists, root.getJSONArray("list")) {
            val json = getJSONArray(it)
            val list = ArrayList<String>()
            var i = 0
            while (i < json.length()) {
                list.add(json.getString(i))
                i++
            }
            list.toTypedArray()
        }
    }

    private fun serializeData(out: ObjectOutputStream) {
        val json = JSONObject()
        json.put("singles", toJson(singles, Single<*>::toString))
        json.put("list", toJson(lists) {
            JSONArray().apply { it.forEach(::put) }
        })
        out.writeUTF(json.toString())
    }

    private fun <T> toJson(
        map: HashMap<String, T>,
        mapper: (T) -> Any? = { it }
    ): JSONArray {
        return settingsFileLock.withLock {
            JSONArray().apply {
                map.forEach { entry ->
                    put(JSONArray().apply {
                        put(entry.key)
                        put(mapper(entry.value))
                    })
                }
            }
        }
    }

    private fun <T> fill(
        map: HashMap<String, T>,
        entries: JSONArray,
        mapper: JSONArray.(Int) -> T
    ): Boolean {
        var hasUpdated = false
        var i = 0
        while (i < entries.length()) {
            val entry = entries.getJSONArray(i)
            val key = entry.getString(0)
            val value = mapper(entry, 1)
            if (map[key] != value) hasUpdated = true
            map[key] = value
            i++
        }
        return hasUpdated
    }

    fun saveBackup(context: Context) {
        settingsFileLock.withLock {
            ExternalStorage.writeDataOutsideScope(
                context,
                "${
                    SimpleDateFormat(
                        "'home_'MMMd-HHmmss",
                        Locale.getDefault()
                    ).format(Date())
                }.slabbackup",
                true, ::serializeData
            )
        }
    }

    fun restoreFromBackup(context: Context, uri: Uri) {
        settingsFileLock.withLock {
            ExternalStorage.read(context, uri, ::initializeData)
        }
    }

    fun clearAllInformation() {
        settingsFileLock.withLock {
            singles.clear()
            lists.clear()
        }
    }

    /**
     * @return whether there was a change
     */
    fun reload(context: Context): Boolean {
        return settingsFileLock.withLock {
            PrivateStorage.read(context, SAVE_FILE, ::initializeData) != false
        }
    }
}
