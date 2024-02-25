import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun KotlinMultiplatformExtension.allTargets() {
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
    mingwX64()
}

fun KotlinTargetContainerWithPresetFunctions.appleTargets(
    flavor: String = "",
    configure: KotlinNativeTarget.() -> Unit = {},
) {
    iosArm64("iosArm64$flavor", configure)
    iosSimulatorArm64("iosSimulatorArm64$flavor", configure)
    iosX64("iosX64$flavor", configure)

    tvosArm64("tvosArm64$flavor", configure)
    tvosSimulatorArm64("tvosSimulatorArm64$flavor", configure)
    tvosX64("tvosX64$flavor", configure)

    watchosArm64("watchosArm64$flavor", configure)
    watchosSimulatorArm64("watchosSimulatorArm64$flavor", configure)
    watchosX64("watchosX64$flavor", configure)

    macosArm64("macosArm64$flavor", configure)
    macosX64("macosX64$flavor", configure)
}
