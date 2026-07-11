plugins {
    id("io.flatzen.base-shared-module")
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
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
    }
}
