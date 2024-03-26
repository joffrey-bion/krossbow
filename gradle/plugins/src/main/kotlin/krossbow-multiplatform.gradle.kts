import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.*

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
}

// Drop this configuration when the Node.JS version in KGP will support wasm gc milestone 4
// check it here:
// https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/nodejs/NodeJsRootExtension.kt
//extensions.configure<> {  }<NodeJsRootExtension>().nodeVersion {
//    // canary nodejs that supports recent Wasm GC changes
//    nodeVersion = "21.0.0-v8-canary202309167e82ab1fa2"
//    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
//}

// Drop this when node js version become stable
tasks.withType<KotlinNpmInstallTask> {
    args.add("--ignore-engines")
}
