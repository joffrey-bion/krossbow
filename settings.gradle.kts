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

include("krossbow-stomp-core")
include("krossbow-stomp-kxserialization")
include("krossbow-stomp-jackson")
include("krossbow-websocket-api")
include("krossbow-websocket-sockjs")
include("krossbow-websocket-spring")

enableFeaturePreview("GRADLE_METADATA")
