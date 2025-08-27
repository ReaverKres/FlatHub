package io.flatzen.commoncomponents.config

/**
 * Application configuration object that provides access to build-time configuration values.
 * This object serves as a central place for accessing configuration parameters
 * that are set during the build process.
 */
object Config {
    /**
     * AppMetrica API key for analytics integration.
     * This value is injected during build time through BuildConfig.
     */
    var appMetricaApiKey: String = ""
        private set

    /**
     * Initialize configuration with build-time values.
     * This method should be called during application initialization.
     * 
     * @param apiKey AppMetrica API key from BuildConfig
     */
    fun addAppMetricaApiKey(apiKey: String) {
        appMetricaApiKey = apiKey
    }
}