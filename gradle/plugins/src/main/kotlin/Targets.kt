import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.allTargets() {
    ktor3Targets()

    wasmWasi {
        nodejs()
    }
}

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.ktor3Targets() {
    ktor2Targets()

    wasmJs {
        browser()
        nodejs()
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.ktor2Targets() {
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all-compatibility")
        }
    }
    js {
        browser()
        nodejs {
            testTask {
                useMocha {
                    timeout = "60s"
                }
            }
        }
    }
    appleTargets()
    linuxX64()
    linuxArm64()
    mingwX64()
}

fun KotlinTargetContainerWithPresetFunctions.appleTargets() {
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()

    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()

    macosArm64()
    macosX64()
}
