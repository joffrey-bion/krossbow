import org.jetbrains.kotlin.gradle.*

plugins {
    kotlin("multiplatform")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("native") {
                group("unix") {
                    withLinux()
                    withApple()
                }
            }
            group("jsAndWasm") {
                withJs()
                group("wasm") {
                    withWasmJs()
                    withWasmWasi()
                }
            }
        }
    }
    compilerOptions {
        progressiveMode = true
        freeCompilerArgs.add("-Xrender-internal-diagnostic-names")

        // Note: target-specific compiler options are set in the target helpers (see Targets.kt)
    }
}
