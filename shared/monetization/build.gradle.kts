plugins {
    id("io.flatzen.base-shared-module")
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shared:commoncomponents"))
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization)
                implementation(libs.ktor.client.core)
                api(libs.datetime)
                api(libs.androidx.datastore.preferences.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.androidx.billing.ktx)
                implementation(libs.applovin.sdk)
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
