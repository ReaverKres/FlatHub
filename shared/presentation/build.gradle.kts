import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "io.flatzen.shared.presentation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Presentation"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:commoncomponents"))
            implementation(project(":shared:data"))
            implementation(project(":shared:domain"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.flowmvi.core)
            api(libs.kmp.notifier)
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.notifications)
            implementation(libs.mp.maps)
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }
    }
}
