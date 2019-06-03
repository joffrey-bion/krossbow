pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        // this one is for kotlin-frontend-plugin
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform" || requested.id.id == "kotlin2js") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "org.jetbrains.kotlin.frontend") {
                useModule("org.jetbrains.kotlin:kotlin-frontend-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "krossbow"

include("krossbow-client")
include("krossbow-engine-api")
include("krossbow-engine-spring")
include("krossbow-engine-webstompjs")

enableFeaturePreview("GRADLE_METADATA")
