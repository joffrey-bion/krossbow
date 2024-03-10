import org.jetbrains.kotlin.gradle.dsl.*

fun KotlinMultiplatformExtension.allTargets() {
    ktorTargets()
}

fun KotlinMultiplatformExtension.ktorTargets() {
    jvm()
    js {
        browser()
        nodejs {
            testTask {
                useMocha {
                    timeout = "20s"
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
