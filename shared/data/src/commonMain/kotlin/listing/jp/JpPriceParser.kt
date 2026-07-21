package listing.jp

/** Parse Japanese rent/price labels like `7.2万円`, `8000円`, `550万円` into yen. */
internal fun parseYenLabel(label: String?): Double? {
    if (label.isNullOrBlank() || label.contains("－") || label == "なし") return null
    val cleaned = label
        .replace(",", "")
        .replace("円", "")
        .replace(Regex("<[^>]+>"), "")
        .trim()
    Regex("""(\d+)億(\d+(?:\.\d+)?)?万""").find(cleaned)?.let { match ->
        val oku = match.groupValues[1].toDoubleOrNull() ?: return@let null
        val man = match.groupValues[2].toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
        return oku * 100_000_000 + man * 10_000
    }
    Regex("""(\d+)億""").find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
        return it * 100_000_000
    }
    Regex("""([\d.]+)\s*万""").find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
        return it * 10_000
    }
    return cleaned.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
}

/** Extract m² from labels like `28.25m<sup>2</sup>`. */
internal fun parseAreaSqm(label: String?): Double? {
    if (label.isNullOrBlank()) return null
    val plain = label.replace(Regex("<[^>]+>"), "")
    Regex("""([\d.]+)\s*m""").find(plain)?.groupValues?.get(1)?.toDoubleOrNull()
        ?.let { return it }
    return null
}

/** Stable numeric id from external listing id string. */
internal fun stableAdId(externalId: String): Long =
    externalId.hashCode().toLong() and 0x7FFF_FFFFL

/** Rooms count from Japanese layout label (間取り), e.g. `1LDK`, `2DK`, `ワンルーム`. */
internal fun parseMadoriRooms(madori: String?): Int? {
    if (madori.isNullOrBlank()) return null
    if (madori.contains("ワンルーム")) return 1
    return Regex("""(\d+)""").find(madori)?.groupValues?.get(1)?.toIntOrNull()
}

internal fun isMadoriStudio(madori: String?): Boolean {
    if (madori.isNullOrBlank()) return false
    return madori.contains("ワンルーム") || Regex("""^1R$""").matches(madori.trim())
}
