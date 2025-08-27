plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {

    androidLibrary {
        namespace = "io.flatzen.commoncomponents"
        compileSdk = 36
        minSdk = 24
    }

    val xcfName = "shared:commoncomponentsKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

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
                // Add KMP dependencies here
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
                api(libs.datetime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.appmetrica.analytics)
            }
        }

        iosMain {
            dependencies {

            }
        }
    }

}