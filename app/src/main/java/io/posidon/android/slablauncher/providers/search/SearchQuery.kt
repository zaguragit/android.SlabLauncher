package io.posidon.android.slablauncher.providers.search

@JvmInline
value class SearchQuery(
    val text: CharSequence
) {
    inline val length: Int get() = text.length

    override fun toString() = text.toString()
}
