package utils

import com.fleeksoft.ksoup.Ksoup

/**
 * Strips HTML to plain text (same approach as Onliner detail: Ksoup `.text()`).
 */
fun String?.stripHtmlToPlainText(): String? {
    if (this.isNullOrBlank()) return this
    val plain = Ksoup.parse(this).text()
        .replace('\u00a0', ' ')
        .trim()
    return plain.ifBlank { null }
}
