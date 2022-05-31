import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun KotlinMultiplatformExtension.jsWithBigTimeouts(name: String = "js") {
    js(name, BOTH) {
        useCommonJs()
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }
}

fun KotlinMultiplatformExtension.setupNativeTargets() {
    ios()
    iosSimulatorArm64()

    tvos()
    tvosSimulatorArm64()

    // watchos() shortcut cannot be used because okio is missing watchosX64()
    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosSimulatorArm64()

    // Desktop not supported yet
    // macosX64()
    // linuxX64()
    // mingwX64()
}

fun NamedDomainObjectContainer<KotlinSourceSet>.setupNativeSourceSets() {
    val commonMain by getting {}
    val commonTest by getting {}

    val nativeMain by creating {
        dependsOn(commonMain)
    }
    val nativeTest by creating {
        dependsOn(commonTest)
    }

    val nativeDarwinMain by creating {
        dependsOn(nativeMain)
    }
    val nativeDarwinTest by creating {
        dependsOn(nativeTest)
    }

    val iosMain by getting {
        dependsOn(nativeDarwinMain)
    }
    val iosTest by getting {
        dependsOn(nativeDarwinTest)
    }

    val iosSimulatorArm64Main by getting {
        dependsOn(iosMain)
    }
    val iosSimulatorArm64Test by getting {
        dependsOn(iosTest)
    }

    val watchosX86Main by getting {
        dependsOn(nativeDarwinMain)
    }
    val watchosX86Test by getting {
        dependsOn(nativeDarwinTest)
    }
    val watchosArm32Main by getting {
        dependsOn(nativeDarwinMain)
    }
    val watchosArm32Test by getting {
        dependsOn(nativeDarwinTest)
    }
    val watchosArm64Main by getting {
        dependsOn(nativeDarwinMain)
    }
    val watchosArm64Test by getting {
        dependsOn(nativeDarwinTest)
    }

    val watchosSimulatorArm64Main by getting {
        dependsOn(nativeDarwinMain)
    }
    val watchosSimulatorArm64Test by getting {
        dependsOn(nativeDarwinTest)
    }

    val tvosMain by getting {
        dependsOn(nativeDarwinMain)
    }
    val tvosTest by getting {
        dependsOn(nativeDarwinTest)
    }

    val tvosSimulatorArm64Main by getting {
        dependsOn(tvosMain)
    }
    val tvosSimulatorArm64Test by getting {
        dependsOn(tvosTest)
    }
}
