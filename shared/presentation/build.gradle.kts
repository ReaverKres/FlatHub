plugins {
    id("io.flatzen.base-shared-module")
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:commoncomponents"))
            implementation(project(":shared:data"))
            implementation(project(":shared:domain"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.flowmvi.core)
            api(libs.kmp.notifier)
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.notifications)
            implementation(libs.mp.maps)
            implementation(libs.kotlinx.collections.immutable)
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }
    }
}
