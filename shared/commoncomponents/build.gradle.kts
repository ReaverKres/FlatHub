plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
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
                implementation(libs.kotlinx.serialization)
                api(libs.datetime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.appmetrica.analytics)
                implementation(project.dependencies.platform(libs.firebase.bom))
                // Add the dependencies for the Remote Config and Analytics libraries
                // When using the BoM, you don't specify versions in Firebase library dependencies
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