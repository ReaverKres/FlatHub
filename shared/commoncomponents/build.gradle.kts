import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    id("io.flatzen.base-shared-module")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
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
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.config)
            }
        }
    }
}

// Prefer explicit -Pbuildkonfig.flavor=…; else infer from task names (assembleRelease → release).
val buildkonfigFlavor =
    gradle.startParameter.projectProperties["buildkonfig.flavor"]
        ?: gradle.startParameter.taskNames
            .joinToString(" ")
            .let { tasks ->
                if (tasks.contains(
                        "Release",
                        ignoreCase = true
                    )
                ) "release" else "debug"
            }
extra.set("buildkonfig.flavor", buildkonfigFlavor)

buildkonfig {
    packageName = "io.flatzen"
    objectName = "BuildKonfig"
    exposeObjectWithName = "BuildKonfig"

    defaultConfigs {
        buildConfigField(BOOLEAN, "DEBUG", "true")
    }
    defaultConfigs("debug") {
        buildConfigField(BOOLEAN, "DEBUG", "true")
    }
    defaultConfigs("release") {
        buildConfigField(BOOLEAN, "DEBUG", "false")
    }
}
