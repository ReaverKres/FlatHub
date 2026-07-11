plugins {
    id("io.flatzen.base-shared-module")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shared:commoncomponents"))

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.logging)
                implementation(libs.ktorfit.lib)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ksoup.html)

                implementation(libs.koin.core)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.mp.maps)
            }
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    if (name != "compileKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}
