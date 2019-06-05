import org.jetbrains.kotlin.gradle.frontend.util.frontendExtension
import org.jetbrains.kotlin.gradle.frontend.webpack.WebPackExtension
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin2js")
    id("org.jetbrains.kotlin.frontend")
}

repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlin-js-wrappers")
}

dependencies {
    api(project(":krossbow-engine-api"))
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.2.1")
}

tasks {
    "compileKotlin2Js"(Kotlin2JsCompile::class)  {
        kotlinOptions.metaInfo = true
        kotlinOptions.outputFile = "${project.buildDir.path}/js/${project.name}.js"
        kotlinOptions.sourceMap = true
        kotlinOptions.moduleKind = "commonjs"
        kotlinOptions.main = "call"
    }
}

kotlinFrontend {

    sourceMaps = true

//    webpack {
//        contentPath = file(staticFilesBuildDir)
//    }

    npm {
        dependency("sockjs-client", "1.1.4")
        dependency("webstomp-client", "1.0.6")
    }
}

fun org.jetbrains.kotlin.gradle.frontend.KotlinFrontendExtension.webpack(
    configure: org.jetbrains.kotlin.gradle.frontend.webpack.WebPackExtension.() -> Unit
) {
    bundle("webpack", delegateClosureOf(configure))
}
