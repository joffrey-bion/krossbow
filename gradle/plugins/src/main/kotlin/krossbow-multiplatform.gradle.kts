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
            group("wasm") {
                withWasmJs()
                withWasmWasi()
            }
        }
    }
    compilerOptions {
        progressiveMode = true

        // We can't enable 'allWarningsAsErrors' yet because of the unique_name warning:
        // KLIB resolver: The same 'unique_name=org.hildan.krossbow:krossbow-websocket-core' found in more than one library:
        //   <root>\krossbow-websocket-core\build\classes\kotlin\wasmJs\main,
        //   <root>\krossbow-websocket-core\build\libs\krossbow-websocket-core-wasm-js.klib
        // allWarningsAsErrors = true

        // Note: target-specific compiler options are set in the target helpers (see Targets.kt)
    }
}
