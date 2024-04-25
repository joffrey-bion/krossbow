import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate {
        group("native") {
            group("unix") {
                withLinux()
                withApple()
            }
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}
