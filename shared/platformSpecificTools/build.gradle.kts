import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.android.library")
    kotlin("plugin.serialization") version "2.2.0"
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
            baseName = "PlatformSpecificTools"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shared:data"))
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }

        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("androidx.work:work-runtime-ktx:2.9.0")
        }

        iosMain.dependencies {
            // iOS specific dependencies if needed
        }
    }
}

android {
    namespace = "io.flatzen.shared.platformspecifictools"
    compileSdk = 35
    defaultConfig.minSdk = 24
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}