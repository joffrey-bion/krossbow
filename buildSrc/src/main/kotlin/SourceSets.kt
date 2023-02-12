import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

fun KotlinMultiplatformExtension.jsTargets() {
    js(BOTH) {
        browser()
        nodejs()
    }
}

fun KotlinMultiplatformExtension.jsWithBigTimeouts(name: String = "js") {
    js(name, BOTH) {
        useCommonJs()
        nodejsWithBigTimeout()
        browserWithBigTimeout()
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

fun KotlinTargetContainerWithNativeShortcuts.nativeTargets(flavor: String = "") {
    darwinTargets(flavor, setupSourceSets = false)

    linuxX64("linuxX64$flavor")
    mingwX64("mingwX64$flavor")

    sourceSets {
        setupMainAndTestSourceSets(allSourceSetRefs, flavor)
    }
}

fun KotlinTargetContainerWithNativeShortcuts.darwinTargets(
    flavor: String = "",
    setupSourceSets: Boolean = true,
) {
    iosArm64("iosArm64$flavor")
    iosX64("iosX64$flavor")
    iosSimulatorArm64("iosSimulatorArm64$flavor")

    tvosArm64("tvosArm64$flavor")
    tvosX64("tvosX64$flavor")
    tvosSimulatorArm64("tvosSimulatorArm64$flavor")

    watchosArm64("watchosArm64$flavor")
    watchosX64("watchosX64$flavor")
    watchosX86("watchosX86$flavor")
    watchosSimulatorArm64("watchosSimulatorArm64$flavor")

    macosArm64("macosArm64$flavor")
    macosX64("macosX64$flavor")

    if (setupSourceSets) {
        sourceSets {
            setupMainAndTestSourceSets(darwinRelatedSourceSetRefs, flavor)
        }
    }
}

private fun NamedDomainObjectContainer<KotlinSourceSet>.setupMainAndTestSourceSets(
    baseRefs: List<SourceSetRef>,
    flavor: String,
) {
    val commonMain by getting {}
    val commonTest by getting {}

    setupSourceSetsTree(baseRefs.withAppendedNames("${flavor}Main"))
    setupSourceSetsTree(baseRefs.withAppendedNames("${flavor}Test"))

    getByName("native${flavor}Main") {
        dependsOn(commonMain)
    }
    getByName("native${flavor}Test") {
        dependsOn(commonTest)
    }
}

private fun NamedDomainObjectContainer<KotlinSourceSet>.setupSourceSetsTree(sourceSetRefs: List<SourceSetRef>) {
    fun SourceSetRef.getOrCreateSourceSet() = findByName(name) ?: create(name)

    sourceSetRefs.forEach { ref ->
        val ss = ref.getOrCreateSourceSet()
        ref.parents.forEach {
            ss.dependsOn(it.getOrCreateSourceSet())
        }
    }
}

private val allSourceSetRefs = buildSourceSetRefs(
    parentsToChildren = mapOf(
        "native" to listOf(
            "desktop",
            "unix",
            "nonDarwin",
        ),
        "desktop" to listOf(
            "linuxX64",
            "mingwX64",
            "macosX64",
            "macosArm64",
        ),
        "unix" to listOf(
            "darwin",
            "linuxX64",
        ),
        "nonDarwin" to listOf(
            "linuxX64",
            "mingwX64",
        ),
        "darwin" to listOf(
            "iosArm64",
            "iosX64",
            "iosSimulatorArm64",
            "tvosArm64",
            "tvosX64",
            "tvosSimulatorArm64",
            "watchosArm64",
            "watchosX64",
            "watchosX86",
            "watchosSimulatorArm64",
            "macosArm64",
            "macosX64",
        ),
    ),
)

private val windowsPrefix = "mingw"
private val linuxPrefix = "linux"

/**
 * The Darwin source set, and all its ancestors and descendants
 */
private val darwinRelatedSourceSetRefs = allSourceSetRefs.filter { it.isDarwinOrDarwinDescendant } +
    allSourceSetRefs.first { it.isDarwin }.ancestors

private val SourceSetRef.isDarwin: Boolean
    get() = name == "darwin"

private val SourceSetRef.isDarwinOrDarwinDescendant: Boolean
    get() = isDarwin || parents.any { it.isDarwinOrDarwinDescendant }

private val SourceSetRef.ancestors: Set<SourceSetRef>
    get() = parents.toSet() + parents.flatMap { it.ancestors }

private val SourceSetRef.isWindows
    get() = name.startsWith(windowsPrefix)
