pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform" || requested.id.id == "org.jetbrains.kotlin.js") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "krossbow"

include("krossbow-client")
include("krossbow-engine-api")
include("krossbow-engine-spring")
include("krossbow-engine-webstompjs")
include("krossbow-engine-mpp")

enableFeaturePreview("GRADLE_METADATA")
