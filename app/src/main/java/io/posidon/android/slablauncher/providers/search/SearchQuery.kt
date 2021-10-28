package io.posidon.android.slablauncher.providers.search

@JvmInline
value class SearchQuery(
    val text: CharSequence
) {
    inline val length: Int get() = text.length

    override fun toString() = text.toString()

    companion object {
        val EMPTY = SearchQuery(object : CharSequence {
            override val length = 0
            override fun get(index: Int) = throw IndexOutOfBoundsException()
            override fun subSequence(startIndex: Int, endIndex: Int) = throw Exception()
        })
    }
}
