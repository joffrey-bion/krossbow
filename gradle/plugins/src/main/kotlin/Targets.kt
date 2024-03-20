import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.allTargets() {
    ktorTargets()

    wasmJs {
        browser()
        nodejs()
    }
    // wasmWasi() not supported by coroutines 1.8.0
}

fun KotlinMultiplatformExtension.ktorTargets() {
    jvm()
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
