plugins {
    id("io.flatzen.base-shared-module")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.appmetrica.analytics)
            implementation(libs.koin.android)
        }
    }
}
