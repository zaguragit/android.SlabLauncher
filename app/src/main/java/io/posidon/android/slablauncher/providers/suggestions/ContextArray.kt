package io.posidon.android.slablauncher.providers.suggestions

import kotlin.math.abs
import kotlin.math.min

@JvmInline
value class ContextArray(
    val data: FloatArray
) {

    constructor() : this(FloatArray(CONTEXT_DATA_SIZE))
    constructor(list: List<Float>) : this(list.toFloatArray())

    inline var hour: Float
        get() = data[CONTEXT_DATA_HOUR_OF_DAY] * 12f
        set(value) = data.set(CONTEXT_DATA_HOUR_OF_DAY, value / 12f)

    inline var battery: Float
        get() = data[CONTEXT_DATA_BATTERY] * 100f
        set(value) = data.set(CONTEXT_DATA_BATTERY, value / 100f)

    inline var hasHeadset: Boolean
        get() = data[CONTEXT_DATA_HAS_HEADSET] != 0f
        set(value) = data.set(CONTEXT_DATA_HAS_HEADSET, if (value) 1.2f else 0f)

    inline var hasWifi: Boolean
        get() = data[CONTEXT_DATA_HAS_WIFI] != 0f
        set(value) = data.set(CONTEXT_DATA_HAS_WIFI, if (value) 1.5f else 0f)

    inline var isPluggedIn: Boolean
        get() = data[CONTEXT_DATA_IS_PLUGGED_IN] != 0f
        set(value) = data.set(CONTEXT_DATA_IS_PLUGGED_IN, if (value) 1f else 0f)

    inline var isWeekend: Boolean
        get() = data[CONTEXT_DATA_IS_WEEKEND] != 0f
        set(value) = data.set(CONTEXT_DATA_IS_WEEKEND, if (value) 2f else 0f)

    companion object {
        const val CONTEXT_DATA_HOUR_OF_DAY = 0
        const val CONTEXT_DATA_BATTERY = 1
        const val CONTEXT_DATA_HAS_HEADSET = 2
        const val CONTEXT_DATA_HAS_WIFI = 3
        const val CONTEXT_DATA_IS_PLUGGED_IN = 4
        const val CONTEXT_DATA_IS_WEEKEND = 5
        const val CONTEXT_DATA_SIZE = 6

        fun differentiator(i: Int, a: Float, b: Float): Float {
            val base = abs(a - b)
            return when (i) {
                CONTEXT_DATA_HOUR_OF_DAY -> {
                    val t = min(base, 2 - base)
                    val it = 1 - t
                    1 - (it * it * it)
                }
                CONTEXT_DATA_BATTERY -> (base * base)
                    .let { 1 - it }
                    .let { it * it }
                    .let { 1 - it }
                else -> base * base
            }
        }
    }
}

inline fun ContextArray.toList() = data.toList()