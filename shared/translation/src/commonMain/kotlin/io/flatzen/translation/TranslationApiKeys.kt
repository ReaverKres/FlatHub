package io.flatzen.translation

/**
 * Paste keys here. Leave Azure blank until you create an F0 resource —
 * failover will use only configured providers.
 */
object TranslationApiKeys {
    const val AZURE_KEY = ""
    const val AZURE_REGION = "westeurope"
    const val DEEPL_KEY = "d47d3531-612e-440f-a4ef-73674769da4d:fx"

    val isAzureConfigured: Boolean
        get() = AZURE_KEY.isNotBlank() && !AZURE_KEY.startsWith("YOUR_")

    val isDeepLConfigured: Boolean
        get() = DEEPL_KEY.isNotBlank() && !DEEPL_KEY.startsWith("YOUR_")
}
