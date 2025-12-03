package io.flatzen.commoncomponents.extensions

fun Any?.toNullableString(): String? = if(this == null) null else this.toString()

fun String?.substringBeforeAnyDelimiter(): String? {
    if (this.isNullOrEmpty()) return null

    val delimiters = listOf("-", "_", "(", ")", ":", "http")

    var minIndex = this.length
    var found = false

    for (delimiter in delimiters) {
        val index = this.indexOf(delimiter)
        if (index != -1 && index < minIndex) {
            minIndex = index
            found = true
        }
    }
    return if (found) this.substring(0, minIndex) else this
}