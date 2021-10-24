package io.posidon.android.slablauncher.providers.suggestions

class ContextMap<T>(
    contextDataSize: Int,
    val differentiator: (Int, Float, Float) -> Float
) : Map<T, List<FloatArray>> {

    private var contexts = HashMap<T, List<FloatArray>>()

    operator fun set(item: T, value: List<FloatArray>) {
        contexts[item] = value
    }

    override val entries: Set<Map.Entry<T, List<FloatArray>>>
        get() = contexts.entries
    override val keys: Set<T>
        get() = contexts.keys
    override val size: Int
        get() = contexts.size
    override val values: Collection<List<FloatArray>>
        get() = contexts.values

    override fun containsKey(key: T) = contexts.containsKey(key)
    override fun containsValue(value: List<FloatArray>) = contexts.containsValue(value)
    override fun get(key: T) = contexts[key]
    override fun isEmpty() = contexts.isEmpty()

    private val lengthBuffer = FloatArray(contextDataSize)
    fun calculateDistance(currentContext: FloatArray, multipleContexts: List<FloatArray>): Float {
        return multipleContexts.map { d ->
            calculateDistance(currentContext, d)
        }.reduce(Float::times)
    }
    fun calculateDistance(a: FloatArray, b: FloatArray): Float {
        a.forEachIndexed { i, fl ->
            lengthBuffer[i] = differentiator(i, fl, b[i])
            lengthBuffer[i] *= lengthBuffer[i]
        }
        return lengthBuffer.sum()
    }

    fun trimContextListIfTooBig(list: List<FloatArray>, maxContexts: Int): List<FloatArray> {
        val s = list.size
        return if (list.size > maxContexts) {
            val matches = list.mapIndexedTo(ArrayList()) { ai, a ->
                a to list.mapIndexedTo(ArrayList()) { i, b ->
                    i to if (i == ai) 0f else calculateDistance(a, b)
                }.also {
                    it.removeAt(ai)
                    it.sortBy { (_, c) -> c }
                }[0]
            }
            matches.sortBy { (array, closest) ->
                closest.second
            }
            var amountOfFiledMixAttempts = 0
            var iOffset = 0
            while (matches.size > maxContexts || amountOfFiledMixAttempts > matches.size) {
                val match = matches.removeAt(0)
                iOffset++
                val (matchData, matchLoc) = match
                if (matchLoc.first == -1) {
                    amountOfFiledMixAttempts++
                    matches.add(match)
                    continue
                }
                val trueI = matchLoc.first - iOffset
                val (arr, loc) = matches[trueI]
                arr.forEachIndexed { i, f ->
                    arr[i] = (f + matchData[i]) / 2
                }
                matches[trueI] = arr to loc.copy(first = -1)
            }
            println("context map trim -> initial size: $s, new size: ${matches.size}")
            matches.map { it.first }
        } else list
    }

    fun push(item: T, data: FloatArray, maxContexts: Int) {
        contexts[item] = contexts[item]?.plus(data)?.let { trimContextListIfTooBig(it, maxContexts) } ?: listOf(data)
    }
}