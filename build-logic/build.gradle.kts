plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("baseSharedModule") {
            id = "io.flatzen.base-shared-module"
            implementationClass = "io.flatzen.buildlogic.BaseSharedModulePlugin"
        }
    }
}
