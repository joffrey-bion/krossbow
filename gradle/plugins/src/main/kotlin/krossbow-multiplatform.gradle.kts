import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    sourceSets {
        all {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
        }
    }
    applyDefaultHierarchyTemplate {
        group("native") {
            group("unix") {
                withLinux()
                withApple()
            }
        }
    }
}
