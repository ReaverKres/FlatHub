# Navigation destinations
-keepnames class io.flatzen.ListScreenDestination
-keepnames class io.flatzen.FavoritesScreenDestination
-keepnames class io.flatzen.SettingsScreenDestination
-keepnames class io.flatzen.MapScreenDestination
-keepnames class io.flatzen.NotificationScreenDestination

# Kotlin / reflection metadata (needed by ktor TypeInfo / kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation

# kotlinx.serialization (official-style rules)
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class **$$serializer { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn kotlinx.serialization.internal.**

# API JSON models + Ktorfit generated APIs
-keep class server_response.** { *; }
-keep class server_request.** { *; }
-keep class api.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class entities.** { *; }
-keep @androidx.room.Dao interface database.** { *; }

# Ktor / Ktorfit
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class de.jensklingenberg.ktorfit.** { *; }

# Appodeal
-keep class com.appodeal.** { *; }
-dontwarn com.appodeal.**

# Google Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
