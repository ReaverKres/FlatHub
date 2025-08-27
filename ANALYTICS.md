# Analytics Integration

This document describes the AppMetrica analytics integration in the FlatZen KMP application.

## Overview

The application uses AppMetrica SDK for analytics tracking on Android platform only. The integration follows the Kotlin Multiplatform architecture with dependency injection through Koin.

## Configuration

### 1. API Key Setup

The AppMetrica API key is configured through BuildConfig:

```kotlin
// In composeApp/build.gradle.kts
buildConfigField("String", "APPMETRICA_API_KEY", "\"YOUR_API_KEY_HERE\"")
```

Replace `YOUR_API_KEY_HERE` with your actual AppMetrica API key.

### 2. Dependencies

AppMetrica dependency is added to the following modules:
- `composeApp` (Android target)
- `shared:commoncomponents` (Android target)

Unwanted AppMetrica modules are excluded to reduce APK size:
- analytics-ad-revenue variants
- analytics-apphud
- analytics-appsetid
- analytics-identifiers
- analytics-location
- analytics-ndkcrashes
- analytics-screenshot

## Architecture

### Interface

```kotlin
interface AnalyticsManagerInterface {
    suspend fun registerEvent(event: AnalyticsEvent)
}

data class AnalyticsEvent(
    val eventName: String,
    val parameters: Map<String, Any> = emptyMap()
)
```

### Platform Implementations

- **Android**: Uses AppMetrica SDK for real analytics tracking
- **iOS**: Stub implementation (no-op) that logs events to console

### Dependency Injection

Analytics manager is registered in Koin DI container and injected into:
- Application class for app launch tracking
- ViewModels for user action tracking
- Screens for screen view tracking

## Tracked Events

### 1. App Launch
- **Event**: `app_launch`
- **Location**: `FlatZenApp.onCreate()`
- **Parameters**:
  - `app_version`: Application version name
  - `version_code`: Application version code

### 2. Screen Views
- **Event**: `screen_view`
- **Parameters**:
  - `screen_name`: Name of the viewed screen
  - `timestamp`: Screen view timestamp
  - Additional screen-specific parameters

Tracked screens:
- `list_screen` - Main apartments list
- `detail_screen` - Apartment details with platform and object_id
- `filter_screen` - Filter modal screen

### 3. User Actions

#### Search Flats Action
- **Event**: `search_flats`
- **Location**: `FlatSearchViewModel.handleIntent()`
- **Trigger**: When user performs search/refresh/load more
- **Parameters**:
  - `is_load_more`: Boolean indicating if this is a load more action
  - `is_refreshing`: Boolean indicating if this is a refresh action
  - `page`: Current page number
  - `has_network`: Network availability status

## Usage Examples

### Manual Event Tracking

```kotlin
class MyViewModel(
    private val analyticsManager: AnalyticsManagerInterface
) {
    suspend fun trackCustomEvent() {
        analyticsManager.registerEvent(
            AnalyticsEvent(
                eventName = "custom_action",
                parameters = mapOf(
                    "action_type" to "button_click",
                    "screen" to "settings"
                )
            )
        )
    }
}
```

### Screen View Tracking

```kotlin
@Composable
fun MyScreen() {
    TrackScreenView(
        screenName = "my_screen",
        parameters = mapOf(
            "feature" to "premium"
        )
    )
    
    // Screen content...
}
```

## Testing

The analytics integration includes:
1. Unit tests for event creation and validation
2. Mock implementations for testing
3. Error handling to prevent crashes

Run tests with:
```bash
./gradlew :shared:commoncomponents:testDebugUnitTest
```

## Troubleshooting

### Common Issues

1. **Events not appearing in AppMetrica dashboard**
   - Check API key configuration
   - Verify network connectivity
   - Enable debug logging in development

2. **Compilation errors**
   - Ensure all dependencies are properly configured
   - Check BuildConfig field is correctly set
   - Verify module dependencies in DI configuration

3. **Analytics manager injection fails**
   - Verify analytics module is included in Koin initialization
   - Check dependency injection order
   - Ensure expect/actual implementations exist for both platforms

### Debug Mode

In debug builds, AppMetrica is configured with logging enabled:

```kotlin
val config = AppMetricaConfig.newConfigBuilder(apiKey)
    .withLogs() // Enables debug logging
    .build()
```

## Security Considerations

- API key is stored in BuildConfig (not in source code)
- No sensitive user data is tracked
- All analytics events are sanitized before sending
- Error handling prevents crashes from analytics failures

## Future Improvements

1. Add iOS AppMetrica SDK integration
2. Implement custom event builders for common patterns
3. Add analytics event validation
4. Create analytics dashboard for monitoring
5. Implement A/B testing capabilities