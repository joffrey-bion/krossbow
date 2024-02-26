rootProject.name = "test-server"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

//include("websocket-test-api")
include("websocket-test-server")
include("websocket-test-server-gradle-plugin")
