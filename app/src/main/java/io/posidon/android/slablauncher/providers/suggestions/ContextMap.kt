package io.posidon.android.slablauncher.providers.suggestions

class ContextMap<T>(
    contextDataSize: Int,
    val differentiator: (Int, Float, Float) -> Float
) : Map<T, List<ContextArray>> {

    private var contexts = HashMap<T, List<ContextArray>>()

    operator fun set(item: T, value: List<ContextArray>) {
        contexts[item] = value
    }

    override val entries: Set<Map.Entry<T, List<ContextArray>>>
        get() = contexts.entries
    override val keys: Set<T>
        get() = contexts.keys
    override val size: Int
        get() = contexts.size
    override val values: Collection<List<ContextArray>>
        get() = contexts.values

    override fun containsKey(key: T) = contexts.containsKey(key)
    override fun containsValue(value: List<ContextArray>) = contexts.containsValue(value)
    override fun get(key: T) = contexts[key]
    override fun isEmpty() = contexts.isEmpty()

    fun calculateDistance(currentContext: ContextArray, multipleContexts: List<ContextArray>): Float {
        return multipleContexts.map { d ->
            calculateDistance(currentContext, d)
        }.reduce(Float::times)
    }
    private val lengthBuffer = FloatArray(contextDataSize)
    fun calculateDistance(a: ContextArray, b: ContextArray): Float {
        a.data.forEachIndexed { i, fl ->
            lengthBuffer[i] = differentiator(i, fl, b.data[i])
        }
        return lengthBuffer.sum()
    }

    fun trimContextListIfTooBig(list: List<ContextArray>, maxContexts: Int): List<ContextArray> {
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
                arr.data.forEachIndexed { i, f ->
                    arr.data[i] = (f + matchData.data[i]) / 2
                }
                matches[trueI] = arr to loc.copy(first = -1)
            }
            println("context map trim -> initial size: $s, new size: ${matches.size}")
            matches.map { it.first }
        } else list
    }

    fun push(item: T, data: ContextArray, maxContexts: Int) {
        contexts[item] = contexts[item]?.plus(data)?.let { trimContextListIfTooBig(it, maxContexts) } ?: listOf(data)
    }
}