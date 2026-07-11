package io.flatzen.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

@Suppress("UNUSED")
class BaseSharedModulePlugin : Plugin<Project> {
    private companion object {
        const val NAMESPACE_PREFIX = "io.flatzen"
    }

    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val compileSdkVersion = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        val minSdkVersion = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            val moduleName = target.name
            val androidNamespace = "$NAMESPACE_PREFIX.$moduleName"
            val iosFrameworkBaseName = "${moduleName}Kit"

            iosArm64()
            iosSimulatorArm64()

            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compileSdk = compileSdkVersion
                minSdk = minSdkVersion
                namespace = androidNamespace
            }

            targets.withType<KotlinNativeTarget>().configureEach {
                if (konanTarget.family == Family.IOS) {
                    binaries.withType(Framework::class.java).configureEach {
                        baseName = iosFrameworkBaseName
                    }
                }
            }

            targets.configureEach {
                compilations.configureEach {
                    compileTaskProvider.configure {
                        compilerOptions {
                            freeCompilerArgs.add("-Xexpect-actual-classes")
                        }
                    }
                }
            }
        }
    }
}
