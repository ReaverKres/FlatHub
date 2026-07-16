plugins {
    id("io.flatzen.base-shared-module")
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:analytics"))
            implementation(project(":shared:commoncomponents"))
            implementation(project(":shared:data"))
            implementation(project(":shared:domain"))
            implementation(project(":shared:monetization"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            implementation(libs.flowmvi.core)
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.notifications)
            implementation(libs.mp.maps)
            implementation(libs.kotlinx.collections.immutable)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }
    }
}
