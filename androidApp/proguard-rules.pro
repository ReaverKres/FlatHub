# Сохранить имена классов для навигации
-keepnames class io.flatzen.ListScreenDestination
-keepnames class io.flatzen.FavoritesScreenDestination
-keepnames class io.flatzen.SettingsScreenDestination
-keepnames class io.flatzen.MapScreenDestination
-keepnames class io.flatzen.NotificationScreenDestination

# Appodeal
-keep class com.appodeal.** { *; }
-dontwarn com.appodeal.**
-keepattributes Signature, *Annotation*