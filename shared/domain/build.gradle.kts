plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {

    androidLibrary {
        namespace = "io.flatzen.domain"
        compileSdk = 36
        minSdk = 24
    }

    val xcfName = "shared:domainKit"

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
                //TODO rewrite module relations
                implementation(project(":shared:data"))
                implementation(project(":shared:commoncomponents"))


                // Add KMP dependencies here
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