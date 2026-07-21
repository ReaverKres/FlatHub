package io.flatzen.commoncomponents.theme

import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.marketCountry

/**
 * Resolves BCP-47 language tag for listing translation.
 * Preferred explicit target > app language > system locale if supported > English.
 */
fun resolveTranslationTargetTag(
    appLanguage: AppLanguage,
    preferredTarget: AppLanguage?,
    systemLocaleTag: String,
): String {
    preferredTarget?.tag?.let { return it }
    appLanguage.tag?.let { return it }
    val systemLang = systemLocaleTag
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
    val supported = AppLanguage.entries.mapNotNull { it.tag }.toSet()
    return if (systemLang in supported) systemLang else "en"
}

/**
 * Typical listing content language for a market (not UI localization).
 * Used to skip auto-translate when source ≈ target.
 */
fun CountryCode.marketContentLanguageTag(): String = when (this) {
    CountryCode.BY, CountryCode.KZ -> "ru"
    CountryCode.PL -> "pl"
    CountryCode.GE -> "ka"
    CountryCode.ES -> "es"
    CountryCode.DE, CountryCode.CH -> "de"
    CountryCode.TR -> "tr"
    CountryCode.AE -> "en"
    CountryCode.TH -> "th"
    CountryCode.US -> "en"
    CountryCode.KR -> "ko"
    CountryCode.JP -> "ja"
}

fun FlatPlatform.marketContentLanguageTag(): String =
    marketCountry().marketContentLanguageTag()

/** Auto-translate only when market language differs from the user's target. */
fun shouldAutoTranslateListing(
    platform: FlatPlatform,
    targetLangTag: String,
): Boolean = platform.marketContentLanguageTag() != targetLangTag.lowercase()
