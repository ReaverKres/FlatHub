import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object Keystore {
    //Debug
    const val debug_key_store_password = "android"
    const val debug_key_alias = "androiddebugkey"
    const val debug_key_alias_password = "android"

    //Release
    const val key_store_password = "Denis@24862"
    const val key_alias = "LocusKey"
    const val key_alias_password = "Denis@24862"
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    id("com.google.gms.google-services")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(project(":shared:presentation"))
            export(project(":shared:commoncomponents"))
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.splashScreen)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.appmetrica.analytics)
        }

        commonMain.dependencies {
            api(project(":shared:presentation"))
            api(project(":shared:commoncomponents"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.navigation.compose)
            implementation(libs.compose.multiplatform.backhandler)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.material.icons.core)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.svg)
            implementation(libs.coil.network.ktor)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.mp.maps)
        }
    }
}

android {
    namespace = "io.flatzen"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.flatzen"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        create("release") {
            keyAlias = Keystore.key_alias
            keyPassword = Keystore.key_alias_password
            storeFile = rootProject.file("LocusKeyStore.jks")
            storePassword = Keystore.key_store_password
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
            // Debug build doesn't need obfuscation
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            // Release build with R8 optimization and obfuscation
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    
    configurations.configureEach {
        exclude(group = "io.appmetrica.analytics", module = "analytics-ad-revenue")
        exclude(group = "io.appmetrica.analytics", module = "analytics-ad-revenue-admob-v23")
        exclude(group = "io.appmetrica.analytics", module = "analytics-ad-revenue-applovin-v12")
        exclude(group = "io.appmetrica.analytics", module = "analytics-ad-revenue-fyber-v3")
        exclude(group = "io.appmetrica.analytics", module = "analytics-ad-revenue-ironsource-v7")
        exclude(group = "io.appmetrica.analytics", module = "analytics-apphud")
        exclude(group = "io.appmetrica.analytics", module = "analytics-location")
        exclude(group = "io.appmetrica.analytics", module = "analytics-ndkcrashes")
        exclude(group = "io.appmetrica.analytics", module = "analytics-screenshot")
    }
}