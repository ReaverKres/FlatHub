import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "io.flatzen"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.flatzen"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.android.versionCode.get().toInt()
        versionName = libs.versions.android.versionName.get()
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("release") {
            val storePath = localProperties.getProperty("flatzen.keystore.path") ?: "LocusKeyStore.jks"
            keyAlias = localProperties.getProperty("flatzen.key.alias")
            keyPassword = localProperties.getProperty("flatzen.key.password")
            storeFile = rootProject.file(storePath)
            storePassword = localProperties.getProperty("flatzen.keystore.password")
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared.presentation)
    implementation(projects.shared.commoncomponents)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashScreen)
    implementation(libs.appmetrica.analytics)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.flowmvi.compose)
    implementation(libs.kmp.notifier)
}
