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
        progressiveMode = true
        // we can't enable this yet because of -Xjvm-default=all-compatibility generating a waning on non-JVM
        // allWarningsAsErrors = true
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}
