plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    android {
        namespace = "io.flatzen.commoncomponents"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    val xcfName = "shared:commoncomponentsKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization)
                api(libs.datetime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.appmetrica.analytics)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.config)
                implementation(libs.firebase.analytics)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
