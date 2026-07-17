rootProject.name = "FlatZen"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven { url = uri("https://artifactory.appodeal.com/appodeal") }
        maven { url = uri("https://android-sdk.is.com/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://s3.amazonaws.com/smaato-sdk-releases/") }
        maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
        maven {
            url = uri("https://maven.google.com")
            content {
                includeGroup("org.chromium.net")
            }
        }
    }
}

include(":androidApp")
include(":composeApp")
include(":shared")
include(":shared:presentation")
include(":shared:domain")
include(":shared:data")
include(":shared:commoncomponents")
include(":shared:monetization")
include(":shared:analytics")
include(":shared:translation")
