plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    android {
        namespace = "io.flatzen.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    val xcfName = "shared:domainKit"

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
                //TODO rewrite module relations
                implementation(project(":shared:data"))
                implementation(project(":shared:commoncomponents"))

                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
