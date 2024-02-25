import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

fun KotlinMultiplatformExtension.allTargets() {
    jvm()
    js {
        browser()
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }
    nativeTargets()
}

fun KotlinMultiplatformExtension.jsWithBigTimeouts(
    name: String = "js",
    configure: KotlinJsTargetDsl.() -> Unit = {},
) {
    js(name) {
        useCommonJs()
        nodejsWithBigTimeout()
        browserWithBigTimeout()
        configure()
    }
}

private fun KotlinJsTargetDsl.nodejsWithBigTimeout() {
    nodejs {
        testTask {
            useMocha {
                timeout = "10s"
            }
        }
    }
}

fun KotlinJsTargetDsl.browserWithBigTimeout() {
    browser {
        testTask {
            useMocha {
                timeout = "10s"
            }
        }
    }
}

fun KotlinTargetContainerWithPresetFunctions.nativeTargets(
    flavor: String = "",
    configure: KotlinNativeTarget.() -> Unit = {},
) {
    appleTargets(flavor, configure)

    linuxX64("linuxX64$flavor", configure)
    mingwX64("mingwX64$flavor", configure)
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